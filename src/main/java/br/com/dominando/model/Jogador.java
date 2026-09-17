package br.com.dominando.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Jogador {
    private final String nome;
    private int pontuacao;
    private int erros;
    private final List<Peca> mao;

    public Jogador(String nome){
        this.nome = nome;
        this.pontuacao = 0;
        this.erros = 0;
        this.mao = new ArrayList<>();
    }

    public boolean removerPeca(Peca peca){
         return mao.remove(peca);
    }

    public int quantidadePecas(){
        return mao.size();
    }

    public void comprarDoBaralho(Baralho baralho){
        Peca peca = baralho.comprarPeca();
        if (peca != null){
            mao.add(peca);
        }
    }

    public boolean possuiPeca(Peca peca) {
        return mao.contains(peca);
    }

    public int valorTotalMao(){
        int soma = 0;
        for (Peca peca : mao){
            soma += peca.valorTotal();
        }
        return soma;
    }

    public void adicionarPontos(int pontos) {
        pontuacao += pontos;
    }
    public void registrarGatos(){
        erros++;
    }

    // gets //
    public String getNome() {
        return nome;
    }
    public int getPontuacao() {
        return pontuacao;
    }
    public List<Peca> getMao() {
        return Collections.unmodifiableList(mao);
    }
    public int getErros(){
        return erros;
    }

    public Peca getPeca(int indicePeca){
        if(indicePeca < 0 || indicePeca >= mao.size()){
            return null;
        }
        return mao.get(indicePeca);
    }

    @Override
    public String toString() {
        return nome + " -> " + mao;
    }
}


