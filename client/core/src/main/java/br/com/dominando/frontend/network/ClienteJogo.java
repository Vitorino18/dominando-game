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
import br.com.dominando.grpc.EstadoJogoRequest;
import br.com.dominando.grpc.EstadoJogoResponse;
import br.com.dominando.grpc.AcaoJogoResponse;
import br.com.dominando.grpc.JogarPecaRequest;
import br.com.dominando.grpc.LadoJogada;
import br.com.dominando.grpc.AcaoSimplesRequest;

import io.grpc.Context;
import io.grpc.Status;
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
    private Context.CancellableContext acompanhamento;

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

    public void jogarPeca(String codigo, String token, int indice, LadoJogada lado, Consumer<AcaoJogoResponse> sucesso, Consumer<Throwable> erro) {
        JogarPecaRequest request = JogarPecaRequest.newBuilder().setCodigoSala(codigo).setTokenJogador(token).setIndicePeca(indice).setLado(lado).build();
        stub.withDeadlineAfter(8, TimeUnit.SECONDS).jogarPeca(request, observar(sucesso, erro));
    }

    public void comprarPeca(String codigo, String token, Consumer<AcaoJogoResponse> sucesso, Consumer<Throwable> erro) {
        AcaoSimplesRequest request = AcaoSimplesRequest.newBuilder().setCodigoSala(codigo).setTokenJogador(token).build();
        stub.withDeadlineAfter(8, TimeUnit.SECONDS).comprarPeca(request, observar(sucesso, erro));
    }

    public void passarVez(String codigo, String token, Consumer<AcaoJogoResponse> sucesso, Consumer<Throwable> erro) {
        AcaoSimplesRequest request = AcaoSimplesRequest.newBuilder().setCodigoSala(codigo).setTokenJogador(token).build();
        stub.withDeadlineAfter(8, TimeUnit.SECONDS).passarVez(request, observar(sucesso, erro));
    }

    public void consultarEstadoJogo(String codigo, String token, Consumer<EstadoJogoResponse> sucesso, Consumer<Throwable> erro) {
        EstadoJogoRequest request = EstadoJogoRequest.newBuilder().setCodigoSala(codigo).setTokenJogador(token).build();
        stub.withDeadlineAfter(3, TimeUnit.SECONDS).consultarEstadoJogo(request, observar(sucesso, erro));
    }

    public void acompanharJogo(String codigo, String token, Consumer<EstadoJogoResponse> receber, Consumer<Throwable> erro) {
        if (disposed) {
            return;
        }

        pararAcompanhamento();
        Context.CancellableContext contexto = Context.ROOT.withCancellation();

        acompanhamento = contexto;
        EstadoJogoRequest request = EstadoJogoRequest.newBuilder().setCodigoSala(codigo).setTokenJogador(token).build();

        StreamObserver<EstadoJogoResponse> observador =
            new StreamObserver<EstadoJogoResponse>() {
                @Override
                public void onNext(EstadoJogoResponse estado) {
                    Gdx.app.postRunnable(() -> {
                        if (!disposed && acompanhamento == contexto) {
                            receber.accept(estado);
                        }
                    });
                }

                @Override
                public void onError(Throwable falha) {
                    finalizar(falha);
                }

                @Override
                public void onCompleted() {
                    finalizar(Status.UNAVAILABLE.withDescription("Acompanhamento encerrado.").asRuntimeException());
                }

                private void finalizar(Throwable falha) {
                    Gdx.app.postRunnable(() -> {
                        if (disposed || acompanhamento != contexto) {
                            return;
                        }

                        acompanhamento = null;
                        contexto.cancel(null);
                        erro.accept(falha);
                    });
                }
            };

        try {
            contexto.run(() -> stub.acompanharJogo(request, observador));
        } catch (RuntimeException falha) {
            observador.onError(falha);
        }
    }

    public void pararAcompanhamento() {
        Context.CancellableContext anterior = acompanhamento;
        acompanhamento = null;

        if (anterior != null) {
            anterior.cancel(null);
        }
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
        pararAcompanhamento();
        channel.shutdownNow();
    }
}
