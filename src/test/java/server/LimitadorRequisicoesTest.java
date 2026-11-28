package server;

import br.com.dominando.server.LimitadorRequisicoes;
import br.com.dominando.server.ConfiguracaoServidor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class LimitadorRequisicoesTest {

    private LimitadorRequisicoes limitador;

    private final int limite =
            ConfiguracaoServidor.MAX_REQUISICOES_POR_SEGUNDO;

    @BeforeEach
    void configurar() {
        limitador = new LimitadorRequisicoes(limite);
    }

    @Test
    void devePermitirRequisicoesDentroDoLimite() {

        for (int i = 0; i < limite; i++) {
            assertTrue(limitador.permitirRequisicao());
        }
    }

    @Test
    void deveBloquearRequisicaoAcimaDoLimite() {

        for (int i = 0; i < limite; i++) {
            assertTrue(limitador.permitirRequisicao());
        }

        assertFalse(limitador.permitirRequisicao());
    }

    @Test
    void deveBloquearCentenasDeRequisicoes() {

        for (int i = 0; i < limite; i++) {
            assertTrue(limitador.permitirRequisicao());
        }

        for (int i = 0; i < 500; i++) {
            assertFalse(limitador.permitirRequisicao());
        }
    }

    @Test
    void deveLiberarRequisicoesAposUmSegundo()
            throws InterruptedException {

        for (int i = 0; i < limite; i++) {
            assertTrue(limitador.permitirRequisicao());
        }

        assertFalse(limitador.permitirRequisicao());

        Thread.sleep(1100);

        assertTrue(limitador.permitirRequisicao());
    }

    @Test
    void deveManterLimitadoresIndependentes() {

        LimitadorRequisicoes outroJogador =
                new LimitadorRequisicoes(limite);

        for (int i = 0; i < limite; i++) {
            assertTrue(limitador.permitirRequisicao());
        }

        assertFalse(limitador.permitirRequisicao());

        assertTrue(outroJogador.permitirRequisicao());
    }

    @Test
    void deveRespeitarLimiteComThreadsSimultaneas()
            throws Exception {

        ExecutorService executor =
                Executors.newFixedThreadPool(10);

        CountDownLatch inicio = new CountDownLatch(1);

        List<Future<Boolean>> resultados =
                new ArrayList<>();

        try {

            for (int i = 0; i < 100; i++) {

                resultados.add(executor.submit(() -> {

                    inicio.await();

                    return limitador.permitirRequisicao();
                }));
            }

            inicio.countDown();

            int permitidas = 0;
            int bloqueadas = 0;

            for (Future<Boolean> resultado : resultados) {

                if (resultado.get(5, TimeUnit.SECONDS)) {
                    permitidas++;
                } else {
                    bloqueadas++;
                }
            }

            assertEquals(limite, permitidas);
            assertEquals(100 - limite, bloqueadas);

        } finally {

            executor.shutdownNow();
        }
    }

    @Test
    void deveRecusarLimiteInvalido() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new LimitadorRequisicoes(0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new LimitadorRequisicoes(-1)
        );
    }
}
