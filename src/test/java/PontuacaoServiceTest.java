import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import br.com.dominando.model.*;
import br.com.dominando.service.*;
import br.com.dominando.controller.*;
import java.util.List;

public class PontuacaoServiceTest {

    private final PontuacaoService service = new PontuacaoService();

    private void adicionarPeca(Jogador jogador, Peca peca) {
        Baralho baralho = new Baralho();

        baralho.getPecas();

        while (baralho.quantidadePecas() > 0) {
            if (baralho.getPecas().getFirst().equals(peca)) {
                jogador.comprarDoBaralho(baralho);
                return;
            }
            baralho.comprarPeca();
        }

        fail("Peça não encontrada no baralho.");
    }

    @Test
    void deveEncontrarVencedorComMenorSoma() {
        Jogador vitor = new Jogador("Vitor");
        Jogador yago = new Jogador("Yago");
        Jogador lucas = new Jogador("Lucas");

        adicionarPeca(vitor, new Peca(2, 1)); // 3
        adicionarPeca(yago, new Peca(6, 5));  // 11
        adicionarPeca(lucas, new Peca(4, 3)); // 7

        List<Jogador> vencedores = service.vencedorRodadaTravada(List.of(vitor, yago, lucas));

        assertEquals(1, vencedores.size());
        assertEquals(vitor, vencedores.getFirst());
    }

    @Test
    void empateDeveRetornarTodosComMenorPontuacao() {
        Jogador vitor = new Jogador("Vitor");
        Jogador yago = new Jogador("Yago");
        Jogador lucas = new Jogador("Lucas");

        adicionarPeca(vitor, new Peca(2, 1)); // 3
        adicionarPeca(yago, new Peca(3, 0));  // 3
        adicionarPeca(lucas, new Peca(6, 5)); // 11

        List<Jogador> vencedores = service.vencedorRodadaTravada(List.of(vitor, yago, lucas));

        assertEquals(2, vencedores.size());
        assertTrue(vencedores.contains(vitor));
        assertTrue(vencedores.contains(yago));
        assertFalse(vencedores.contains(lucas));
    }

    @Test
    void deveSomarPontosDosAdversarios() {
        Jogador vitor = new Jogador("Vitor");
        Jogador yago = new Jogador("Yago");
        Jogador lucas = new Jogador("Lucas");

        adicionarPeca(yago, new Peca(6, 5));  // 11
        adicionarPeca(lucas, new Peca(4, 3)); // 7

        int pontos = service.calcularPontosRodada(
                List.of(vitor, yago, lucas), vitor);

        assertEquals(18, pontos);
        assertEquals(18, vitor.getPontuacao());
        assertEquals(0, yago.getPontuacao());
    }
}