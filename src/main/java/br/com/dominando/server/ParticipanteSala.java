package br.com.dominando.server;
import java.util.UUID;

public class ParticipanteSala {
    private final String nome;
    private final String token;
    private final boolean criador;

    public ParticipanteSala(String nome, boolean criador) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome não pode ser vazio.");
        }

        this.nome = nome;
        this.criador = criador;
        this.token = UUID.randomUUID().toString();
    }

    public String getNome() {
        return nome;
    }
    public String getToken() {
        return token;
    }
    public boolean isCriador() {
        return criador;
    }
}
