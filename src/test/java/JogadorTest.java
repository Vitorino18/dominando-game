import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import br.com.dominando.model.*;

class JogadorTest {

    @Test
    void jogadorDeveComecarSemVitorias() {
        Jogador jogador = new Jogador("Vitor");

        assertEquals(0, jogador.getVitorias());
        assertEquals(0, jogador.getPontosPetiscos());
        assertEquals(0, jogador.getPontuacao());
    }

    @Test
    void registrarVitoriaDeveAdicionarTampinhaEPetiscos() {
        Jogador jogador = new Jogador("Vitor");

        jogador.registrarVitoria();

        assertEquals(1, jogador.getVitorias());
        assertEquals(10, jogador.getPontosPetiscos());
    }

    @Test
    void cincoVitoriasDevemGerarCinquentaPetiscos() {
        Jogador jogador = new Jogador("Vitor");

        for (int i = 0; i < 5; i++) {
            jogador.registrarVitoria();
        }

        assertEquals(5, jogador.getVitorias());
        assertEquals(50, jogador.getPontosPetiscos());
    }

    @Test
    void pontosDaRodadaNaoDevemAlterarPetiscos() {
        Jogador jogador = new Jogador("Vitor");

        jogador.adicionarPontos(25);

        assertEquals(25, jogador.getPontuacao());
        assertEquals(0, jogador.getPontosPetiscos());
        assertEquals(0, jogador.getVitorias());
    }

    @Test
    void limparMaoDeveRemoverTodasAsPecas() {
        Jogador jogador = new Jogador("Vitor");
        Baralho baralho = new Baralho();

        jogador.comprarDoBaralho(baralho);
        jogador.comprarDoBaralho(baralho);

        assertEquals(2, jogador.quantidadePecas());

        jogador.limparMao();

        assertEquals(0, jogador.quantidadePecas());
    }
}
