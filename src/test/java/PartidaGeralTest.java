import static org.junit.jupiter.api.Assertions.*;
import br.com.dominando.model.*;
import org.junit.jupiter.api.Test;
import br.com.dominando.service.MesaService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PartidaGeralTest {
    @Test
    void deveCriarCompeticaoComJogadores() {
        PartidaGeral geral = new PartidaGeral(List.of("Vitor", "Yago"));

        assertEquals(2, geral.getJogadores().size());
        assertNull(geral.getPartidaAtual());
        assertNull(geral.getCampeao());
        assertFalse(geral.competicaoEncerrada());
    }

    @Test
    void deveRejeitarQuantidadeInvalida() {
        assertThrows(IllegalArgumentException.class, () -> new PartidaGeral(List.of("Vitor")));

        assertThrows(IllegalArgumentException.class, () -> new PartidaGeral(List.of("A", "B", "C", "D", "E")));
    }

    @Test
    void deveRejeitarNomeVazio() {
        assertThrows(IllegalArgumentException.class, () -> new PartidaGeral(List.of("Vitor", " ")));
    }

    @Test
    void deveIniciarPrimeiraPartida() {
        PartidaGeral geral = new PartidaGeral(List.of("Vitor", "Yago"));

        geral.inicioCompeticao();

        assertNotNull(geral.getPartidaAtual());
        assertNotNull(geral.getJogadorInicialPartidaAtual());
        assertFalse(geral.competicaoEncerrada());
    }

    @Test
    void naoDeveIniciarCompeticaoDuasVezes() {
        PartidaGeral geral = new PartidaGeral(List.of("Vitor", "Yago"));

        geral.inicioCompeticao();

        assertThrows(IllegalStateException.class, geral::inicioCompeticao);
    }

    @Test
    void verificarFimNaoDeveAlterarPartidaEmAndamento() {
        PartidaGeral geral = new PartidaGeral(List.of("Vitor", "Yago"));

        geral.inicioCompeticao();

        Partida partidaOriginal = geral.getPartidaAtual();

        geral.verificarFimPartida();

        assertSame(partidaOriginal, geral.getPartidaAtual());
        assertNull(geral.getCampeao());
    }

    @Test
    void vencedorDeveGanharTampinhaEDezPetiscos() {
        PartidaGeral geral = new PartidaGeral(List.of("Vitor", "Yago"));

        geral.inicioCompeticao();

        Jogador vencedor = geral.getJogadores().getFirst();

        vencedor.limparMao();

        geral.verificarFimPartida();

        assertEquals(1, vencedor.getVitorias());
        assertEquals(10, vencedor.getPontosPetiscos());
        assertSame(vencedor, geral.getVencedorAnterior());
    }

    @Test
    void vencedorAnteriorDeveIniciarProximaPartida() {
        PartidaGeral geral = new PartidaGeral(List.of("Vitor", "Yago"));

        geral.inicioCompeticao();

        Jogador vencedor = geral.getJogadores().getFirst();

        vencedor.limparMao();
        geral.verificarFimPartida();

        assertSame(vencedor, geral.getPartidaAtual().getJogadorVez());

        assertSame(vencedor, geral.getJogadorInicialPartidaAtual());

        assertEquals(7, vencedor.quantidadePecas());
    }

    @Test
    void deveEncerrarCompeticaoNaQuintaVitoria() {
        PartidaGeral geral = new PartidaGeral(List.of("Vitor", "Yago"));

        geral.inicioCompeticao();

        Jogador vencedor = geral.getJogadores().getFirst();

        for (int i = 0; i < 5; i++) {
            vencedor.limparMao();
            geral.verificarFimPartida();
        }

        assertEquals(5, vencedor.getVitorias());
        assertEquals(50, vencedor.getPontosPetiscos());

        assertTrue(geral.competicaoEncerrada());
        assertSame(vencedor, geral.getCampeao());
        assertNull(geral.getPartidaAtual());
    }

    @Test
    void naoDeveEncerrarCompeticaoComQuatroVitorias() {
        PartidaGeral geral = new PartidaGeral(List.of("Vitor", "Yago"));

        geral.inicioCompeticao();

        Jogador vencedor = geral.getJogadores().getFirst();

        for (int i = 0; i < 4; i++) {
            vencedor.limparMao();
            geral.verificarFimPartida();
        }

        assertEquals(4, vencedor.getVitorias());
        assertFalse(geral.competicaoEncerrada());
        assertNotNull(geral.getPartidaAtual());
    }

    @Test
    void deveManterMesmosObjetosJogadoresEntrePartidas() {
        PartidaGeral geral = new PartidaGeral(List.of("Vitor", "Yago"));

        geral.inicioCompeticao();

        Jogador jogador = geral.getJogadores().getFirst();

        jogador.limparMao();
        geral.verificarFimPartida();

        assertSame(jogador, geral.getPartidaAtual().getJogadores().getFirst());
    }


    // caso tenha empate no jogo //


    @Test
    void empateDeveReiniciarSemPremiarJogadores() {
        PartidaGeral geral = new PartidaGeral(List.of("Vitor", "Yago", "Lucas", "Ana"));

        geral.inicioCompeticao();

        Partida partidaAnterior = geral.getPartidaAtual();

        assertEquals(0, partidaAnterior.getBaralho().quantidadePecas());

        // Prepara uma mesa cujas pontas são zero.
        partidaAnterior.getMesa().getListaInterna().clear();

        partidaAnterior.getMesa().getListaInterna().add(new Peca(0, 0));


        for (Jogador jogador : geral.getJogadores()) {
            jogador.limparMao();
        }

        Jogador vitor = geral.getJogadores().get(0);
        Jogador yago = geral.getJogadores().get(1);
        Jogador lucas = geral.getJogadores().get(2);
        Jogador ana = geral.getJogadores().get(3);

        adicionarPecaParaTeste(vitor, new Peca(2, 1)); // 3
        adicionarPecaParaTeste(yago, new Peca(3, 3));  // 6
        adicionarPecaParaTeste(lucas, new Peca(3, 2)); // 5
        adicionarPecaParaTeste(ana, new Peca(2, 1));   // 3

        assertTrue(partidaAnterior.rodadaTravada());

        geral.verificarFimPartida();

        assertNotSame(partidaAnterior, geral.getPartidaAtual());
        assertFalse(geral.competicaoEncerrada());

        for (Jogador jogador : geral.getJogadores()) {
            assertEquals(0, jogador.getVitorias());
            assertEquals(0, jogador.getPontosPetiscos());
        }

        assertNotNull(geral.getJogadorInicialPartidaAtual());
        assertSame(geral.getJogadorInicialPartidaAtual(), geral.getPartidaAtual().getJogadorVez());
    }

    private void adicionarPecaParaTeste(Jogador jogador, Peca peca) {
        Baralho baralho = new Baralho();

        while (baralho.quantidadePecas() > 0) {
            if (baralho.getPecas().getFirst().equals(peca)) {
                jogador.comprarDoBaralho(baralho);
                return;
            }
            baralho.comprarPeca();
        }

        throw new IllegalArgumentException("Peça não encontrada.");

    }
}
