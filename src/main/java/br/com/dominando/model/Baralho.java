package br.com.dominando.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Baralho {
    private final List<Peca> pecas;

    public Baralho() {
        pecas = new ArrayList<>();
        gerarPecas();
    }

    // gera as pecas para o baralho (28)
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

    // consulta //
    public int quantidadePecas(){
        return pecas.size();
    }

    //devolve a lista como somente leitura para nao poder modificar.//
    public List<Peca> getPecas() {
        return Collections.unmodifiableList(pecas);
    }
}
