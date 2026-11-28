
package br.com.dominando.server;

import br.com.dominando.grpc.EstadoJogoResponse;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import io.grpc.Status;

public class AssinaturaJogo {

    private final String token;
    private final StreamObserver<EstadoJogoResponse> observer;
    private final Executor executor;
    private final Runnable remover;

    private final AtomicBoolean ativa = new AtomicBoolean(true);

    private EstadoJogoResponse pendente;
    private boolean enviando;

    private final Object bloqueioObserver = new Object();

    public AssinaturaJogo(String token, StreamObserver<EstadoJogoResponse> observer, Executor executor, Runnable remover) {
        this.token = token;
        this.observer = observer;
        this.executor = executor;
        this.remover = remover;
    }

    public void publicar(EstadoJogoResponse estado) {
        synchronized (this) {
            if (!ativa.get()) {
                return;
            }

            pendente = estado;

            if (enviando) {
                return;
            }

            enviando = true;
        }

        try {
            executor.execute(this::processarEnvios);
        } catch (RuntimeException e) {
            encerrar();
        }
    }

    private void processarEnvios() {
        while (ativa.get()) {
            EstadoJogoResponse estado;

            synchronized (this) {
                estado = pendente;
                pendente = null;

                if (estado == null) {
                    enviando = false;
                    return;
                }
            }

            try {
                synchronized (bloqueioObserver) {

                    if (!ativa.get()) {
                        return;
                    }
                    observer.onNext(estado);
                }
            } catch (RuntimeException e) {
                encerrar();
                return;
            }
        }
    }

    // encerra a assinatura apenas uma vez //
    public void encerrar() {
        if (ativa.compareAndSet(true, false)) {
            synchronized (this) {
                pendente = null;
            }

            remover.run();

            System.out.println("[STREAM] Assinatura encerrada");
        }
    }

    public void encerrarPeloServidor() {
        if (!ativa.compareAndSet(true, false)) {
            return;
        }
        synchronized (this) {
            pendente = null;
        }

        try {
            synchronized (bloqueioObserver) {
                observer.onError(Status.NOT_FOUND.withDescription("A sala foi encerrada pelo servidor.").asRuntimeException());
            }
        } catch (RuntimeException e) {
            System.out.println("[STREAM] Cliente já desconectado.");
        } finally {
            remover.run();
            System.out.println("[STREAM] Assinatura encerrada pelo servidor.");
        }
    }

    public void registrarCancelamento() {
        if (observer instanceof ServerCallStreamObserver<?> servidor) {
            servidor.setOnCancelHandler(this::encerrar);
        }
    }


    public String getToken() {
        return token;
    }
    public boolean isAtiva() {
        return ativa.get();
    }
}
