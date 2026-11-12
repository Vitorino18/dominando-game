import static org.junit.jupiter.api.Assertions.*;
import br.com.dominando.model.*;
import br.com.dominando.controller.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import br.com.dominando.service.MesaService;

import java.util.*;

public class GameControllerTest {
    private PartidaGeral geral;
    private GameController controller;

    @BeforeEach
    void preparar() {
        geral = new PartidaGeral(List.of("Vitor", "Yago"));
        geral.inicioCompeticao();

        controller = new GameController(geral, new Scanner(""));
    }

    @Test
    void deveEncaminharJogadaParaPartida() {
        Partida partida = geral.getPartidaAtual();
        Jogador jogador = partida.getJogadorVez();

        Peca obrigatoria = null;

        for (Peca peca : jogador.getMao()) {
            if (peca.bucha()) {
                if (obrigatoria == null || peca.getLadoA() > obrigatoria.getLadoA()) {
                    obrigatoria = peca;
                }
            }
        }

        // Se não houver bucha, usa a maior peça da mão.
        if (obrigatoria == null) {
            for (Peca peca : jogador.getMao()) {
                if (obrigatoria == null || peca.valorTotal() > obrigatoria.valorTotal()) {
                    obrigatoria = peca;
                }
            }
        }

        int indice = jogador.getMao().indexOf(obrigatoria);

        ResultadoJogada resultado = controller.executarAcao(AcaoJogo.JOGAR_PECA, indice, LadoMesa.DIREITA);

        assertEquals(ResultadoJogada.JOGADA_VALIDA, resultado);
        assertEquals(1, partida.getMesa().quantidadePecasMesa());
    }

    @Test
    void deveImpedirCompraQuandoExisteJogadaValida() {
        ResultadoJogada resultado = controller.executarAcao(AcaoJogo.COMPRAR_PECA, null, null);

        assertEquals(ResultadoJogada.POSSUI_JOGADA_VALIDA, resultado);
    }

    @Test
    void deveImpedirPassarNaPrimeiraJogada() {
        ResultadoJogada resultado = controller.executarAcao(AcaoJogo.PASSAR_VEZ, null, null);

        assertEquals(ResultadoJogada.NAO_PODE_PASSAR, resultado);
    }

}
