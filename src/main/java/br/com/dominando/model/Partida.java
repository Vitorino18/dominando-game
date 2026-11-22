package br.com.dominando.model;

import java.util.ArrayList;
import java.util.List;

import br.com.dominando.service.MesaService;
import br.com.dominando.service.PontuacaoService;
import br.com.dominando.service.TurnoService;

public class Partida {
    private final Baralho baralho;
    private final Mesa mesa;
    private boolean comprouNoTurno = true;
    private Peca buchaObrigatoria;

    private final MesaService mesaService;
    private final TurnoService turnoService;
    private final PontuacaoService pontuacaoService;

    private final Jogador jogadorInicial;
    private final List<Jogador> jogadores;


    public Partida(List<String> nomesJogadores) {
        this(criarJogadores(nomesJogadores), null);
    }

    // construtor dos jogadores da partida inicial //
    public Partida(List<Jogador> jogadores, Jogador jogadorInicial){
        if (jogadores.size() < 2 || jogadores.size() > 4) {
            throw new IllegalArgumentException("A partida deve ter entre 2 e 4 jogadores.");
        }
        if (jogadorInicial != null && !jogadores.contains(jogadorInicial)) {
            throw new IllegalArgumentException("O jogador inicial precisa participar da partida.");
        }

        this.baralho = new Baralho();
        this.mesa = new Mesa();

        this.mesaService = new MesaService(mesa);
        this.turnoService = new TurnoService();
        this.pontuacaoService = new PontuacaoService();

        this.jogadores = new ArrayList<>(jogadores);
        this.jogadorInicial = jogadorInicial;
    }

    private static List<Jogador> criarJogadores(List<String> nomesJogadores) {
        List<Jogador> jogadores = new ArrayList<>();

        for (String nome : nomesJogadores) {
            jogadores.add(new Jogador(nome));
        }
        return jogadores;
    }


    // Açoes do jogo //
    public void iniciarPartida() {
        baralho.embaralhar();

        for (Jogador jogador : jogadores) {
            jogador.limparMao();
        }

        for (int rodada = 0; rodada < 7; rodada++) {
            for (Jogador jogador : jogadores) {
                jogador.comprarDoBaralho(baralho);
            }
        }

        if (jogadorInicial == null) {
            definirPrimeiroJogador();
        } else {
            int indice = jogadores.indexOf(jogadorInicial);
            turnoService.definirPrimeiroJogador(indice);
        }

        comprouNoTurno = false;
    }

    public ResultadoJogada jogar(Peca peca, LadoMesa lado) {
        Jogador jogador = getJogadorVez();

        if (!jogador.possuiPeca(peca)) {
            return ResultadoJogada.NAO_POSSUI_PECA;
        }
        if (buchaObrigatoria != null && !peca.equals(buchaObrigatoria)) {
            return ResultadoJogada.DEVE_JOGAR_MAIOR_BUCHA;
        }
        if (!mesaService.jogadaValida(peca, lado)) {
            jogador.registrarGatos();
            finalizarTurno();
            return ResultadoJogada.GATO;
        }

        mesaService.jogarPeca(peca, lado);
        buchaObrigatoria = null;
        jogador.removerPeca(peca);

        if (jogador.quantidadePecas() == 0) {
            return ResultadoJogada.PARTIDA_ENCERRADA;
        }

        finalizarTurno();

        return ResultadoJogada.JOGADA_VALIDA;
    }

    public ResultadoJogada comprarPeca() {
        Jogador jogador = getJogadorVez();

        if (mesaService.existeJogadaPara(jogador)){
         return ResultadoJogada.POSSUI_JOGADA_VALIDA;
        }
        if (comprouNoTurno) {
            return ResultadoJogada.COMPROU_JA_REALIZADA;
        }
        if (!podeComprar()){
            return ResultadoJogada.BARALHO_VAZIO;
        }

        jogador.comprarDoBaralho(baralho);
        comprouNoTurno = true;

        return ResultadoJogada.COMPRA_REALIZADA;
    }

    public ResultadoJogada passarVez() {
        Jogador jogador = getJogadorVez();

        if (mesaService.existeJogadaPara(jogador) && !comprouNoTurno){
            return ResultadoJogada.NAO_PODE_PASSAR;
        }
        if (!comprouNoTurno && podeComprar()){
            return ResultadoJogada.NAO_PODE_PASSAR_SEM_COMPRAR;
        }

        finalizarTurno();
        return ResultadoJogada.VEZ_PASSADA;
    }


    // CONSULTAS //

    public Jogador getJogadorVez() {
        return jogadores.get(turnoService.getIndiceJogadorDaVez());
    }

    public List<Jogador> getJogadores() {
        return List.copyOf(jogadores);
    }

    public Mesa getMesa() {
        return mesa;
    }

    public Baralho getBaralho() {
        return baralho;
    }

    public boolean podeComprar() {
        return baralho.quantidadePecas() > 0;
    }

    public boolean terminouRodada() {
        for (Jogador jogador : jogadores) {
            if (jogador.quantidadePecas() == 0) {
                return true;
            }
        }
        return false;
    }

    public boolean rodadaTravada() {
        if (podeComprar()) {
            return false;
        }

        for (Jogador jogador : jogadores) {
            if (mesaService.existeJogadaPara(jogador)) {
                return false;
            }
        }
        return true;
    }

    public Jogador getVencedor() {
        for (Jogador jogador : jogadores) {
            if (jogador.quantidadePecas() == 0) {
                return jogador;
            }
        }
        return null;
    }

    public List<Jogador> getVencedorRodadaTravada() {
        return pontuacaoService.vencedorRodadaTravada(jogadores);
    }

    public int calcularPontosRodada(Jogador vencedor) {
        return pontuacaoService.calcularPontosRodada(jogadores, vencedor);
    }


    // MÉTODOS PRIVADOS //

    private void finalizarTurno() {
        turnoService.proximoTurno(jogadores.size());
        comprouNoTurno = false;
    }

    private void definirPrimeiroJogador() {
        int maiorbucha = -1;
        int maiorValor = -1;
        int indiceMaior = 0;
        Peca pecaObrigatoria = null;

        for (int i = 0; i < jogadores.size(); i++) {
            for (Peca peca : jogadores.get(i).getMao()) {
               // maior bucha //
                if (peca.bucha() && peca.getLadoA() > maiorbucha) {
                    maiorbucha = peca.getLadoA();
                    indiceMaior = i;
                    pecaObrigatoria = peca;
                }
                // Guarda a peça de maior valor como alternativa //
                int valorPeca = peca.getLadoA() + peca.getLadoB();

                if (valorPeca > maiorValor) {
                    maiorValor = valorPeca;
                }
            }
        }

        // Se ninguém recebeu bucha, procura a peça de maior valor //
        if (pecaObrigatoria == null) {
            for (int i = 0; i < jogadores.size(); i++) {
                for (Peca peca : jogadores.get(i).getMao()) {
                    int valorPeca = peca.getLadoA() + peca.getLadoB();

                    if (valorPeca == maiorValor) {
                        indiceMaior = i;
                        pecaObrigatoria = peca;
                        break;
                    }
                }

                if (pecaObrigatoria != null) {
                    break;
                }
            }
        }

        turnoService.definirPrimeiroJogador(indiceMaior);
        buchaObrigatoria = pecaObrigatoria;
    }


    @Override
    public String toString() {

        StringBuilder texto = new StringBuilder();

        texto.append("\n========== Desce Uma  ==========\n\n");

        for (Jogador jogador : jogadores) {

            texto.append(jogador.getNome())
                    .append(" | Pontos: ")
                    .append(jogador.getPontuacao())
                    .append(" | Gatos: ")
                    .append(jogador.getErros())
                    .append("\n");

            texto.append("Mão: ")
                    .append(jogador.getMao())
                    .append("\n\n");
        }

        texto.append("Jogador da vez: ")
                .append(getJogadorVez().getNome())
                .append("\n\n");

        texto.append("Mesa: ")
                .append(mesa.getPecasMesa())
                .append("\n");

        texto.append("Baralho: ")
                .append(baralho.quantidadePecas())
                .append(" peças restantes\n");

        texto.append("===============================\n");

        return texto.toString();
    }

}
