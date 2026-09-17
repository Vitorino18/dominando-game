package br.com.dominando.service;

public class TurnoService {
    private int indiceJogadorDaVez;

    public TurnoService(){
        indiceJogadorDaVez = 0;
    }

    public int getIndiceJogadorDaVez() {
        return indiceJogadorDaVez;
    }

    public void definirPrimeiroJogador(int indice) {
        indiceJogadorDaVez = indice;
    }

    public void proximoTurno(int quantidadeJogadores) {
        indiceJogadorDaVez = (indiceJogadorDaVez + 1) % quantidadeJogadores;
    }

}
