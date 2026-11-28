package server;

import br.com.dominando.server.ConfiguracaoServidor;
import br.com.dominando.server.Sala;
import br.com.dominando.server.SalaService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SalaServerTempoTest {
    private SalaService salaService;

    @BeforeEach
    void configurar() {
        salaService = new SalaService();
    }

    @AfterEach
    void finalizar() {
        salaService.encerrar();
    }

    @Test
    void deveCriarSalaNormalmente() {

        Sala sala = salaService.criarSala("Jogador1");

        assertNotNull(sala);
        assertNotNull(sala.getCodigo());
        assertEquals(6, sala.getCodigo().length());
        assertEquals(1, salaService.quantidadeSalas());
    }

    @Test
    void deveRespeitarLimiteDeSalas() {

        for (int i = 0;
             i < ConfiguracaoServidor.MAX_SALAS;
             i++) {

            salaService.criarSala("Jogador" + i);
        }

        assertEquals(ConfiguracaoServidor.MAX_SALAS, salaService.quantidadeSalas());

        assertThrows(IllegalStateException.class, () -> salaService.criarSala("JogadorExtra"));

        assertEquals(ConfiguracaoServidor.MAX_SALAS, salaService.quantidadeSalas());
    }

    @Test
    void deveRecusarNomeVazio() {

        assertThrows(IllegalArgumentException.class, () -> salaService.criarSala(""));

        assertEquals(0, salaService.quantidadeSalas());
    }

    @Test
    void deveRecusarNomeMuitoGrande() {

        String nome = "A".repeat(
                ConfiguracaoServidor.MAX_TAMANHO_NOME + 1
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> salaService.criarSala(nome)
        );

        assertEquals(0, salaService.quantidadeSalas());
    }

    @Test
    void deveRecusarNomeDuplicado() {

        Sala sala = salaService.criarSala("Jogador1");

        assertThrows(
                IllegalArgumentException.class,
                () -> salaService.entrarNaSala(
                        sala.getCodigo(),
                        "jogador1"
                )
        );
    }

    @Test
    void deveRemoverSalaExpirada() throws Exception {

        Sala sala = salaService.criarSala("Jogador1");

        simularInatividade(
                sala,
                ConfiguracaoServidor
                        .TEMPO_SALA_AGUARDANDO_MINUTOS + 1
        );

        assertTrue(sala.deveSerRemovida());

        executarLimpeza();

        assertNull(
                salaService.buscarSala(sala.getCodigo())
        );

        assertEquals(0, salaService.quantidadeSalas());
    }

    @Test
    void devePreservarSalaNaoExpirada() throws Exception {

        Sala sala = salaService.criarSala("Jogador1");

        simularInatividade(
                sala,
                ConfiguracaoServidor
                        .TEMPO_SALA_AGUARDANDO_MINUTOS / 2
        );

        assertFalse(sala.deveSerRemovida());

        executarLimpeza();

        assertSame(
                sala,
                salaService.buscarSala(sala.getCodigo())
        );

        assertEquals(1, salaService.quantidadeSalas());
    }

    @Test
    void deveLiberarVagaAoRemoverSala() {

        for (int i = 0;
             i < ConfiguracaoServidor.MAX_SALAS;
             i++) {

            salaService.criarSala("Jogador" + i);
        }

        // Uma nova sala não pode ser criada.
        assertThrows(
                IllegalStateException.class,
                () -> salaService.criarSala("Extra")
        );

        // Reinicia o serviço para preparar o cenário.
        salaService.encerrar();
        salaService = new SalaService();

        Sala sala = salaService.criarSala("Jogador1");

        for (int i = 1;
             i < ConfiguracaoServidor.MAX_SALAS;
             i++) {

            salaService.criarSala("Jogador" + i);
        }

        assertTrue(
                salaService.removerSala(sala.getCodigo())
        );

        assertEquals(
                ConfiguracaoServidor.MAX_SALAS - 1,
                salaService.quantidadeSalas()
        );

        Sala novaSala = salaService.criarSala("NovoJogador");

        assertNotNull(novaSala);

        assertEquals(
                ConfiguracaoServidor.MAX_SALAS,
                salaService.quantidadeSalas()
        );
    }

    @Test
    void deveBloquearOperacoesEmSalaRemovida() {

        Sala sala = salaService.criarSala("Jogador1");

        salaService.removerSala(sala.getCodigo());

        assertThrows(
                IllegalStateException.class,
                () -> sala.adicionarJogador("Jogador2")
        );
    }

    @Test
    void deveEncerrarTodasAsSalas() {

        salaService.criarSala("Jogador1");
        salaService.criarSala("Jogador2");
        salaService.criarSala("Jogador3");

        assertEquals(3, salaService.quantidadeSalas());

        salaService.encerrar();

        assertEquals(0, salaService.quantidadeSalas());
    }

    @Test
    void naoDeveCriarSalaAposEncerramento() {

        salaService.encerrar();

        assertThrows(
                IllegalStateException.class,
                () -> salaService.criarSala("Jogador1")
        );
    }

    // Métodos auxiliares

    private void simularInatividade(
            Sala sala,
            long minutos
    ) throws Exception {

        Field campo = Sala.class
                .getDeclaredField("ultimaAtividade");

        campo.setAccessible(true);

        long instanteAnterior = System.nanoTime()
                - TimeUnit.MINUTES.toNanos(minutos);

        campo.setLong(sala, instanteAnterior);
    }

    private void executarLimpeza() throws Exception {

        Method metodo = SalaService.class
                .getDeclaredMethod("limparSalasExpiradas");

        metodo.setAccessible(true);

        metodo.invoke(salaService);
    }
}