package br.com.dominando.frontend.network;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Disposable;

import br.com.dominando.grpc.ConsultarSalaRequest;
import br.com.dominando.grpc.ConsultarSalaResponse;
import br.com.dominando.grpc.CriarSalaRequest;
import br.com.dominando.grpc.CriarSalaResponse;
import br.com.dominando.grpc.DesceUmaServiceGrpc;
import br.com.dominando.grpc.EntrarSalaRequest;
import br.com.dominando.grpc.EntrarSalaResponse;
import br.com.dominando.grpc.IniciarPartidaRequest;
import br.com.dominando.grpc.IniciarPartidaResponse;

import io.grpc.ManagedChannel;
import io.grpc.okhttp.OkHttpChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class ClienteJogo implements Disposable {

    private final ManagedChannel channel;
    private final DesceUmaServiceGrpc.DesceUmaServiceStub stub;
    private volatile boolean disposed;

    public ClienteJogo(String host, int porta) {
        channel = OkHttpChannelBuilder.forAddress(host, porta).usePlaintext().build();
        stub = DesceUmaServiceGrpc.newStub(channel);
    }

    public void criarSala(String nome, Consumer<CriarSalaResponse> sucesso, Consumer<Throwable> erro) {
        CriarSalaRequest request = CriarSalaRequest.newBuilder().setNomeCriador(nome.trim()).build();
        stub.withDeadlineAfter(8, TimeUnit.SECONDS).criarSala(request, observar(sucesso, erro));
    }

    public void entrarSala(String codigo, String nome, Consumer<EntrarSalaResponse> sucesso, Consumer<Throwable> erro) {
        EntrarSalaRequest request = EntrarSalaRequest.newBuilder().setCodigoSala(codigo.trim().toUpperCase(Locale.ROOT)).setNomeJogador(nome.trim()).build();
        stub.withDeadlineAfter(8, TimeUnit.SECONDS).entrarSala(request, observar(sucesso, erro));
    }

    public void consultarSala(String codigo, String token, Consumer<ConsultarSalaResponse> sucesso, Consumer<Throwable> erro) {
        ConsultarSalaRequest request = ConsultarSalaRequest.newBuilder().setCodigoSala(codigo).setTokenJogador(token).build();
        stub.withDeadlineAfter(8, TimeUnit.SECONDS).consultarSala(request, observar(sucesso, erro));
    }

    public void iniciarPartida(String codigo, String token, Consumer<IniciarPartidaResponse> sucesso, Consumer<Throwable> erro) {
        IniciarPartidaRequest request = IniciarPartidaRequest.newBuilder().setCodigoSala(codigo).setTokenCriador(token).build();
        stub.withDeadlineAfter(8, TimeUnit.SECONDS).iniciarPartida(request, observar(sucesso, erro));
    }

    private <T> StreamObserver<T> observar(Consumer<T> sucesso, Consumer<Throwable> erro) {
        return new StreamObserver<T>() {
            @Override
            public void onNext(T resposta) {
                Gdx.app.postRunnable(() -> {
                    if (!disposed) {
                        sucesso.accept(resposta);
                    }
                });
            }

            @Override
            public void onError(Throwable falha) {
                Gdx.app.postRunnable(() -> {
                    if (!disposed) {
                        erro.accept(falha);
                    }
                });
            }

            @Override
            public void onCompleted() {
            }
        };
    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }

        disposed = true;
        channel.shutdownNow();
    }
}
