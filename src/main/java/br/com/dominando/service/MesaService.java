package br.com.dominando.service;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import br.com.dominando.model.*;

public class MesaService {
    private final Mesa mesa;

    public MesaService(Mesa mesa) {
        this.mesa = mesa;
    }

    private LinkedList<Peca> lista() {
        return mesa.getListaInterna();
    }

    public boolean estaVazia() {
        return lista().isEmpty();
    }

    public int getPontaEsquerda() {
        return lista().getFirst().getLadoA();
    }

    public int getPontaDireita() {
        return lista().getLast().getLadoB();
    }

    public boolean jogadaValida(Peca peca, LadoMesa lado) {
        if (estaVazia()) {
            return true;
        }
        if (lado == LadoMesa.ESQUERDA) {
            return peca.combina(getPontaEsquerda());
        }
        return peca.combina(getPontaDireita());
    }

    public boolean jogarPeca(Peca peca, LadoMesa lado) {
        if (!jogadaValida(peca, lado)) {
            return false;
        }
        if (estaVazia()) {
            lista().add(peca);
            return true;
        }

        if (lado == LadoMesa.ESQUERDA) {
            if (peca.getLadoB() == getPontaEsquerda()) {
                lista().addFirst(peca);
            } else {
                lista().addFirst(peca.inverter());
            }
        } else {
            if (peca.getLadoA() == getPontaDireita()) {
                lista().addLast(peca);
            } else {
                lista().addLast(peca.inverter());
            }
        }

        return true;
    }

    public boolean existeJogadaPara(Jogador jogador) {
        if (estaVazia()) {
            return true;
        }
        for (Peca peca : jogador.getMao()) {
            if (jogadaValida(peca, LadoMesa.ESQUERDA) || jogadaValida(peca, LadoMesa.DIREITA)) {
                return true;
            }
        }

        return false;
    }
}
