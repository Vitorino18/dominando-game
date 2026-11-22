package br.com.dominando.server;
import br.com.dominando.model.PartidaGeral;

import br.com.dominando.model.*;
import br.com.dominando.grpc.*;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

public class Sala {
    private static final int MAX_JOGADORES = 2;

    private final String codigo;
    private final List<ParticipanteSala> participantes;

    private final List<AssinaturaJogo> assinaturas =
            new CopyOnWriteArrayList<>();

    private PartidaGeral partidaGeral;
    private boolean iniciada;

    public Sala(String codigo, String criador) {
        this.codigo = codigo;
        this.participantes = new ArrayList<>();
        participantes.add(new ParticipanteSala(criador, true));

        this.iniciada = false;
    }

    public synchronized ParticipanteSala adicionarJogador(String nome) {
        if (iniciada || participantes.size() >= MAX_JOGADORES) {
            throw new IllegalStateException("A sala está cheia ou já foi iniciada.");
        }
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do jogador não pode ser vazio.");
        }

        ParticipanteSala novo = new ParticipanteSala(nome, false);
        participantes.add(novo);

        return novo;
    }

    public synchronized ParticipanteSala buscarPorToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        for (ParticipanteSala participante : participantes) {
            if (Objects.equals(participante.getToken(), token)) {
                return participante;
            }
        }
        return null;
    }

    public synchronized boolean podeIniciar() {
        return !iniciada && participantes.size() == MAX_JOGADORES;
    }

    public synchronized void iniciarPartida(String tokenCriador) {
        ParticipanteSala solicitante = buscarPorToken(tokenCriador);

        if (solicitante == null || !solicitante.isCriador()) {
            throw new SecurityException("Somente o criador pode iniciar a partida.");
        }

        if (!podeIniciar()) {
            throw new IllegalStateException("A sala precisa ter 2 jogadores para iniciar.");
        }

        List<String> nomes = new ArrayList<>();

        for (ParticipanteSala participante : participantes) {
            nomes.add(participante.getNome());
        }

        partidaGeral = new PartidaGeral(nomes);
        partidaGeral.inicioCompeticao();
        iniciada = true;
    }

    public synchronized ResultadoJogada executarAcao(String token, AcaoJogo acao, Integer indicePeca, LadoMesa lado) {
        ParticipanteSala participante = buscarPorToken(token);

        if (participante == null) {
            throw new SecurityException("Token inválido.");
        }

        if (!iniciada || partidaGeral == null || partidaGeral.competicaoEncerrada()) {
            throw new IllegalStateException("Não existe uma competição em andamento.");
        }

        Partida partida = partidaGeral.getPartidaAtual();
        Jogador jogador = partida.getJogadorVez();

        if (!jogador.getNome().equals(participante.getNome())) {
            throw new SecurityException("Não é a vez deste jogador.");
        }

        ResultadoJogada resultado;

        switch (acao) {
            case JOGAR_PECA -> {
                if (indicePeca == null || lado == null) {
                    throw new IllegalArgumentException("Informe a peça e o lado.");
                }

                if (indicePeca < 0 || indicePeca >= jogador.getMao().size()) {
                    throw new IllegalArgumentException("Índice de peça inválido.");
                }

                Peca peca = jogador.getMao().get(indicePeca);
                resultado = partida.jogar(peca, lado);
            }

            case COMPRAR_PECA -> resultado = partida.comprarPeca();

            case PASSAR_VEZ -> resultado = partida.passarVez();

            default -> throw new IllegalArgumentException("Ação não reconhecida.");
        }

        partidaGeral.verificarFimPartida();
        return resultado;
    }

    // registrar uma conexão de streaming //
    public synchronized AssinaturaJogo acompanharJogo(String token, StreamObserver<EstadoJogoResponse> observer, Executor executor) {
        if (buscarPorToken(token) == null) {
            throw new SecurityException("Token inválido.");
        }

        AtomicReference<AssinaturaJogo> referencia = new AtomicReference<>();
        AssinaturaJogo assinatura = new AssinaturaJogo(token, observer, executor, () -> assinaturas.remove(referencia.get()));

        referencia.set(assinatura);
        if (assinatura.isAtiva()) {
            assinaturas.add(assinatura);
        }

        // Protege contra cancelamento durante o registro //
        if (!assinatura.isAtiva()) {
            assinaturas.remove(assinatura);
            return assinatura;
        }

        // Envia o estado inicial se o jogo já começou //
        if (iniciada && partidaGeral != null) {
            assinatura.publicar(consultarEstadoJogo(token));
        }
        return assinatura;
    }

    // notificar todos os clientes conectados //
    public synchronized void notificarJogadores() {
        if (!iniciada || partidaGeral == null) {
            return;
        }

        for (AssinaturaJogo assinatura : assinaturas) {
            if (!assinatura.isAtiva()) {
                continue;
            }

            EstadoJogoResponse estado = consultarEstadoJogo(assinatura.getToken());
            assinatura.publicar(estado);
        }
    }

    public synchronized EstadoJogoResponse consultarEstadoJogo(String token) {

        ParticipanteSala participante = buscarPorToken(token);

        if (participante == null) {
            throw new SecurityException("Token inválido.");
        }
        if (!iniciada || partidaGeral == null) {
            throw new IllegalStateException("A competição ainda não começou.");
        }

        EstadoJogoResponse.Builder resposta = EstadoJogoResponse.newBuilder();

        for (Jogador jogador : partidaGeral.getJogadores()) {

            JogadorMensagem dados = JogadorMensagem.newBuilder().setNome(jogador.getNome()).setQuantidadePecas(jogador.quantidadePecas())
                    .setPontuacao(jogador.getPontuacao())
                    .setVitorias(jogador.getVitorias())
                    .setGatos(jogador.getErros())
                    .setPontosPetiscos(jogador.getPontosPetiscos())
                    .build();

            resposta.addJogadores(dados);
        }

        if (partidaGeral.competicaoEncerrada()) {
            resposta.setCompeticaoEncerrada(true);
            resposta.setCampeao(partidaGeral.getCampeao().getNome());

            return resposta.build();
        }

        Partida partida = partidaGeral.getPartidaAtual();

        resposta.setJogadorDaVez(partida.getJogadorVez().getNome());

        resposta.setPecasBaralho(partida.getBaralho().quantidadePecas());

        for (Peca peca : partida.getMesa().getPecasMesa()) {
            resposta.addPecasMesa(converterPeca(peca));
        }

        for (Jogador jogador : partida.getJogadores()) {
            if (jogador.getNome().equals(participante.getNome())) {

                for (Peca peca : jogador.getMao()) {
                    resposta.addMinhaMao(converterPeca(peca));
                }
                break;
            }
        }

        return resposta.build();
    }

    private PecaMensagem converterPeca(Peca peca) {
        return PecaMensagem.newBuilder().setLadoA(peca.getLadoA()).setLadoB(peca.getLadoB()).build();
    }

    // metodo para fazer testes principalmente //
    public int quantidadeAssinaturasAtivas() {
        int quantidade = 0;

        for (AssinaturaJogo assinatura : assinaturas) {
            if (assinatura.isAtiva()) {
                quantidade++;
            }
        }
        return quantidade;
    }



    public String getCodigo() {
        return codigo;
    }

    public synchronized List<String> getJogadores() {
        List<String> nomes = new ArrayList<>();

        for (ParticipanteSala participante : participantes) {
            nomes.add(participante.getNome());
        }

        return List.copyOf(nomes);
    }

    public synchronized ParticipanteSala getCriador() {
        return participantes.getFirst();
    }

    public synchronized boolean isIniciada() {
        return iniciada;
    }

    public synchronized PartidaGeral getPartidaGeral() {
        return partidaGeral;
    }
}
