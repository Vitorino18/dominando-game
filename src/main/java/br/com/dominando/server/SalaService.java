package br.com.dominando.server;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SalaService {
    private static final String CARACTERES = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private static final int TAMANHO_CODIGO = 6;

    private final SecureRandom random = new SecureRandom();

    private final Map<String, Sala> salas = new ConcurrentHashMap<>();

    private boolean encerrado = false;
    private final ScheduledExecutorService executorLimpeza = Executors.newSingleThreadScheduledExecutor(tarefa -> {
                Thread thread = new Thread(tarefa, "limpeza-salas");
                thread.setDaemon(true);
                return thread;
            });


    public SalaService() {
        executorLimpeza.scheduleWithFixedDelay(this::limparSalasExpiradas, ConfiguracaoServidor.INTERVALO_LIMPEZA_SEGUNDOS,
                ConfiguracaoServidor.INTERVALO_LIMPEZA_SEGUNDOS, TimeUnit.SECONDS);
    }



    public synchronized Sala criarSala(String nomeCriador) {
        if (encerrado) {
            throw new IllegalStateException("O servidor está sendo encerrado.");
        }

        nomeCriador = ParticipanteSala.validarNome(nomeCriador);
        if (salas.size() >= ConfiguracaoServidor.MAX_SALAS) {
            throw new IllegalStateException("O servidor atingiu o limite de salas. Tente novamente mais tarde.");
        }

        String codigo;
        Sala sala;

        do {
            codigo = gerarCodigo();
            sala = new Sala(codigo, nomeCriador);

        } while (salas.putIfAbsent(codigo, sala) != null);

        System.out.println("[SALA] Criada: " + codigo + " | Total: " + salas.size() + "/" + ConfiguracaoServidor.MAX_SALAS);

        return sala;
    }

    public Sala buscarSala(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("O código da sala não pode ser vazio.");
        }

        codigo = codigo.trim().toUpperCase(Locale.ROOT);

        if (!codigo.matches("[A-Z0-9]{6}")) {
            throw new IllegalArgumentException("Código da sala inválido.");
        }
        return salas.get(codigo);
    }

    public ParticipanteSala entrarNaSala(String codigo, String nomeJogador) {
        nomeJogador = ParticipanteSala.validarNome(nomeJogador);
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

        sala.encerrarSala();
        boolean removida = salas.remove(sala.getCodigo(), sala);

        if (removida) {
            System.out.println("[SALA] Removida: " + sala.getCodigo() + " | Total: " + salas.size() + "/" + ConfiguracaoServidor.MAX_SALAS);
        }
        return removida;
    }

    public void encerrar() {
        synchronized (this) {
            if (encerrado) {
                return;
            }
            encerrado = true;
        }

        executorLimpeza.shutdownNow();
        for (Sala sala : salas.values()) {
            removerSala(sala.getCodigo());
        }

        System.out.println("[SERVIDOR] Serviço de salas encerrado.");
    }

    private void limparSalasExpiradas() {
        for (Sala sala : salas.values()) {
            try {
                if (sala.marcarExpirada()) {
                    removerSala(sala.getCodigo());
                }
            } catch (RuntimeException e) {
                System.err.println("[LIMPEZA] Erro ao remover sala: " + sala.getCodigo());
            }
        }
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
}
