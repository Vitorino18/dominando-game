import static org.junit.jupiter.api.Assertions.*;

import br.com.dominando.model.Partida;
import org.junit.jupiter.api.Test;
import br.com.dominando.model.ResultadoJogada;
import br.com.dominando.service.MesaService;

import java.util.ArrayList;
import java.util.List;

public class PartidaTest {
    @Test
    void naoDevePermitirPassarQuandoExisteJogada() {

        // Arrange
        Partida partida = new Partida(List.of("Vitor", "Yago"));
        partida.iniciarPartida();

        // Act
        ResultadoJogada resultado = partida.passarVez();

        // Assert
        assertEquals(ResultadoJogada.NAO_PODE_PASSAR, resultado);
    }

    @Test
    void naoDevePermitirComprarQuandoExisteJogada() {

        // Arrange
        Partida partida = new Partida(List.of("Vitor", "Yago"));
        partida.iniciarPartida();

        // Act
        ResultadoJogada resultado = partida.comprarPeca();

        // Assert
        assertEquals(ResultadoJogada.POSSUI_JOGADA_VALIDA, resultado);
    }
}
