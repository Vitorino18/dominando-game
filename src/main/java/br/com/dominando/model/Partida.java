package br.com.dominando.model;

import java.util.ArrayList;
import java.util.List;

import br.com.dominando.service.MesaService;
import br.com.dominando.service.PontuacaoService;
import br.com.dominando.service.TurnoService;

public class Partida {
    private final Baralho baralho;
    private final Mesa mesa;

    private final MesaService mesaService;
    private final TurnoService turnoService;
    private final PontuacaoService pontuacaoService;

    private final List<Jogador> jogadores;

    public Partida(List<String> nomesJogadores) {
        if (nomesJogadores.size() < 2 || nomesJogadores.size() > 4) {
            throw new IllegalArgumentException("A partida deve ter entre 2 e 4 jogadores.");
        }
        this.baralho = new Baralho();
        this.mesa = new Mesa();

        this.mesaService = new MesaService(mesa);
        this.turnoService = new TurnoService();
        this.pontuacaoService = new PontuacaoService();

        this.jogadores = new ArrayList<>();
        for (String nome : nomesJogadores) {
            jogadores.add(new Jogador(nome));
        }
    }


    // Açoes do jogo //
    public void iniciarPartida() {
        baralho.embaralhar();
        for (int rodada = 0; rodada < 7; rodada++) {
            for (Jogador jogador : jogadores) {
                jogador.comprarDoBaralho(baralho);
            }
        }
        definirPrimeiroJogador();
    }

    public ResultadoJogada jogar(Peca peca, LadoMesa lado) {
        Jogador jogador = getJogadorVez();

        if (!jogador.possuiPeca(peca)) {
            return ResultadoJogada.NAO_POSSUI_PECA;
        }
        if (!mesaService.jogadaValida(peca, lado)) {
            jogador.registrarGatos();
            finalizarTurno();
            return ResultadoJogada.GATO;
        }

        mesaService.jogarPeca(peca, lado);
        jogador.removerPeca(peca);

        if (jogador.quantidadePecas() == 0) {
            return ResultadoJogada.PARTIDA_ENCERRADA;
        }

        finalizarTurno();
        return ResultadoJogada.JOGADA_VALIDA;
    }

    public boolean comprarPeca() {
        if (!podeComprar()) {
            return false;
        }

        getJogadorVez().comprarDoBaralho(baralho);
        return true;
    }

    public void passarVez() {
        finalizarTurno();
    }


    // CONSULTAS //

    public Jogador getJogadorVez() {
        return jogadores.get(turnoService.getIndiceJogadorDaVez());
    }

    public List<Jogador> getJogadores() {
        return jogadores;
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

    public Jogador getVencedorRodadaTravada() {
        return pontuacaoService.vencedorRodadaTravada(jogadores);
    }

    public int calcularPontosRodada(Jogador vencedor) {
        return pontuacaoService.calcularPontosRodada(jogadores, vencedor);
    }


    // MÉTODOS PRIVADOS //

    private void finalizarTurno() {
        turnoService.proximoTurno(jogadores.size());
    }

    private void definirPrimeiroJogador() {
        int maiorCarroca = -1;
        int indiceMaior = 0;

        for (int i = 0; i < jogadores.size(); i++) {
            int carroca = maiorBucha(jogadores.get(i));
            if (carroca > maiorCarroca) {
                maiorCarroca = carroca;
                indiceMaior = i;
            }
        }
        turnoService.definirPrimeiroJogador(indiceMaior);
    }

    private int maiorBucha(Jogador jogador) {
        int maior = -1;

        for (Peca peca : jogador.getMao()) {
            if (peca.bucha() && peca.getLadoA() > maior) {
                maior = peca.getLadoA();
            }
        }
        return maior;
    }

    @Override
    public String toString() {

        StringBuilder texto = new StringBuilder();

        texto.append("\n========== DOMINANDO ==========\n\n");

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
