package br.com.dominando.server;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SalaService {
    private static final String CARACTERES = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private static final int TAMANHO_CODIGO = 6;

    private final SecureRandom random = new SecureRandom();

    private final Map<String, Sala> salas = new ConcurrentHashMap<>();


    public Sala criarSala(String nomeCriador) {
        validarNome(nomeCriador);
        String codigo;
        Sala sala;

        do {
            codigo = gerarCodigo();
            sala = new Sala(codigo, nomeCriador);

        } while (salas.putIfAbsent(codigo, sala) != null);
        return sala;
    }

    public Sala buscarSala(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("O código da sala não pode ser vazio.");
        }
        return salas.get(codigo.trim().toUpperCase());
    }

    public ParticipanteSala entrarNaSala(String codigo, String nomeJogador) {
        validarNome(nomeJogador);
        Sala sala = buscarSala(codigo);

        if (sala == null) {
            throw new IllegalArgumentException("Sala não encontrada.");
        }

        return sala.adicionarJogador(nomeJogador);
    }

    public void iniciarSala(String codigo, String tokenCriador) {
        Sala sala = buscarSala(codigo);

        if (sala == null) {
            throw new IllegalArgumentException("Sala não encontrada.");
        }
        sala.iniciarPartida(tokenCriador);
    }

    public boolean removerSala(String codigo) {
        Sala sala = buscarSala(codigo);

        if (sala == null) {
            return false;
        }
        return salas.remove(sala.getCodigo(), sala);
    }

    public int quantidadeSalas() {
        return salas.size();
    }

    private String gerarCodigo() {
        StringBuilder codigo = new StringBuilder();

        for (int i = 0; i < TAMANHO_CODIGO; i++) {
            int indice = random.nextInt(CARACTERES.length());
            codigo.append(CARACTERES.charAt(indice));
        }
        return codigo.toString();
    }

    private void validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do jogador não pode ser vazio.");
        }
    }
}
