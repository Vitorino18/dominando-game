package br.com.dominando.model;

import java.util.List;
import java.util.ArrayList;
import br.com.dominando.model.*;

public class PartidaGeral {
    private final List<Jogador> jogadores;

    private Partida partidaAtual;

    private Jogador vencedorAnterior;
    private Jogador jogadorInicialPartidaAtual;
    private Jogador campeao;

    // quantidades de tampinhas total para vencer //
    private static final int LIMITE_VITORIAS = 5;

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

        // Registra uma vitória e concede 10 pontos de petiscos //
        vencedor.registrarVitoria();
        vencedorAnterior = vencedor;

        // Verifica se alguém venceu a competição //
        if (vencedor.getVitorias() >= LIMITE_VITORIAS) {
            campeao = vencedor;
            partidaAtual = null;
            return;
        }

        iniciarNovaPartida(vencedor);
    }

    private void reiniciarPartidaEmpatada() {
        iniciarNovaPartida(null);
    }

    private void iniciarNovaPartida(Jogador jogadorInicial) {
        partidaAtual = new Partida(jogadores, jogadorInicial);
        partidaAtual.iniciarPartida();
        jogadorInicialPartidaAtual = partidaAtual.getJogadorVez();
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

    public boolean competicaoEncerrada() {
        return campeao != null;
    }


}
