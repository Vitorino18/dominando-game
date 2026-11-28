package br.com.dominando.server;
import java.util.Stack;
import java.util.UUID;

public class ParticipanteSala {
    private final String nome;
    private final String token;
    private final boolean criador;
    private final LimitadorRequisicoes limitador = new LimitadorRequisicoes(ConfiguracaoServidor.MAX_REQUISICOES_POR_SEGUNDO);

    public ParticipanteSala(String nome, boolean criador) {
        this.nome = nome;
        this.criador = criador;
        this.token = UUID.randomUUID().toString();
    }

    public static String validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do jogador não pode ser vazio.");
        }

        nome = nome.strip();
        if (nome.length() > ConfiguracaoServidor.MAX_TAMANHO_NOME) {
            throw new IllegalArgumentException("O nome deve possuir no máximo " + ConfiguracaoServidor.MAX_TAMANHO_NOME + " caracteres.");
        }
        if (!nome.matches("[\\p{L}\\p{M}\\p{N} _-]+")) {
            throw new IllegalArgumentException("O nome contém caracteres inválidos.");
        }
        return nome;
    }

    public boolean permitirRequisicao() {
        return limitador.permitirRequisicao();
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
