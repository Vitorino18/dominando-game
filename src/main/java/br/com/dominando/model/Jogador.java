package br.com.dominando.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Jogador {
    private final String nome;
    private int pontuacao;
    private int erros;
    private final List<Peca> mao;
    private int vitorias;
    private int pontosPetiscos;
    private final InventarioPetiscos inventarioPetiscos;

    private static final int MOEDAS_POR_VITORIA = 2;

    public Jogador(String nome){
        this.nome = nome;
        this.pontuacao = 0;
        this.erros = 0;
        this.mao = new ArrayList<>();
        this.vitorias = 0;
        this.pontosPetiscos = 0;

        this.inventarioPetiscos = new InventarioPetiscos();
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

    public void registrarVitoria() {
        vitorias++;
        pontosPetiscos += MOEDAS_POR_VITORIA;
    }

    public void limparMao() {
        mao.clear();
    }

    public boolean possuiSaldo(int custo) {
        return custo >= 0 && pontosPetiscos >= custo;
    }

    public boolean gastarPontosPetiscos(int custo) {
        if (custo <= 0 || !possuiSaldo(custo)) {
            return false;
        }
        pontosPetiscos -= custo;
        return true;
    }

    // OS METODOS DOS PETISCOS //
    public void adicionarPeca(Peca peca) {
        if (peca == null) {
            throw new IllegalArgumentException("A peça não pode ser nula.");
        }
        mao.add(peca);
    }

    public Peca substituirPeca(int indice, Peca novaPeca) {
        if (indice < 0 || indice >= mao.size()) {
            throw new IllegalArgumentException("Índice de peça inválido.");
        }
        if (novaPeca == null) {
            throw new IllegalArgumentException("A nova peça não pode ser nula.");
        }
        return mao.set(indice, novaPeca);
    }

    public List<Peca> retirarTodasPecas() {
        List<Peca> pecasRetiradas = new ArrayList<>(mao);
        mao.clear();
        return pecasRetiradas;
    }
    // OS METODOS DOS PETISCOS //

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

    public int getPontosPetiscos() {
        return pontosPetiscos;
    }
    public int getVitorias() {
        return vitorias;
    }
    public InventarioPetiscos getInventarioPetiscos() {
        return inventarioPetiscos;
    }

    @Override
    public String toString() {
        return nome + " -> " + mao;
    }
}


