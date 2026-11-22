import static org.junit.jupiter.api.Assertions.*;
import br.com.dominando.model.*;
import org.junit.jupiter.api.Test;
import br.com.dominando.service.MesaService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PartidaTest {

    @Test
    void deveAceitarDeDoisAQuatroJogadores() {
        assertDoesNotThrow(() -> new Partida(List.of("Vitor", "Yago")));

        assertDoesNotThrow(() -> new Partida(List.of("Vitor", "Yago", "Lucas", "Ana")));
    }

    @Test
    void deveDistribuirSetePecasPorJogador() {
        Partida partida = new Partida(List.of("Vitor", "Yago", "Lucas"));

        partida.iniciarPartida();

        for (Jogador jogador : partida.getJogadores()) {
            assertEquals(7, jogador.quantidadePecas());
        }

        assertEquals(7, partida.getBaralho().quantidadePecas());
    }

    @Test
    void quatroJogadoresDevemEsgotarBaralho() {
        Partida partida = new Partida(List.of("Vitor", "Yago", "Lucas", "Ana"));

        partida.iniciarPartida();

        assertEquals(0, partida.getBaralho().quantidadePecas());
    }

    @Test
    void distribuicaoNaoDeveDuplicarPecas() {
        Partida partida = new Partida(List.of("Vitor", "Yago", "Lucas", "Ana"));

        partida.iniciarPartida();

        Set<Peca> pecas = new HashSet<>();

        for (Jogador jogador : partida.getJogadores()) {
            pecas.addAll(jogador.getMao());
        }

        assertEquals(28, pecas.size());
    }

    @Test
    void primeiraPartidaDeveComecarPelaMaiorBucha() {
        Partida partida =
                new Partida(List.of("Vitor", "Yago"));

        partida.iniciarPartida();

        Peca maiorBucha = null;
        Jogador dono = null;

        for (Jogador jogador : partida.getJogadores()) {
            for (Peca peca : jogador.getMao()) {
                if (peca.bucha() && (maiorBucha == null || peca.getLadoA() > maiorBucha.getLadoA())) {
                    maiorBucha = peca;
                    dono = jogador;
                }
            }
        }

        if (maiorBucha != null) {
            assertSame(dono, partida.getJogadorVez());
        } else {
            Peca maiorPeca = null;
            Jogador donoMaiorPeca = null;

            for (Jogador jogador : partida.getJogadores()) {
                for (Peca peca : jogador.getMao()) {
                    if (maiorPeca == null || peca.valorTotal() > maiorPeca.valorTotal()) {
                        maiorPeca = peca;
                        donoMaiorPeca = jogador;
                    }
                }
            }

            assertSame(donoMaiorPeca, partida.getJogadorVez());
        }
    }

    @Test
    void deveExigirPecaObrigatoriaNaPrimeiraJogada() {
        Partida partida = new Partida(List.of("Vitor", "Yago", "Lucas", "Ana"));
        partida.iniciarPartida();
        Jogador inicial = partida.getJogadorVez();

        Peca maiorBucha = null;

        for (Jogador jogador : partida.getJogadores()) {
            for (Peca peca : jogador.getMao()) {
                if (peca.bucha() && (maiorBucha == null || peca.getLadoA() > maiorBucha.getLadoA())) {
                    maiorBucha = peca;
                }
            }
        }

        assertNotNull(maiorBucha);

        Peca outraPeca = null;

        for (Peca peca : inicial.getMao()) {
            if (!peca.equals(maiorBucha)) {
                outraPeca = peca;
                break;
            }
        }

        assertNotNull(outraPeca);

        ResultadoJogada resultado = partida.jogar(outraPeca, LadoMesa.DIREITA);

        assertEquals(ResultadoJogada.DEVE_JOGAR_MAIOR_BUCHA, resultado);

        assertSame(inicial, partida.getJogadorVez());
        assertTrue(inicial.possuiPeca(outraPeca));
    }

    @Test
    void vencedorAnteriorDeveComecarSemBuchaObrigatoria() {
        Jogador vitor = new Jogador("Vitor");
        Jogador yago = new Jogador("Yago");

        Partida partida = new Partida(List.of(vitor, yago), yago);

        partida.iniciarPartida();

        assertSame(yago, partida.getJogadorVez());

        Peca qualquerPeca = yago.getMao().getFirst();

        ResultadoJogada resultado = partida.jogar(qualquerPeca, LadoMesa.DIREITA);

        assertEquals(ResultadoJogada.JOGADA_VALIDA, resultado);
    }

    @Test
    void naoDeveAceitarJogadorInicialForaDaPartida() {
        Jogador vitor = new Jogador("Vitor");
        Jogador yago = new Jogador("Yago");
        Jogador lucas = new Jogador("Lucas");

        assertThrows(IllegalArgumentException.class, () -> new Partida(List.of(vitor, yago), lucas));
    }
}
