package br.com.dominando.server;


import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;

public class LimitadorRequisicoes {
    private final Deque<Long> requisicoes = new ArrayDeque<>();
    private final int limite;
    private final long intervaloNanos = TimeUnit.SECONDS.toNanos(1);

    public LimitadorRequisicoes(int limite) {
        if (limite <= 0) {
            throw new IllegalArgumentException("O limite deve ser positivo.");
        }
        this.limite = limite;
    }

    public synchronized boolean permitirRequisicao() {
        long agora = System.nanoTime();

        while (!requisicoes.isEmpty() && agora - requisicoes.peekFirst() >= intervaloNanos) {
            requisicoes.removeFirst();
        }

        if (requisicoes.size() >= limite) {
            return false;
        }

        requisicoes.addLast(agora);

        return true;
    }
}
