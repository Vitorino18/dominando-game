package server;

import br.com.dominando.grpc.*;
import br.com.dominando.server.*;

import io.grpc.*;
import io.grpc.stub.StreamObserver;

import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

class DesceUmaSegurancaTest {

    private SalaService salaService;
    private Server servidor;

    private ManagedChannel canal1;
    private ManagedChannel canal2;

    private DesceUmaServiceGrpc.DesceUmaServiceBlockingStub cliente1;
    private DesceUmaServiceGrpc.DesceUmaServiceBlockingStub cliente2;

    private DesceUmaServiceGrpc.DesceUmaServiceStub async1;
    private DesceUmaServiceGrpc.DesceUmaServiceStub async2;

    private String codigoSala;
    private String token1;
    private String token2;

    @BeforeEach
    void configurar() throws Exception {

        salaService = new SalaService();

        servidor = ServerBuilder
                .forPort(0)
                .addService(
                        new DesceUmaGrpcService(salaService)
                )
                .build()
                .start();

        canal1 = ManagedChannelBuilder
                .forAddress("localhost", servidor.getPort())
                .usePlaintext()
                .build();

        canal2 = ManagedChannelBuilder
                .forAddress("localhost", servidor.getPort())
                .usePlaintext()
                .build();

        cliente1 = DesceUmaServiceGrpc.newBlockingStub(canal1);
        cliente2 = DesceUmaServiceGrpc.newBlockingStub(canal2);

        async1 = DesceUmaServiceGrpc.newStub(canal1);
        async2 = DesceUmaServiceGrpc.newStub(canal2);
    }

    @AfterEach
    void finalizar() throws Exception {

        salaService.encerrar();

        canal1.shutdownNow();
        canal2.shutdownNow();

        servidor.shutdownNow();

        canal1.awaitTermination(5, TimeUnit.SECONDS);
        canal2.awaitTermination(5, TimeUnit.SECONDS);

        servidor.awaitTermination(5, TimeUnit.SECONDS);
    }

    // ==========================================
    // TESTE 1 - CRIAÇÃO CONCORRENTE
    // ==========================================

    @Test
    void naoDeveUltrapassarLimiteDeSalas()
            throws Exception {

        int limite = ConfiguracaoServidor.MAX_SALAS;
        int tentativas = limite + 20;

        ExecutorService executor =
                Executors.newFixedThreadPool(20);

        CountDownLatch inicio = new CountDownLatch(1);

        List<Future<Boolean>> resultados =
                new ArrayList<>();

        try {

            for (int i = 0; i < tentativas; i++) {

                final int numero = i;

                resultados.add(executor.submit(() -> {

                    inicio.await();

                    try {

                        cliente1
                                .withDeadlineAfter(
                                        10, TimeUnit.SECONDS
                                )
                                .criarSala(
                                        CriarSalaRequest.newBuilder()
                                                .setNomeCriador(
                                                        "Jogador" + numero
                                                )
                                                .build()
                                );

                        return true;

                    } catch (StatusRuntimeException e) {

                        assertEquals(
                                Status.Code.FAILED_PRECONDITION,
                                e.getStatus().getCode()
                        );

                        return false;
                    }
                }));
            }

            inicio.countDown();

            int criadas = 0;
            int recusadas = 0;

            for (Future<Boolean> resultado : resultados) {

                if (resultado.get(15, TimeUnit.SECONDS)) {
                    criadas++;
                } else {
                    recusadas++;
                }
            }

            assertEquals(limite, criadas);
            assertEquals(20, recusadas);

            assertEquals(
                    limite,
                    salaService.quantidadeSalas()
            );

        } finally {

            executor.shutdownNow();
        }
    }

    // ==========================================
    // TESTE 2 - LIMPEZA AUTOMÁTICA
    // ==========================================

    @Test
    void deveExecutarLimpezaAutomaticamente()
            throws Exception {

        CriarSalaResponse resposta = cliente1.criarSala(
                CriarSalaRequest.newBuilder()
                        .setNomeCriador("Vitor")
                        .build()
        );

        Sala sala = salaService.buscarSala(
                resposta.getCodigoSala()
        );

        alterarTempo(
                sala,
                "ultimaAtividade",
                ConfiguracaoServidor
                        .TEMPO_SALA_AGUARDANDO_MINUTOS + 1
        );

        aguardarCondicao(
                () -> salaService.buscarSala(
                        resposta.getCodigoSala()
                ) == null,

                ConfiguracaoServidor
                        .INTERVALO_LIMPEZA_SEGUNDOS + 10
        );

        assertEquals(0, salaService.quantidadeSalas());
    }

    // ==========================================
    // TESTE 3 - TOKEN INVÁLIDO
    // ==========================================

    @Test
    void deveBloquearTokenInvalido() {

        prepararPartida();

        StatusRuntimeException erro = assertThrows(
                StatusRuntimeException.class,

                () -> cliente1.consultarEstadoJogo(
                        criarRequest("token-invalido")
                )
        );

        assertEquals(
                Status.Code.PERMISSION_DENIED,
                erro.getStatus().getCode()
        );
    }

    // ==========================================
    // TESTE 4 - TOKEN DE OUTRA SALA
    // ==========================================

    @Test
    void deveBloquearTokenDeOutraSala() {

        prepararPartida();

        CriarSalaResponse outraSala = cliente2.criarSala(
                CriarSalaRequest.newBuilder()
                        .setNomeCriador("OutroJogador")
                        .build()
        );

        StatusRuntimeException erro = assertThrows(
                StatusRuntimeException.class,

                () -> cliente2.consultarEstadoJogo(
                        criarRequest(
                                outraSala.getTokenJogador()
                        )
                )
        );

        assertEquals(
                Status.Code.PERMISSION_DENIED,
                erro.getStatus().getCode()
        );
    }

    // ==========================================
    // TESTE 5 - AMBOS DESCONECTADOS
    // ==========================================

    @Test
    void deveRemoverPartidaComAmbosDesconectados()
            throws Exception {

        prepararPartida();

        Sala sala = salaService.buscarSala(codigoSala);

        ObservadorEstado observador1 =
                new ObservadorEstado();

        ObservadorEstado observador2 =
                new ObservadorEstado();

        observador1.iniciar(async1, criarRequest(token1));
        observador2.iniciar(async2, criarRequest(token2));

        aguardarCondicao(
                () -> jogadorConectado("Vitor")
                        && jogadorConectado("Yago"),
                5
        );

        observador1.cancelar();
        observador2.cancelar();

        aguardarCondicao(
                () -> !jogadorConectado("Vitor")
                        && !jogadorConectado("Yago"),
                5
        );

        alterarTempo(
                sala,
                "desconectadosDesde",
                ConfiguracaoServidor
                        .TEMPO_DESCONECTADOS_MINUTOS + 1
        );

        executarLimpeza();

        assertNull(
                salaService.buscarSala(codigoSala)
        );
    }

    // ==========================================
    // TESTE 6 - RECONEXÃO
    // ==========================================

    @Test
    void devePreservarSalaQuandoJogadorReconectar()
            throws Exception {

        prepararPartida();

        Sala sala = salaService.buscarSala(codigoSala);

        ObservadorEstado observador1 =
                new ObservadorEstado();

        ObservadorEstado observador2 =
                new ObservadorEstado();

        observador1.iniciar(async1, criarRequest(token1));
        observador2.iniciar(async2, criarRequest(token2));

        aguardarCondicao(
                () -> jogadorConectado("Vitor")
                        && jogadorConectado("Yago"),
                5
        );

        observador1.cancelar();
        observador2.cancelar();

        aguardarCondicao(
                () -> !jogadorConectado("Vitor")
                        && !jogadorConectado("Yago"),
                5
        );

        alterarTempo(
                sala,
                "desconectadosDesde",
                ConfiguracaoServidor
                        .TEMPO_DESCONECTADOS_MINUTOS - 1
        );

        ObservadorEstado reconexao =
                new ObservadorEstado();

        reconexao.iniciar(
                async1,
                criarRequest(token1)
        );

        aguardarCondicao(
                () -> jogadorConectado("Vitor"),
                5
        );

        assertNull(
                lerAtributo(sala, "desconectadosDesde")
        );

        executarLimpeza();

        assertSame(
                sala,
                salaService.buscarSala(codigoSala)
        );

        reconexao.cancelar();
    }

    // ==========================================
    // TESTE 7 - PARTIDA ATIVA
    // ==========================================

    @Test
    void naoDeveRemoverPartidaAtiva()
            throws Exception {

        prepararPartida();

        Sala sala = salaService.buscarSala(codigoSala);

        ObservadorEstado observador1 =
                new ObservadorEstado();

        ObservadorEstado observador2 =
                new ObservadorEstado();

        observador1.iniciar(async1, criarRequest(token1));
        observador2.iniciar(async2, criarRequest(token2));

        aguardarCondicao(
                () -> jogadorConectado("Vitor")
                        && jogadorConectado("Yago"),
                5
        );

        alterarTempo(
                sala,
                "ultimaAtividade",
                ConfiguracaoServidor
                        .TEMPO_INATIVIDADE_MINUTOS - 1
        );

        executarLimpeza();

        assertSame(
                sala,
                salaService.buscarSala(codigoSala)
        );

        assertFalse(sala.deveSerRemovida());

        observador1.cancelar();
        observador2.cancelar();
    }

    // ==========================================
    // TESTE 8 - ENCERRAMENTO DO STREAMING
    // ==========================================

    @Test
    void deveEncerrarStreamsAoRemoverSala()
            throws Exception {

        prepararPartida();

        ObservadorEstado observador1 =
                new ObservadorEstado();

        ObservadorEstado observador2 =
                new ObservadorEstado();

        observador1.iniciar(async1, criarRequest(token1));
        observador2.iniciar(async2, criarRequest(token2));

        aguardarCondicao(
                () -> jogadorConectado("Vitor")
                        && jogadorConectado("Yago"),
                5
        );

        assertTrue(
                salaService.removerSala(codigoSala)
        );

        assertTrue(
                observador1.aguardarEncerramento()
        );

        assertTrue(
                observador2.aguardarEncerramento()
        );

        assertNotNull(observador1.erro);
        assertNotNull(observador2.erro);

        assertEquals(
                Status.Code.NOT_FOUND,
                Status.fromThrowable(
                        observador1.erro
                ).getCode()
        );

        assertEquals(
                Status.Code.NOT_FOUND,
                Status.fromThrowable(
                        observador2.erro
                ).getCode()
        );

        assertNull(
                salaService.buscarSala(codigoSala)
        );
    }

    // ==========================================
    // TESTE 9 - EXCESSO DE REQUISIÇÕES
    // ==========================================

    @Test
    void deveBloquearExcessoDeRequisicoes() {

        prepararPartida();

        StatusRuntimeException erro =
                excederLimiteRequisicoes();

        assertEquals(
                Status.Code.RESOURCE_EXHAUSTED,
                erro.getStatus().getCode()
        );
    }

    // ==========================================
    // TESTE 10 - RECUPERAÇÃO DA COTA
    // ==========================================

    @Test
    void deveLiberarRequisicoesAposEspera()
            throws InterruptedException {

        prepararPartida();

        StatusRuntimeException erro =
                excederLimiteRequisicoes();

        assertEquals(
                Status.Code.RESOURCE_EXHAUSTED,
                erro.getStatus().getCode()
        );

        Thread.sleep(1100);

        assertDoesNotThrow(() ->
                cliente1
                        .withDeadlineAfter(2, TimeUnit.SECONDS)
                        .consultarEstadoJogo(
                                criarRequest(token1)
                        )
        );
    }

    // ==========================================
    // TESTE 11 - LIMITADORES INDEPENDENTES
    // ==========================================

    @Test
    void naoDeveBloquearOutroJogador() {

        prepararPartida();

        StatusRuntimeException erro =
                excederLimiteRequisicoes();

        assertEquals(
                Status.Code.RESOURCE_EXHAUSTED,
                erro.getStatus().getCode()
        );

        assertDoesNotThrow(() ->
                cliente2
                        .withDeadlineAfter(2, TimeUnit.SECONDS)
                        .consultarEstadoJogo(
                                criarRequest(token2)
                        )
        );
    }

    // ==========================================
    // TESTE 12 - DOIS STREAMS SIMULTÂNEOS
    // ==========================================

    @Test
    void devePermitirDoisStreamsPorJogador()
            throws Exception {

        prepararPartida();

        ObservadorEstado primeiro =
                new ObservadorEstado();

        ObservadorEstado segundo =
                new ObservadorEstado();

        try {

            primeiro.iniciar(async1, criarRequest(token1));
            segundo.iniciar(async1, criarRequest(token1));

            assertTrue(primeiro.aguardarPrimeiroEstado());
            assertTrue(segundo.aguardarPrimeiroEstado());

            assertEquals(2, quantidadeStreams(token1));

            assertNull(primeiro.erro);
            assertNull(segundo.erro);

        } finally {

            primeiro.cancelar();
            segundo.cancelar();
        }
    }

    // ==========================================
    // TESTE 13 - BLOQUEIO DO TERCEIRO STREAM
    // ==========================================

    @Test
    void deveBloquearTerceiroStream()
            throws Exception {

        prepararPartida();

        ObservadorEstado primeiro =
                new ObservadorEstado();

        ObservadorEstado segundo =
                new ObservadorEstado();

        ObservadorEstado terceiro =
                new ObservadorEstado();

        try {

            primeiro.iniciar(async1, criarRequest(token1));
            segundo.iniciar(async1, criarRequest(token1));

            assertTrue(primeiro.aguardarPrimeiroEstado());
            assertTrue(segundo.aguardarPrimeiroEstado());

            terceiro.iniciar(async1, criarRequest(token1));

            assertTrue(terceiro.aguardarEncerramento());

            assertNotNull(terceiro.erro);

            assertEquals(
                    Status.Code.RESOURCE_EXHAUSTED,
                    Status.fromThrowable(
                            terceiro.erro
                    ).getCode()
            );

            assertEquals(2, quantidadeStreams(token1));

            assertNull(primeiro.erro);
            assertNull(segundo.erro);

        } finally {

            primeiro.cancelar();
            segundo.cancelar();
            terceiro.cancelar();
        }
    }

    // ==========================================
    // TESTE 14 - LIBERAÇÃO DE VAGA NO STREAMING
    // ==========================================

    @Test
    void deveLiberarVagaAposCancelarStream()
            throws Exception {

        prepararPartida();

        ObservadorEstado primeiro =
                new ObservadorEstado();

        ObservadorEstado segundo =
                new ObservadorEstado();

        ObservadorEstado reconexao =
                new ObservadorEstado();

        try {

            primeiro.iniciar(async1, criarRequest(token1));
            segundo.iniciar(async1, criarRequest(token1));

            assertTrue(primeiro.aguardarPrimeiroEstado());
            assertTrue(segundo.aguardarPrimeiroEstado());

            assertEquals(2, quantidadeStreams(token1));

            primeiro.cancelar();

            aguardarCondicao(
                    () -> quantidadeStreams(token1) == 1,
                    5
            );

            reconexao.iniciar(
                    async1,
                    criarRequest(token1)
            );

            assertTrue(reconexao.aguardarPrimeiroEstado());

            assertEquals(2, quantidadeStreams(token1));

            assertNull(reconexao.erro);

        } finally {

            primeiro.cancelar();
            segundo.cancelar();
            reconexao.cancelar();
        }
    }

    // ==========================================
    // MÉTODOS AUXILIARES
    // ==========================================

    private void prepararPartida() {

        CriarSalaResponse criacao = cliente1.criarSala(
                CriarSalaRequest.newBuilder()
                        .setNomeCriador("Vitor")
                        .build()
        );

        codigoSala = criacao.getCodigoSala();
        token1 = criacao.getTokenJogador();

        EntrarSalaResponse entrada = cliente2.entrarSala(
                EntrarSalaRequest.newBuilder()
                        .setCodigoSala(codigoSala)
                        .setNomeJogador("Yago")
                        .build()
        );

        token2 = entrada.getTokenJogador();

        IniciarPartidaResponse resposta =
                cliente1.iniciarPartida(
                        IniciarPartidaRequest.newBuilder()
                                .setCodigoSala(codigoSala)
                                .setTokenCriador(token1)
                                .build()
                );

        assertTrue(resposta.getIniciada());
    }

    private EstadoJogoRequest criarRequest(String token) {

        return EstadoJogoRequest.newBuilder()
                .setCodigoSala(codigoSala)
                .setTokenJogador(token)
                .build();
    }

    private boolean jogadorConectado(String nome) {

        try {

            EstadoJogoResponse estado = cliente1
                    .withDeadlineAfter(2, TimeUnit.SECONDS)
                    .consultarEstadoJogo(
                            criarRequest(token1)
                    );

            return estado.getJogadoresList()
                    .stream()
                    .filter(jogador ->
                            jogador.getNome().equals(nome)
                    )
                    .anyMatch(JogadorMensagem::getConectado);

        } catch (StatusRuntimeException e) {

            if (e.getStatus().getCode()
                    == Status.Code.RESOURCE_EXHAUSTED) {

                // Aguarda a recuperação da cota antes
                // de consultar novamente a presença.

                try {

                    Thread.sleep(1100);

                } catch (InterruptedException ex) {

                    Thread.currentThread().interrupt();

                    throw new AssertionError(
                            "Consulta de presença interrompida.",
                            ex
                    );
                }

                EstadoJogoResponse estado = cliente1
                        .withDeadlineAfter(2, TimeUnit.SECONDS)
                        .consultarEstadoJogo(
                                criarRequest(token1)
                        );

                return estado.getJogadoresList()
                        .stream()
                        .filter(jogador ->
                                jogador.getNome().equals(nome)
                        )
                        .anyMatch(JogadorMensagem::getConectado);
            }

            throw e;
        }
    }

    private void alterarTempo(
            Sala sala,
            String atributo,
            long minutos
    ) throws Exception {

        Field campo = Sala.class
                .getDeclaredField(atributo);

        campo.setAccessible(true);

        long instante = System.nanoTime()
                - TimeUnit.MINUTES.toNanos(minutos);

        synchronized (sala) {
            campo.set(sala, instante);
        }
    }

    private Object lerAtributo(
            Sala sala,
            String atributo
    ) throws Exception {

        Field campo = Sala.class
                .getDeclaredField(atributo);

        campo.setAccessible(true);

        synchronized (sala) {
            return campo.get(sala);
        }
    }

    private void executarLimpeza() throws Exception {

        Method metodo = SalaService.class
                .getDeclaredMethod(
                        "limparSalasExpiradas"
                );

        metodo.setAccessible(true);

        metodo.invoke(salaService);
    }

    private void aguardarCondicao(
            BooleanSupplier condicao,
            long segundos
    ) throws InterruptedException {

        long inicio = System.nanoTime();

        long limite = TimeUnit.SECONDS
                .toNanos(segundos);

        while (System.nanoTime() - inicio < limite) {

            if (condicao.getAsBoolean()) {
                return;
            }

            Thread.sleep(50);
        }

        assertTrue(
                condicao.getAsBoolean(),
                "A condição não foi atendida dentro do prazo."
        );
    }

    // ==========================================
    // AUXILIAR - EXCEDER LIMITE DE REQUISIÇÕES
    // ==========================================

    private StatusRuntimeException excederLimiteRequisicoes() {

        for (int i = 0; i < 500; i++) {

            try {

                cliente1
                        .withDeadlineAfter(2, TimeUnit.SECONDS)
                        .consultarEstadoJogo(
                                criarRequest(token1)
                        );

            } catch (StatusRuntimeException e) {

                assertEquals(
                        Status.Code.RESOURCE_EXHAUSTED,
                        e.getStatus().getCode()
                );

                return e;
            }
        }

        throw new AssertionError(
                "O servidor não bloqueou o excesso de requisições."
        );
    }

    // ==========================================
    // AUXILIAR - QUANTIDADE DE STREAMS ATIVOS
    // ==========================================

    private long quantidadeStreams(String token) {

        Sala sala = salaService.buscarSala(codigoSala);

        assertNotNull(sala);

        try {

            Method metodo = Sala.class.getDeclaredMethod(
                    "quantidadeStreamsAtivos",
                    String.class
            );

            metodo.setAccessible(true);

            synchronized (sala) {

                return (Long) metodo.invoke(sala, token);
            }

        } catch (ReflectiveOperationException e) {

            throw new AssertionError(
                    "Não foi possível consultar os streams.",
                    e
            );
        }
    }

    // ==========================================
    // OBSERVADOR DO STREAMING
    // ==========================================

    private static class ObservadorEstado
            implements StreamObserver<EstadoJogoResponse> {

        private Context.CancellableContext contexto;

        private final CountDownLatch encerramento =
                new CountDownLatch(1);

        private final CountDownLatch primeiroEstado =
                new CountDownLatch(1);

        private volatile Throwable erro;

        void iniciar(
                DesceUmaServiceGrpc.DesceUmaServiceStub stub,
                EstadoJogoRequest request
        ) {

            contexto = Context.current()
                    .withCancellation();

            contexto.run(
                    () -> stub.acompanharJogo(request, this)
            );
        }

        void cancelar() {

            if (contexto != null) {
                contexto.cancel(null);
            }
        }

        boolean aguardarEncerramento()
                throws InterruptedException {

            return encerramento.await(
                    5, TimeUnit.SECONDS
            );
        }

        boolean aguardarPrimeiroEstado()
                throws InterruptedException {

            return primeiroEstado.await(
                    5, TimeUnit.SECONDS
            );
        }

        @Override
        public void onNext(EstadoJogoResponse estado) {

            primeiroEstado.countDown();
        }

        @Override
        public void onError(Throwable throwable) {

            erro = throwable;

            encerramento.countDown();
        }

        @Override
        public void onCompleted() {

            encerramento.countDown();
        }
    }
}