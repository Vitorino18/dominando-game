package br.com.dominando.model;

import java.util.List;
import java.util.ArrayList;
import br.com.dominando.service.*;
import br.com.dominando.model.*;
import br.com.dominando.service.PetiscoService;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class PartidaGeral {
    private final List<Jogador> jogadores;

    private Partida partidaAtual;

    private Jogador vencedorAnterior;
    private Jogador jogadorInicialPartidaAtual;
    private Jogador campeao;

    private int numeroRodada;
    private FasePartida fase;

    // Controle da loja de petiscos //
    private final LojaPetiscos lojaPetiscos;
    private final LojaPetiscosService lojaPetiscosService;

    private final PetiscoService petiscoService = new PetiscoService();
    private final Set<Jogador> jogadoresPreparados = new HashSet<>();

    private long prazoPreparacaoNanos;

    // quantidades de tampinha total para vencer //
    private static final int LIMITE_VITORIAS = 5;
    public static final int DURACAO_PREPARACAO_SEGUNDOS = 15;

    public PartidaGeral(List<String> nomesJogadores) {
        if (nomesJogadores == null) {
            throw new IllegalArgumentException("A lista de jogadores não pode ser nula.");
        }
        if (nomesJogadores.size() < 2 || nomesJogadores.size() > 4) {
            throw new IllegalArgumentException("A competição deve ter entre 2 e 4 jogadores.");
        }
        this.jogadores = new ArrayList<>();

        for (String nome : nomesJogadores) {
            if (nome == null || nome.isBlank()) {
                throw new IllegalArgumentException("O nome do jogador não pode ser vazio.");
            }
            jogadores.add(new Jogador(nome));
        }

        this.numeroRodada = 1;
        this.fase = FasePartida.NAO_INICIADA;
        this.lojaPetiscos = new LojaPetiscos();
        this.lojaPetiscosService = new LojaPetiscosService(lojaPetiscos);
    }

    public void inicioCompeticao(){
        if (partidaAtual != null) {
            throw new IllegalStateException("A competição já foi iniciada.");
        }
        iniciarPrimeiraPartida();
    }

    private void iniciarPrimeiraPartida() {
        partidaAtual = new Partida(jogadores, null);
        partidaAtual.iniciarPartida();

        // Guarda quem foi escolhido pela maior bucha //
        jogadorInicialPartidaAtual = partidaAtual.getJogadorVez();
        fase = FasePartida.EM_ANDAMENTO;
    }

    public void verificarFimPartida() {
        if (campeao != null || partidaAtual == null) {
            return;
        }

        // Verifica se alguém esvaziou a mão //
        if (partidaAtual.terminouRodada()) {
            Jogador vencedor = partidaAtual.getVencedor();
            finalizarPartida(vencedor);
            return;
        }

        // Verifica se a partida ficou travada //
        if (partidaAtual.rodadaTravada()) {
            List<Jogador> vencedores = partidaAtual.getVencedorRodadaTravada();

            // Mais de um jogador com a menor pontuação //
            if (vencedores.size() > 1) {
                reiniciarPartidaEmpatada();
                return;
            }

            finalizarPartida(vencedores.getFirst());
        }
    }


    private void finalizarPartida(Jogador vencedor) {
        // Calcula os pontos da partida individual //
        partidaAtual.calcularPontosRodada(vencedor);

        vencedor.registrarVitoria();
        vencedorAnterior = vencedor;

        expirarPetiscosDaRodada();

        // Verifica se alguém venceu a competição //
        if (vencedor.getVitorias() >= LIMITE_VITORIAS) {
            campeao = vencedor;
            partidaAtual = null;
            fase = FasePartida.ENCERRADA;
            return;
        }

        // Abre a loja ao final de uma rodada par //
        if (numeroRodada % 2 == 0) {
            abrirLojaPetiscos();
            return;
        }

        // Rodada ímpar: inicia a próxima normalmente //
        avancarRodada();
    }

    private void expirarPetiscosDaRodada() {
        for (Jogador jogador : jogadores) {
            jogador.getInventarioPetiscos().expirarPetisco(numeroRodada);
        }
    }

    private void abrirLojaPetiscos() {
        lojaPetiscos.abrir(numeroRodada);
        fase = FasePartida.LOJA_PETISCOS;

        System.out.println("[LOJA] Aberta após a rodada " + numeroRodada);
    }

    public ResultadoPetiscosLoja comprarPetisco(Jogador jogador, TipoPetiscos tipo) {
        if (fase != FasePartida.LOJA_PETISCOS) {
            return ResultadoPetiscosLoja.LOJA_FECHADA;
        }
        if (jogador == null) {
            return ResultadoPetiscosLoja.JOGADOR_INVALIDO;
        }

        return lojaPetiscosService.comprarPetisco(jogador, tipo);
    }

    public boolean verificarEncerramentoLoja() {
        if (fase != FasePartida.LOJA_PETISCOS) {
            return false;
        }
        if (!lojaPetiscos.tempoExpirado()) {
            return false;
        }

        lojaPetiscos.fechar();
        System.out.println("[LOJA] Encerrada.");
        avancarRodada();

        return true;
    }

    private void avancarRodada() {
        numeroRodada++;
        iniciarNovaPartida(vencedorAnterior);
    }

    private void reiniciarPartidaEmpatada() {
        System.out.println("Empate na rodada " + numeroRodada + ". Reiniciando...");
        iniciarNovaPartida(vencedorAnterior);
    }

    private void iniciarNovaPartida(Jogador jogadorInicial) {
        partidaAtual = new Partida(jogadores, jogadorInicial);
        partidaAtual.iniciarPartida();
        jogadorInicialPartidaAtual = partidaAtual.getJogadorVez();
        prepararRodada();
    }

    private void prepararRodada() {
        jogadoresPreparados.clear();
        for (Jogador jogador : jogadores) {
            boolean possuiTorresmo = jogador.getInventarioPetiscos().podeUsarPetisco(TipoPetiscos.TORRESMO, numeroRodada);
            if (!possuiTorresmo) {
                jogadoresPreparados.add(jogador);
            }
        }

        fase = FasePartida.PREPARACAO_RODADA;
        prazoPreparacaoNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(DURACAO_PREPARACAO_SEGUNDOS);

        verificarJogadoresPreparados();
    }

    private void verificarJogadoresPreparados() {
        if (fase != FasePartida.PREPARACAO_RODADA) {
            return;
        }
        if (jogadoresPreparados.size() == jogadores.size()) {
            fase = FasePartida.EM_ANDAMENTO;
            System.out.println("Rodada " + numeroRodada + " iniciada.");
        }
    }

    public boolean confirmarPreparacao(Jogador jogador) {
        if (fase != FasePartida.PREPARACAO_RODADA) {
            return false;
        }
        if (jogador == null || !jogadores.contains(jogador)) {
            return false;
        }
        if (!jogadoresPreparados.add(jogador)) {
            return false;
        }

        verificarJogadoresPreparados();
        return true;
    }

    public UsoPetisco utilizarPetisco(Jogador jogador, TipoPetiscos tipo, Integer indicePeca) {
        if (jogador == null || !jogadores.contains(jogador)) {
            return UsoPetisco.resultado(ResultadoUsoPetisco.JOGADOR_INVALIDO);
        }
        if (tipo == TipoPetiscos.TORRESMO && fase == FasePartida.PREPARACAO_RODADA && jogadoresPreparados.contains(jogador)) {
            return UsoPetisco.resultado(ResultadoUsoPetisco.JOGADOR_JA_PREPARADO);
        }
        if (preparacaoExpirada()) {
            return UsoPetisco.resultado(ResultadoUsoPetisco.FASE_INVALIDA);
        }

        UsoPetisco uso = petiscoService.utilizarPetisco(partidaAtual, jogador, tipo, numeroRodada, fase, indicePeca);

        if (uso.sucesso() && tipo == TipoPetiscos.TORRESMO) {
            jogadoresPreparados.add(jogador);
            verificarJogadoresPreparados();
        }
        return uso;
    }

    public boolean encerrarPreparacao() {
        if (fase != FasePartida.PREPARACAO_RODADA) {
            return false;
        }
        jogadoresPreparados.addAll(jogadores);
        verificarJogadoresPreparados();

        return true;
    }



    public boolean preparacaoExpirada() {
        return fase == FasePartida.PREPARACAO_RODADA && System.nanoTime() - prazoPreparacaoNanos >= 0;
    }
    public long getTempoPreparacaoRestanteSegundos() {
        if (fase != FasePartida.PREPARACAO_RODADA) {
            return 0;
        }
        long restante = Math.max(0, prazoPreparacaoNanos - System.nanoTime());
        return (restante + 999_999_999L) / 1_000_000_000L;
    }
    public boolean podeRealizarJogadas() {
        return fase == FasePartida.EM_ANDAMENTO;
    }
    public boolean isLojaAberta() {
        return fase == FasePartida.LOJA_PETISCOS && lojaPetiscos.isAberta();
    }
    public boolean isJogadorPreparado(Jogador jogador) {
        return jogadoresPreparados.contains(jogador);
    }
    public List<Jogador> getJogadores() {
        return List.copyOf(jogadores);
    }
    public Partida getPartidaAtual() {
        return partidaAtual;
    }
    public Jogador getVencedorAnterior() {
        return vencedorAnterior;
    }
    public Jogador getJogadorInicialPartidaAtual() {
        return jogadorInicialPartidaAtual;
    }
    public Jogador getCampeao() {
        return campeao;
    }
    public int getNumeroRodada() {
        return numeroRodada;
    }
    public FasePartida getFase() {
        return fase;
    }
    public LojaPetiscos getLojaPetiscos() {
        return lojaPetiscos;
    }
    public boolean competicaoEncerrada() {
        return campeao != null;
    }


}
