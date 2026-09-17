package br.com.dominando.model;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Mesa {
    private final LinkedList<Peca> pecasMesa;

    public Mesa() {
        pecasMesa = new LinkedList<>();
    }

    public List<Peca> getPecasMesa() {
        return Collections.unmodifiableList(pecasMesa);
    }

    // Apenas o service pode manipular a lista.
    public LinkedList<Peca> getListaInterna() {
        return pecasMesa;
    }

    public int quantidadePecasMesa() {
        return pecasMesa.size();
    }
}
