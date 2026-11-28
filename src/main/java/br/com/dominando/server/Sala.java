package br.com.dominando.server;
import br.com.dominando.model.PartidaGeral;

import br.com.dominando.model.*;
import br.com.dominando.grpc.*;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import io.grpc.Status;

public class Sala {
    private static final int MAX_JOGADORES = ConfiguracaoServidor.MAX_JOGADORES_POR_SALA;

    private final String codigo;
    private final List<ParticipanteSala> participantes;

    private final List<AssinaturaJogo> assinaturas = new CopyOnWriteArrayList<>();

    private static final ScheduledExecutorService temporizador = Executors.newScheduledThreadPool(2);
    private ScheduledFuture<?> tarefaLoja;

    private PartidaGeral partidaGeral;
    private boolean iniciada;

    private ScheduledFuture<?> tarefaPreparacao;
    private long versaoPreparacao;

    private long ultimaAtividade = System.nanoTime();
    private Long desconectadosDesde;
    private Long competicaoEncerradaDesde;

    private boolean removida = false;
    private boolean recursosLiberados = false;

    public Sala(String codigo, String criador) {
        this.codigo = codigo;
        this.participantes = new ArrayList<>();
        participantes.add(new ParticipanteSala(criador, true));

        this.iniciada = false;
    }

    public synchronized ParticipanteSala adicionarJogador(String nome) {
        verificarSalaAtiva();
        if (iniciada || participantes.size() >= MAX_JOGADORES) {
            throw new IllegalStateException("A sala está cheia ou já foi iniciada.");
        }

        nome = ParticipanteSala.validarNome(nome);

        for (ParticipanteSala participante : participantes) {
            if (participante.getNome().equalsIgnoreCase(nome)) {
                throw new IllegalArgumentException("Já existe um jogador com esse nome na sala.");
            }
        }

        ParticipanteSala novo = new ParticipanteSala(nome, false);

        participantes.add(novo);
        registrarAtividade();

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
        verificarSalaAtiva();
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

        registrarAtividade();
        atualizarDesconexao();
    }

    public synchronized ResultadoJogada executarAcao(String token, AcaoJogo acao, Integer indicePeca, LadoMesa lado) {
        verificarSalaAtiva();
        ParticipanteSala participante = buscarPorToken(token);

        if (participante == null) {
            throw new SecurityException("Token inválido.");
        }

        if (!iniciada || partidaGeral == null || partidaGeral.competicaoEncerrada()) {
            throw new IllegalStateException("Não existe uma competição em andamento.");
        }
        if (!partidaGeral.podeRealizarJogadas()) {
            throw new IllegalStateException("Não é possível jogar durante esta fase.");
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

        if(resultado == ResultadoJogada.JOGADA_VALIDA || resultado == ResultadoJogada.GATO || resultado == ResultadoJogada.COMPRA_REALIZADA
                || resultado == ResultadoJogada.VEZ_PASSADA || resultado == ResultadoJogada.PARTIDA_ENCERRADA){
            registrarAtividade();
        }

        partidaGeral.verificarFimPartida();

        if (partidaGeral.competicaoEncerrada() && competicaoEncerradaDesde == null) {
            competicaoEncerradaDesde = System.nanoTime();
        }

        atualizarTemporizadores();
        return resultado;
    }

    private synchronized void atualizarDesconexao() {
        boolean existeJogadorConectado = assinaturas.stream().anyMatch(AssinaturaJogo::isAtiva);

        if (existeJogadorConectado) {
            desconectadosDesde = null;
        } else if (desconectadosDesde == null) {
            desconectadosDesde = System.nanoTime();
        }
    }

    private boolean tempoExpirado(long inicio, int minutos) {
        long tempoDecorrido = System.nanoTime() - inicio;
        return tempoDecorrido >= TimeUnit.MINUTES.toNanos(minutos);
    }

    public synchronized boolean deveSerRemovida() {
        if (removida) {
            return true;
        }
        // Competição encerrada //
        if (competicaoEncerradaDesde != null) {
            return tempoExpirado(competicaoEncerradaDesde, ConfiguracaoServidor.TEMPO_SALA_ENCERRADA_MINUTOS);
        }
        // Sala aguardando início //
        if (!iniciada) {
            return tempoExpirado(ultimaAtividade, ConfiguracaoServidor.TEMPO_SALA_AGUARDANDO_MINUTOS);
        }
        // Todos os jogadores desconectados //
        if (desconectadosDesde != null && tempoExpirado(desconectadosDesde, ConfiguracaoServidor.TEMPO_DESCONECTADOS_MINUTOS)) {
            return true;
        }
        // Competição sem atividade //
        return tempoExpirado(ultimaAtividade, ConfiguracaoServidor.TEMPO_INATIVIDADE_MINUTOS);
    }
    private void verificarSalaAtiva() {
        if (removida) {
            throw new IllegalStateException("Esta sala foi encerrada por inatividade.");
        }
    }


    // registrar uma conexão de streaming //
    public synchronized AssinaturaJogo acompanharJogo(String token, StreamObserver<EstadoJogoResponse> observer, Executor executor) {
        verificarSalaAtiva();
        
        if (buscarPorToken(token) == null) {
            throw new SecurityException("Token inválido.");
        }
        if (quantidadeStreamsAtivos(token) >= ConfiguracaoServidor.MAX_STREAMS_POR_JOGADOR) {
            throw Status.RESOURCE_EXHAUSTED.withDescription("Limite de conexões simultâneas atingido.").asRuntimeException();
        }

        verificarSalaAtiva();
        AtomicReference<AssinaturaJogo> referencia = new AtomicReference<>();
        AssinaturaJogo assinatura = new AssinaturaJogo(token, observer, executor, () -> removerAssinatura(referencia.get()));
        referencia.set(assinatura);
        assinatura.registrarCancelamento();

        if (assinatura.isAtiva()) {
            assinaturas.add(assinatura);
            atualizarDesconexao();
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

        // Notifica o oponente de que o jogador conectou //
        notificarJogadores();
        return assinatura;
    }

    // notificar todos os clientes conectados //
    public synchronized void notificarJogadores() {
        if (removida) {
            return;
        }
        if (!iniciada || partidaGeral == null) {
            return;
        }

        System.out.println("[SALA] Notificando jogadores. Assinaturas: " + assinaturas.size());

        for (AssinaturaJogo assinatura : assinaturas) {
            if (!assinatura.isAtiva()) {
                continue;
            }

            EstadoJogoResponse estado = consultarEstadoJogo(assinatura.getToken());
            assinatura.publicar(estado);
        }
    }

    public synchronized EstadoJogoResponse consultarEstadoJogo(String token) {
        verificarSalaAtiva();
        ParticipanteSala participante = buscarPorToken(token);

        if (participante == null) {
            throw new SecurityException("Token inválido.");
        }
        if (!iniciada || partidaGeral == null) {
            throw new IllegalStateException("A competição ainda não começou.");
        }

        Jogador meuJogador = buscarJogador(participante);
        InventarioPetiscos inventario = meuJogador.getInventarioPetiscos();

        LojaPetiscos loja = partidaGeral.getLojaPetiscos();

        EstadoJogoResponse.Builder resposta = EstadoJogoResponse.newBuilder();

        // Informações gerais da competição //
        resposta.setNumeroRodada(partidaGeral.getNumeroRodada());
        resposta.setFase(converterFase(partidaGeral.getFase()));

        for (Jogador jogador : partidaGeral.getJogadores()) {
            JogadorMensagem dados = JogadorMensagem.newBuilder().setNome(jogador.getNome()).setQuantidadePecas(jogador.quantidadePecas())
                    .setPontuacao(jogador.getPontuacao())
                    .setVitorias(jogador.getVitorias())
                    .setGatos(jogador.getErros())
                    .setPontosPetiscos(jogador.getPontosPetiscos())
                    .setConectado(jogadorConectadoPorNome(jogador.getNome()))
                    .setPreparado(partidaGeral.isJogadorPreparado(jogador))
                    .build();

            resposta.addJogadores(dados);
        }

        // Verifica se a competição terminou //
        if (partidaGeral.competicaoEncerrada()) {
            resposta.setCompeticaoEncerrada(true);
            resposta.setCampeao(partidaGeral.getCampeao().getNome());

            return resposta.build();
        }

    // Informações privadas do jogador //
        resposta.setMeuPetisco(converterPetisco(inventario.getPetiscoAtual()));
        resposta.setPossoUtilizarPetisco(podeUtilizarPetisco(meuJogador));
        resposta.setEstouPreparado(partidaGeral.isJogadorPreparado(meuJogador));

        if (partidaGeral.getFase() == FasePartida.LOJA_PETISCOS) {
            resposta.setTempoLojaRestanteSegundos(loja.getTempoRestanteSegundos());
            resposta.setJaCompreiNestaLoja(inventario.comprouNestaLoja(loja.getNumeroLoja()));

            for (TipoPetiscos tipo : TipoPetiscos.values()) {
                String motivo = verificarDisponibilidadeCompra(meuJogador, tipo);

                PetiscoLojaMensagem petisco = PetiscoLojaMensagem.newBuilder().setTipo(converterPetisco(tipo)).setNome(tipo.getNome())
                                .setDescricao(tipo.getDescricao())
                                .setCusto(tipo.getCusto())
                                .setPodeComprar(motivo.isEmpty())
                                .setMotivoIndisponibilidade(motivo)
                                .build();
                resposta.addPetiscosLoja(petisco);
            }
            return resposta.build();
        }

        if (partidaGeral.getFase() == FasePartida.PREPARACAO_RODADA) {
            resposta.setTempoPreparacaoRestanteSegundos(partidaGeral.getTempoPreparacaoRestanteSegundos());
        }

        // informaçoes da rodada //
        Partida partida = partidaGeral.getPartidaAtual();
        if (partida == null) {
            return resposta.build();
        }
        if (partidaGeral.podeRealizarJogadas()) {
            resposta.setJogadorDaVez(partida.getJogadorVez().getNome());
        }

        resposta.setPecasBaralho(partida.getBaralho().quantidadePecas());

        // Peças públicas da mesa //
        for (Peca peca : partida.getMesa().getPecasMesa()) {
            resposta.addPecasMesa(converterPeca(peca));
        }

        for (Peca peca : meuJogador.getMao()) {
            resposta.addMinhaMao(converterPeca(peca));
        }
        return resposta.build();
    }

    private long quantidadeStreamsAtivos(String token) {
        return assinaturas.stream().filter(AssinaturaJogo::isAtiva).filter(assinatura -> Objects.equals(assinatura.getToken(), token)).count();
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

    private boolean jogadorConectado(String token) {
        for (AssinaturaJogo assinatura : assinaturas) {
            if (assinatura.isAtiva() && assinatura.getToken().equals(token)) {
                return true;
            }
        }
        return false;
    }

    private synchronized void removerAssinatura(AssinaturaJogo assinatura) {
        if (assinaturas.remove(assinatura)) {
            atualizarDesconexao();
            System.out.println("[SALA " + codigo + "] Assinatura removida.");
            notificarJogadores();
        }
    }

    private boolean jogadorConectadoPorNome(String nome) {
        for (ParticipanteSala participante : participantes) {
            if (participante.getNome().equals(nome)) {
                return jogadorConectado(participante.getToken());
            }
        }
        return false;
    }

    // temporizador da loja de petiscos //
    private synchronized void agendarEncerramentoLoja() {
        if (partidaGeral == null || partidaGeral.getFase() != FasePartida.LOJA_PETISCOS) {
            return;
        }

        // Evita agendar mais de uma tarefa para a mesma loja //
        if (tarefaLoja != null && !tarefaLoja.isDone()) {
            return;
        }

        int numeroLoja = partidaGeral.getLojaPetiscos().getNumeroLoja();

        tarefaLoja = temporizador.schedule(() -> encerrarLojaAutomaticamente(numeroLoja), LojaPetiscos.DURACAO_SEGUNDOS, TimeUnit.SECONDS);

        System.out.println("[SALA " + codigo + "] " + "Encerramento da loja agendado.");
    }

    private synchronized void encerrarLojaAutomaticamente(int numeroLoja) {
        if (removida) {
            return;
        }
        tarefaLoja = null;

        if (partidaGeral == null || partidaGeral.competicaoEncerrada()) {
            return;
        }

        if (partidaGeral.getFase() != FasePartida.LOJA_PETISCOS) {
            return;
        }

        if (partidaGeral.getLojaPetiscos().getNumeroLoja() != numeroLoja) {
            return;
        }

        boolean lojaEncerrada = partidaGeral.verificarEncerramentoLoja();

        if (lojaEncerrada) {
            System.out.println("[SALA " + codigo + "] " + "Loja encerrada automaticamente.");
            System.out.println("[SALA " + codigo + "] " + "Iniciando rodada " + partidaGeral.getNumeroRodada());

            atualizarTemporizadores();
            notificarJogadores();
        } else {
            tarefaLoja = temporizador.schedule(() -> encerrarLojaAutomaticamente(numeroLoja), 100, TimeUnit.MILLISECONDS);
        }
    }

    public synchronized ResultadoPetiscosLoja comprarPetisco(String token, TipoPetiscos tipo) {
        verificarSalaAtiva();
        ParticipanteSala participante = buscarPorToken(token);

        if (participante == null) {
            throw new SecurityException("Token inválido.");
        }
        if (!iniciada || partidaGeral == null || partidaGeral.competicaoEncerrada()) {
            throw new IllegalStateException("Não existe uma competição em andamento.");
        }

        Jogador jogador = buscarJogador(participante);

        ResultadoPetiscosLoja resultado = partidaGeral.comprarPetisco(jogador, tipo);

        if (resultado == ResultadoPetiscosLoja.COMPRA_REALIZADA) {
            registrarAtividade();
            System.out.println("[SALA " + codigo + "] " + jogador.getNome() + " comprou " + tipo.getNome());
            notificarJogadores();
        }

        return resultado;
    }

    private synchronized void agendarEncerramentoPreparacao() {
        if (partidaGeral == null || partidaGeral.getFase() != FasePartida.PREPARACAO_RODADA) {
            return;
        }
        if (tarefaPreparacao != null && !tarefaPreparacao.isDone()) {
            return;
        }

        long versao = ++versaoPreparacao;
        long tempoRestante = partidaGeral.getTempoPreparacaoRestanteSegundos();

        tarefaPreparacao = temporizador.schedule(() -> encerrarPreparacaoAutomaticamente(versao), tempoRestante, TimeUnit.SECONDS);

        System.out.println("[SALA " + codigo + "] " + "Preparação da rodada iniciada.");
    }

    private Jogador buscarJogador(ParticipanteSala participante) {
        for (Jogador jogador : partidaGeral.getJogadores()) {
            if (jogador.getNome().equals(participante.getNome())) {
                return jogador;
            }
        }
        throw new IllegalStateException("Jogador não encontrado na competição.");
    }

    private synchronized void encerrarPreparacaoAutomaticamente(long versao) {
        if (removida) {
            return;
        }
        if (versao != versaoPreparacao) {
            return;
        }

        tarefaPreparacao = null;

        if (partidaGeral == null || partidaGeral.getFase() != FasePartida.PREPARACAO_RODADA) {
            return;
        }

        if (!partidaGeral.preparacaoExpirada()) {
            agendarEncerramentoPreparacao();
            return;
        }

        if (partidaGeral.encerrarPreparacao()) {
            System.out.println("[SALA " + codigo + "] " + "Preparação encerrada automaticamente.");
            notificarJogadores();
        }
    }

    private synchronized void cancelarTemporizadorPreparacao() {
        versaoPreparacao++;

        if (tarefaPreparacao != null) {
            tarefaPreparacao.cancel(false);
            tarefaPreparacao = null;
        }
    }

    private synchronized void atualizarTemporizadores() {
        verificarSalaAtiva();
        if (partidaGeral == null) {
            return;
        }
        if (partidaGeral.getFase() == FasePartida.LOJA_PETISCOS) {
            agendarEncerramentoLoja();
        }
        if (partidaGeral.getFase() == FasePartida.PREPARACAO_RODADA) {
            agendarEncerramentoPreparacao();
        } else if (tarefaPreparacao != null) {
            cancelarTemporizadorPreparacao();
        }
    }

    public synchronized UsoPetisco utilizarPetisco(String token, TipoPetiscos tipo, Integer indicePeca) {
        ParticipanteSala participante = buscarPorToken(token);

        if (participante == null) {
            throw new SecurityException("Token inválido.");
        }
        if (!iniciada || partidaGeral == null || partidaGeral.competicaoEncerrada()) {
            throw new IllegalStateException("Não existe uma competição em andamento.");
        }

        Jogador jogador = buscarJogador(participante);

        UsoPetisco uso = partidaGeral.utilizarPetisco(jogador, tipo, indicePeca);

        if (uso.sucesso()) {
            registrarAtividade();

            System.out.println("[SALA " + codigo + "] " + jogador.getNome() + " utilizou " + tipo.getNome());

            atualizarTemporizadores();
            notificarJogadores();
        }
        return uso;
    }

    public synchronized boolean confirmarPreparacao(String token) {
        verificarSalaAtiva();
        ParticipanteSala participante = buscarPorToken(token);

        if (participante == null) {
            throw new SecurityException("Token inválido.");
        }
        if (!iniciada || partidaGeral == null) {
            throw new IllegalStateException("Não existe uma competição em andamento.");
        }
        if (partidaGeral.preparacaoExpirada()) {
            partidaGeral.encerrarPreparacao();

            atualizarTemporizadores();
            notificarJogadores();

            return false;
        }

        Jogador jogador = buscarJogador(participante);

        boolean confirmado = partidaGeral.confirmarPreparacao(jogador);

        if (confirmado) {
            registrarAtividade();
            atualizarTemporizadores();
            notificarJogadores();
        }
        return confirmado;
    }

    private TipoPetiscoMensagem converterPetisco(TipoPetiscos tipo) {
        if (tipo == null) {
            return TipoPetiscoMensagem.PETISCO_NAO_INFORMADO;
        }
        return switch (tipo) {
            case OVO_COLORIDO -> TipoPetiscoMensagem.OVO_COLORIDO;
            case COXINHA -> TipoPetiscoMensagem.COXINHA;
            case TORRESMO -> TipoPetiscoMensagem.TORRESMO;
        };
    }
    private FasePartidaMensagem converterFase(FasePartida fase) {
        return FasePartidaMensagem.valueOf(fase.name());
    }

    private String verificarDisponibilidadeCompra(Jogador jogador, TipoPetiscos tipo) {
        LojaPetiscos loja = partidaGeral.getLojaPetiscos();
        InventarioPetiscos inventario = jogador.getInventarioPetiscos();

        if (!partidaGeral.isLojaAberta()) {
            return "LOJA_FECHADA";
        }
        if (inventario.comprouNestaLoja(loja.getNumeroLoja())) {
            return "JA_COMPROU_NESTA_LOJA";
        }
        if (inventario.comprouMesmoPetiscoNaLojaAnterior(tipo, loja.getNumeroLoja())) {
            return "PETISCO_REPETIDO";
        }
        if (inventario.possuiPetisco()) {
            return "JA_POSSUI_PETISCO";
        }
        if (!jogador.possuiSaldo(tipo.getCusto())) {
            return "SALDO_INSUFICIENTE";
        }

        return "";
    }
    // metodo para o botão para verificar se pode utilizar o petisco //
    private boolean podeUtilizarPetisco(Jogador jogador) {
        InventarioPetiscos inventario = jogador.getInventarioPetiscos();
        TipoPetiscos tipo = inventario.getPetiscoAtual();

        if (tipo == null) {
            return false;
        }

        if (!inventario.podeUsarPetisco(tipo, partidaGeral.getNumeroRodada())) {
            return false;
        }

        Partida partida = partidaGeral.getPartidaAtual();

        if (partida == null) {
            return false;
        }

        if (tipo == TipoPetiscos.TORRESMO) {
            return partidaGeral.getFase() == FasePartida.PREPARACAO_RODADA && !partidaGeral.preparacaoExpirada() && !partidaGeral.isJogadorPreparado(jogador)
                    && !partida.primeiraJogadaRealizada() && partida.podeComprar();
        }

        if (partidaGeral.getFase() != FasePartida.EM_ANDAMENTO) {
            return false;
        }

        if (partida.getJogadorVez() != jogador || partida.isTurnoIniciado()) {
            return false;
        }

        if (tipo == TipoPetiscos.COXINHA) {
            return partida.podeComprar() && jogador.quantidadePecas() > 0;
        }

        return partida.getJogadores().stream().anyMatch(j -> j != jogador && j.quantidadePecas() > 0);
    }

    private synchronized void registrarAtividade() {
        ultimaAtividade = System.nanoTime();
    }


    public void encerrarSala() {
        List<AssinaturaJogo> assinaturasEncerradas;
        synchronized (this) {
            if (recursosLiberados) {
                return;
            }
            removida = true;
            recursosLiberados = true;

            if (tarefaLoja != null) {
                tarefaLoja.cancel(false);
                tarefaLoja = null;
            }
            cancelarTemporizadorPreparacao();
            assinaturasEncerradas = new ArrayList<>(assinaturas);
            assinaturas.clear();
        }

        for (AssinaturaJogo assinatura : assinaturasEncerradas) {
            assinatura.encerrarPeloServidor();
        }

        System.out.println("[SALA " + codigo + "] Encerrada.");
    }

    public synchronized boolean marcarExpirada() {
        if (removida || !deveSerRemovida()) {
            return false;
        }
        removida = true;
        return true;
    }


    public synchronized void validarRequisicao(String token) {
        verificarSalaAtiva();

        ParticipanteSala participante = buscarPorToken(token);

        if (participante == null) {
            throw new SecurityException("Token inválido.");
        }
        if (!participante.permitirRequisicao()) {
            throw Status.RESOURCE_EXHAUSTED.withDescription("Muitas requisições. Aguarde um instante.").asRuntimeException();
        }
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
