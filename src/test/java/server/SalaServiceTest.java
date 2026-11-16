package server;

import br.com.dominando.server.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SalaServiceTest {
    private SalaService service;

    @BeforeEach
    void preparar() {
        service = new SalaService();
    }

    @Test
    void deveIdentificarJogadorPeloToken() {
        Sala sala = new Sala("AB12CD", "Vitor");
        ParticipanteSala yago = sala.adicionarJogador("Yago");

        assertSame(yago, sala.buscarPorToken(yago.getToken()));
    }

    @Test
    void somenteCriadorPodeIniciarPartida() {
        Sala sala = new Sala("AB12CD", "Vitor");
        ParticipanteSala yago = sala.adicionarJogador("Yago");
        assertThrows(SecurityException.class, () -> sala.iniciarPartida(yago.getToken()));

        assertFalse(sala.isIniciada());
    }

    @Test
    void criadorPodeIniciarComDoisJogadores() {
        Sala sala = new Sala("AB12CD", "Vitor");
        sala.adicionarJogador("Yago");
        String tokenCriador = sala.getCriador().getToken();
        sala.iniciarPartida(tokenCriador);

        assertTrue(sala.isIniciada());
        assertNotNull(sala.getPartidaGeral());
    }

    @Test
    void naoDeveAceitarTerceiroJogador() {
        Sala sala = new Sala("AB12CD", "Vitor");
        sala.adicionarJogador("Yago");

        assertThrows(IllegalStateException.class, () -> sala.adicionarJogador("Lucas"));
    }

    @Test
    void naoDeveIniciarComTokenInvalido() {
        Sala sala = new Sala("AB12CD", "Vitor");
        sala.adicionarJogador("Yago");

        assertThrows(SecurityException.class, () -> sala.iniciarPartida("TOKEN-INVALIDO"));
    }

    @Test
    void deveCriarSalaComCodigoDeSeisCaracteres() {
        Sala sala = service.criarSala("Vitor");

        assertNotNull(sala);
        assertEquals(6, sala.getCodigo().length());
        assertEquals(1, sala.getJogadores().size());
        assertEquals(1, service.quantidadeSalas());
    }

    @Test
    void deveCriarSalasComCodigosDiferentes() {
        Sala sala1 = service.criarSala("Vitor");
        Sala sala2 = service.criarSala("Lucas");

        assertNotEquals(sala1.getCodigo(), sala2.getCodigo());
    }

    @Test
    void deveBuscarSalaPeloCodigo() {
        Sala sala = service.criarSala("Vitor");
        Sala encontrada = service.buscarSala(sala.getCodigo().toLowerCase());

        assertSame(sala, encontrada);
    }

    @Test
    void devePermitirEntradaDoSegundoJogador() {
        Sala sala = service.criarSala("Vitor");
        service.entrarNaSala(sala.getCodigo(), "Yago");

        assertEquals(2, sala.getJogadores().size());
        assertTrue(sala.podeIniciar());
    }


    @Test
    void deveRejeitarSalaInexistente() {
        assertThrows(IllegalArgumentException.class, () -> service.entrarNaSala("XXXXXX", "Yago"));
    }

    @Test
    void deveIniciarSalaComDoisJogadores() {
        Sala sala = service.criarSala("Vitor");

        service.entrarNaSala(sala.getCodigo(), "Yago");
        service.iniciarSala(sala.getCodigo(), sala.getCriador().getToken());

        assertTrue(sala.isIniciada());
        assertNotNull(sala.getPartidaGeral());
        assertNotNull(sala.getPartidaGeral().getPartidaAtual());
    }

    @Test
    void naoDeveIniciarSalaComUmJogador() {
        Sala sala = service.criarSala("Vitor");

        assertThrows(IllegalStateException.class, () -> service.iniciarSala(sala.getCodigo(),sala.getCriador().getToken()));
    }

    @Test
    void deveRemoverSala() {
        Sala sala = service.criarSala("Vitor");

        assertTrue(service.removerSala(sala.getCodigo()));
        assertNull(service.buscarSala(sala.getCodigo()));
        assertEquals(0, service.quantidadeSalas());
    }
}
