package br.com.dominando.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Baralho {
    private final List<Peca> pecas;

    public Baralho() {
        pecas = new ArrayList<>();
        gerarPecas();
    }

    // gera as pecas para o baralho (28) //
    public void gerarPecas(){
        for(int ladoA = 0; ladoA <= 6; ladoA++){
            for (int ladoB = 0; ladoB <= ladoA; ladoB++){
                pecas.add(new Peca(ladoA, ladoB));
            }
        }
    }

    public void embaralhar(){
         Collections.shuffle(pecas);
    }

    public Peca comprarPeca(){
        if (pecas.isEmpty()){
            return null;
        }
        return pecas.removeFirst();
    }

    // METODOS PARA OS PETISCOS //
    public Peca comprarPecaAleatoria() {
        if (pecas.isEmpty()) {return null;}
        int indice = ThreadLocalRandom.current().nextInt(pecas.size());
        return pecas.remove(indice);
    }
    public void devolverPeca(Peca peca) {
        if (peca == null) {
            throw new IllegalArgumentException("A peça não pode ser nula.");
        }
        pecas.add(peca);
    }
    public void devolverPecas(List<Peca> pecasDevolvidas) {
        if (pecasDevolvidas == null || pecasDevolvidas.stream().anyMatch(p -> p == null)) {
            throw new IllegalArgumentException("Lista de peças inválida.");
        }
        pecas.addAll(pecasDevolvidas);
    }
    // METODOS PARA OS PETISCOS //

    // consulta //
    public int quantidadePecas(){
        return pecas.size();
    }

    // devolve a lista como somente leitura para nao poder modificar.//
    public List<Peca> getPecas() {
        return Collections.unmodifiableList(pecas);
    }
}
