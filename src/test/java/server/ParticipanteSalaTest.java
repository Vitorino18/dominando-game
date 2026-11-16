package server;

import br.com.dominando.server.ParticipanteSala;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;



public class ParticipanteSalaTest {
    @Test
    void deveGerarTokensDiferentes() {
        ParticipanteSala jogador1 = new ParticipanteSala("Vitor", true);

        ParticipanteSala jogador2 = new ParticipanteSala("Yago", false);

        assertNotEquals(jogador1.getToken(), jogador2.getToken());
    }

    @Test
    void deveIdentificarCriador() {
        ParticipanteSala jogador = new ParticipanteSala("Vitor", true);

        assertTrue(jogador.isCriador());
    }

    @Test
    void deveIdentificarConvidado() {
        ParticipanteSala jogador = new ParticipanteSala("Yago", false);

        assertFalse(jogador.isCriador());
    }

    @Test
    void naoDeveAceitarNomeVazio() {
        assertThrows(IllegalArgumentException.class, () -> new ParticipanteSala(" ", false));
    }
}
