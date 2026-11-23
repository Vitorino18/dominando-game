
package br.com.dominando.network;

import br.com.dominando.grpc.*;
import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DesceUmaConexaoTest {

    private static final int PORTA = 50051;
    private static final long TIMEOUT_SEGUNDOS = 10;

    public static void main(String[] args) throws Exception {

        ManagedChannel canal = ManagedChannelBuilder
                .forAddress("localhost", PORTA)
                .usePlaintext()
                .build();

        var cliente = DesceUmaServiceGrpc
                .newBlockingStub(canal)
                .withDeadlineAfter(10, TimeUnit.SECONDS);

        var streaming = DesceUmaServiceGrpc.newStub(canal);

        List<ObservadorEstado> observadores = new ArrayList<>();

        try {
            System.out.println("=== TESTE DE DESCONEXÃO ===");

            // 1. Criar sala
            CriarSalaResponse sala = cliente.criarSala(
                    CriarSalaRequest.newBuilder()
                            .setNomeCriador("Vitor")
                            .build()
            );

            String codigo = sala.getCodigoSala();
            String tokenVitor = sala.getTokenJogador();

            System.out.println("Sala: " + codigo);

            // 2. Yago entra na sala
            EntrarSalaResponse entrada = cliente.entrarSala(
                    EntrarSalaRequest.newBuilder()
                            .setCodigoSala(codigo)
                            .setNomeJogador("Yago")
                            .build()
            );

            String tokenYago = entrada.getTokenJogador();

            // 3. Iniciar partida
            cliente.iniciarPartida(
                    IniciarPartidaRequest.newBuilder()
                            .setCodigoSala(codigo)
                            .setTokenCriador(tokenVitor)
                            .build()
            );

            EstadoJogoRequest requestVitor =
                    EstadoJogoRequest.newBuilder()
                            .setCodigoSala(codigo)
                            .setTokenJogador(tokenVitor)
                            .build();

            EstadoJogoRequest requestYago =
                    EstadoJogoRequest.newBuilder()
                            .setCodigoSala(codigo)
                            .setTokenJogador(tokenYago)
                            .build();

            // 4. Conectar Yago
            ObservadorEstado observadorYago =
                    new ObservadorEstado("Yago");

            observadores.add(observadorYago);

            observadorYago.iniciarStreaming(
                    streaming, requestYago
            );

            observadorYago.aguardarConexao("Yago", true);

            // 5. Conectar Vitor
            ObservadorEstado observadorVitor =
                    new ObservadorEstado("Vitor");

            observadores.add(observadorVitor);

            observadorVitor.iniciarStreaming(
                    streaming, requestVitor
            );

            observadorVitor.aguardarConexao("Vitor", true);
            observadorYago.aguardarConexao("Vitor", true);

            System.out.println(
                    "\n[OK] Ambos estão conectados."
            );

            // 6. Desconectar Vitor
            System.out.println(
                    "\n=== VITOR DESCONECTANDO ==="
            );

            // Marca o ponto a partir do qual esperamos
            // uma nova atualização.
            int versao = observadorYago.quantidadeEstados();

            observadorVitor.cancelar();
            observadorVitor.aguardarEncerramento();

            observadorYago.aguardarConexaoDepoisDe(
                    "Vitor", false, versao
            );

            System.out.println(
                    "[OK] Yago recebeu Vitor desconectado."
            );

            // 7. Reconectar Vitor
            System.out.println(
                    "\n=== VITOR RECONECTANDO ==="
            );

            versao = observadorYago.quantidadeEstados();

            ObservadorEstado vitorReconectado =
                    new ObservadorEstado("Vitor reconectado");

            observadores.add(vitorReconectado);

            vitorReconectado.iniciarStreaming(
                    streaming, requestVitor
            );

            vitorReconectado.aguardarConexao(
                    "Vitor", true
            );

            observadorYago.aguardarConexaoDepoisDe(
                    "Vitor", true, versao
            );

            System.out.println(
                    "[OK] Yago recebeu Vitor reconectado."
            );

            // 8. Abrir segunda conexão de Vitor
            System.out.println(
                    "\n=== TESTE DE DUAS CONEXÕES ==="
            );

            ObservadorEstado segundaConexao =
                    new ObservadorEstado("Vitor conexão 2");

            observadores.add(segundaConexao);

            segundaConexao.iniciarStreaming(
                    streaming, requestVitor
            );

            segundaConexao.aguardarConexao(
                    "Vitor", true
            );

            // Fecha apenas a segunda conexão
            segundaConexao.cancelar();
            segundaConexao.aguardarEncerramento();

            // A primeira conexão de Vitor continua ativa.
            // Aguarda o servidor confirmar que o estado
            // continua indicando Vitor conectado.
            aguardarEstadoNoServidor(
                    cliente, requestYago, "Vitor", true
            );

            System.out.println(
                    "[OK] Vitor continua conectado."
            );

            // 9. Fechar a última conexão de Vitor
            System.out.println(
                    "\n=== ÚLTIMA CONEXÃO DE VITOR ==="
            );

            versao = observadorYago.quantidadeEstados();

            vitorReconectado.cancelar();
            vitorReconectado.aguardarEncerramento();

            observadorYago.aguardarConexaoDepoisDe(
                    "Vitor", false, versao
            );

            System.out.println(
                    "[OK] Vitor agora está desconectado."
            );

            // 10. Verificar se Yago continua conectado
            aguardarEstadoNoServidor(
                    cliente, requestYago, "Yago", true
            );

            System.out.println(
                    "[OK] Yago permaneceu conectado."
            );

            System.out.println(
                    "\n=================================="
            );
            System.out.println(
                    " TODOS OS TESTES PASSARAM!"
            );
            System.out.println(
                    "=================================="
            );

        } finally {

            // Encerra todos os streams criados pelo teste.
            for (ObservadorEstado observador : observadores) {
                observador.cancelar();
            }

            canal.shutdownNow();
            canal.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private static void aguardarEstadoNoServidor(
            DesceUmaServiceGrpc.DesceUmaServiceBlockingStub cliente,
            EstadoJogoRequest request,
            String nomeJogador,
            boolean conectadoEsperado) throws Exception {

        long limite = System.nanoTime()
                + TimeUnit.SECONDS.toNanos(TIMEOUT_SEGUNDOS);

        while (System.nanoTime() < limite) {

            EstadoJogoResponse estado =
                    cliente.consultarEstadoJogo(request);

            for (JogadorMensagem jogador :
                    estado.getJogadoresList()) {

                if (jogador.getNome().equals(nomeJogador)
                        && jogador.getConectado()
                        == conectadoEsperado) {
                    return;
                }
            }

            Thread.sleep(50);
        }

        throw new AssertionError(
                "Timeout consultando servidor: "
                        + nomeJogador
                        + " conectado="
                        + conectadoEsperado
        );
    }

    private static class ObservadorEstado
            implements StreamObserver<EstadoJogoResponse> {

        private final String nome;
        private final Object monitor = new Object();

        private final List<EstadoJogoResponse> estados =
                new ArrayList<>();

        private Context.CancellableContext contexto;

        private boolean encerrado = false;
        private Throwable erro;

        ObservadorEstado(String nome) {
            this.nome = nome;
        }

        // Inicia cada streaming em um contexto próprio.
        void iniciarStreaming(
                DesceUmaServiceGrpc.DesceUmaServiceStub stub,
                EstadoJogoRequest request) {

            contexto = Context.current()
                    .withCancellation();

            contexto.run(() ->
                    stub.acompanharJogo(request, this)
            );
        }

        @Override
        public void onNext(EstadoJogoResponse estado) {

            synchronized (monitor) {

                estados.add(estado);

                System.out.println(
                        "\n[" + nome + "] Atualização recebida:"
                );

                for (JogadorMensagem jogador :
                        estado.getJogadoresList()) {

                    System.out.println(
                            "  " + jogador.getNome()
                                    + " - conectado: "
                                    + jogador.getConectado()
                    );
                }

                monitor.notifyAll();
            }
        }

        @Override
        public void onError(Throwable throwable) {

            synchronized (monitor) {

                erro = throwable;
                encerrado = true;

                Status status =
                        Status.fromThrowable(throwable);

                System.out.println(
                        "[" + nome + "] Stream encerrado: "
                                + status.getCode()
                                + " | "
                                + status.getDescription()
                );

                monitor.notifyAll();
            }
        }

        @Override
        public void onCompleted() {

            synchronized (monitor) {

                encerrado = true;

                System.out.println(
                        "[" + nome + "] Stream concluído."
                );

                monitor.notifyAll();
            }
        }

        int quantidadeEstados() {
            synchronized (monitor) {
                return estados.size();
            }
        }

        void aguardarConexao(
                String jogador,
                boolean esperado) throws Exception {

            aguardarConexaoDepoisDe(jogador, esperado, 0);
        }

        // Verifica apenas atualizações recebidas depois
        // do ponto informado, evitando estados antigos.
        void aguardarConexaoDepoisDe(
                String jogador,
                boolean esperado,
                int indiceInicial) throws Exception {

            long limite = System.nanoTime()
                    + TimeUnit.SECONDS.toNanos(
                    TIMEOUT_SEGUNDOS
            );

            synchronized (monitor) {

                while (true) {

                    for (int i = indiceInicial;
                         i < estados.size(); i++) {

                        EstadoJogoResponse estado =
                                estados.get(i);

                        for (JogadorMensagem j :
                                estado.getJogadoresList()) {

                            if (j.getNome().equals(jogador)
                                    && j.getConectado()
                                    == esperado) {
                                return;
                            }
                        }
                    }

                    if (erro != null) {
                        throw new AssertionError(
                                "Erro no streaming de " + nome,
                                erro
                        );
                    }

                    if (encerrado) {
                        throw new AssertionError(
                                "Streaming de " + nome
                                        + " foi encerrado."
                        );
                    }

                    long restante =
                            limite - System.nanoTime();

                    if (restante <= 0) {
                        throw new AssertionError(
                                "Timeout aguardando "
                                        + jogador
                                        + " conectado="
                                        + esperado
                                        + " no streaming de "
                                        + nome
                        );
                    }

                    TimeUnit.NANOSECONDS.timedWait(
                            monitor, restante
                    );
                }
            }
        }

        void aguardarEncerramento() throws Exception {

            long limite = System.nanoTime()
                    + TimeUnit.SECONDS.toNanos(
                    TIMEOUT_SEGUNDOS
            );

            synchronized (monitor) {

                while (!encerrado) {

                    long restante =
                            limite - System.nanoTime();

                    if (restante <= 0) {
                        throw new AssertionError(
                                "Timeout aguardando encerramento "
                                        + "do streaming de " + nome
                        );
                    }

                    TimeUnit.NANOSECONDS.timedWait(
                            monitor, restante
                    );
                }
            }
        }

        void cancelar() {

            if (contexto != null
                    && !contexto.isCancelled()) {

                System.out.println(
                        "[TESTE] Cancelando streaming de "
                                + nome
                );

                contexto.cancel(null);
            }
        }
    }
}
