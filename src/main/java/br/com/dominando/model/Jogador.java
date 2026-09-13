package br.com.dominando.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Jogador {
    private final String nome;
    private int pontuacao;
    private final List<Peca> mao;

    public Jogador(String nome){
        this.nome = nome;
        this.pontuacao = 0;
        this.mao = new ArrayList<>();
    }

    public String getNome() {
        return nome;
    }
    public int getPontuacao() {
        return pontuacao;
    }

    public List<Peca> getMao() {
        return Collections.unmodifiableList(mao);
    }

    public void adicionarPeca(Peca peca){
        mao.add(peca);
    }
    public void removerPeca(Peca peca){
        mao.remove(peca);
    }

    public int quantidadePecas(){
        return mao.size();
    }

    public void comprarDoBaralho(Baralho baralho){
        Peca peca = baralho.comprarPeca();
        if (peca != null){
            adicionarPeca(peca);
        }
    }

    public boolean possuiPeca(Peca peca) {
        return mao.contains(peca);
    }

    public void adicionarPontos(int pontos) {
        pontuacao += pontos;
    }

    @Override
    public String toString() {
        return nome + " -> " + mao;
    }
}


