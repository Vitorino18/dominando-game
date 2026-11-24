package petiscos;
import br.com.dominando.model.*;

import br.com.dominando.service.PetiscoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PetiscoServiceTest {
    private Partida partida;
    private Jogador jogador;
    private Jogador adversario;
    private PetiscoService service;

    @BeforeEach
    void configurar() {
        Jogador primeiro = new Jogador("Vitor");
        Jogador segundo = new Jogador("Yago");

        partida = new Partida(List.of(primeiro, segundo), primeiro);

        partida.iniciarPartida();

        jogador = primeiro;
        adversario = segundo;

        service = new PetiscoService();
    }

    private void concederPetisco(Jogador jogador, TipoPetiscos tipo) {
        jogador.getInventarioPetiscos().registrarCompra(tipo, 1, 3);
    }

    @Test
    void deveRevelarPecaDoAdversario() {
        concederPetisco(jogador, TipoPetiscos.OVO_COLORIDO);

        UsoPetisco uso = service.utilizarPetisco(partida, jogador, TipoPetiscos.OVO_COLORIDO, 3, FasePartida.EM_ANDAMENTO, null);

        assertTrue(uso.sucesso());
        assertNotNull(uso.pecaRevelada());
        assertEquals(5, uso.duracaoRevelacaoSegundos());

        assertTrue(adversario.getMao().stream().anyMatch(p -> p.getLadoA() == uso.pecaRevelada().getLadoA()
                && p.getLadoB() == uso.pecaRevelada().getLadoB()));
        assertFalse(jogador.getInventarioPetiscos().possuiPetisco());
    }

    @Test
    void deveTrocarPecaComCoxinha() {
        concederPetisco(jogador, TipoPetiscos.COXINHA);

        Peca pecaAntiga = jogador.getPeca(0);

        int quantidadeMao = jogador.quantidadePecas();

        int quantidadeCava = partida.getBaralho().quantidadePecas();

        List<Peca> maoAdversario = List.copyOf(adversario.getMao());

        UsoPetisco uso = service.utilizarPetisco(partida, jogador, TipoPetiscos.COXINHA, 3, FasePartida.EM_ANDAMENTO, 0);

        assertTrue(uso.sucesso());
        assertNotSame(pecaAntiga, jogador.getPeca(0));
        assertTrue(partida.getBaralho().getPecas().contains(pecaAntiga));
        assertEquals(quantidadeMao, jogador.quantidadePecas());
        assertEquals(quantidadeCava, partida.getBaralho().quantidadePecas());
        assertEquals(maoAdversario, adversario.getMao());
    }

    @Test
    void deveRedistribuirMaoComTorresmo() {
        concederPetisco(jogador, TipoPetiscos.TORRESMO);

        List<Peca> maoAdversario = List.copyOf(adversario.getMao());

        UsoPetisco uso = service.utilizarPetisco(partida, jogador, TipoPetiscos.TORRESMO, 3, FasePartida.PREPARACAO_RODADA, null);

        assertTrue(uso.sucesso());
        assertEquals(7, jogador.quantidadePecas());
        assertEquals(14, partida.getBaralho().quantidadePecas());
        assertEquals(maoAdversario, adversario.getMao());
        assertTrue(partida.getMesa().getPecasMesa().isEmpty());
        assertFalse(jogador.getInventarioPetiscos().possuiPetisco());
    }

    @Test
    void naoDeveUsarPetiscoForaDoTurno() {
        concederPetisco(adversario, TipoPetiscos.OVO_COLORIDO);

        UsoPetisco uso = service.utilizarPetisco(partida, adversario, TipoPetiscos.OVO_COLORIDO, 3, FasePartida.EM_ANDAMENTO, null);

        assertEquals(ResultadoUsoPetisco.NAO_E_SUA_VEZ, uso.resultado());
        assertTrue(adversario.getInventarioPetiscos().possuiPetisco());
    }

    @Test
    void naoDeveUsarTorresmoDuranteJogadas() {

        concederPetisco(jogador, TipoPetiscos.TORRESMO);

        UsoPetisco uso = service.utilizarPetisco(
                partida,
                jogador,
                TipoPetiscos.TORRESMO,
                3,
                FasePartida.EM_ANDAMENTO,
                null
        );

        assertEquals(ResultadoUsoPetisco.FASE_INVALIDA, uso.resultado());
    }

    @Test
    void naoDeveConsumirCoxinhaComIndiceInvalido() {

        concederPetisco(
                jogador, TipoPetiscos.COXINHA
        );

        UsoPetisco uso = service.utilizarPetisco(
                partida,
                jogador,
                TipoPetiscos.COXINHA,
                3,
                FasePartida.EM_ANDAMENTO,
                99
        );

        assertEquals(
                ResultadoUsoPetisco.PECA_INVALIDA,
                uso.resultado()
        );

        assertTrue(
                jogador.getInventarioPetiscos()
                        .possuiPetisco()
        );
    }

    @Test
    void naoDeveUtilizarPetiscoDuasVezes() {

        concederPetisco(
                jogador, TipoPetiscos.OVO_COLORIDO
        );

        service.utilizarPetisco(
                partida,
                jogador,
                TipoPetiscos.OVO_COLORIDO,
                3,
                FasePartida.EM_ANDAMENTO,
                null
        );

        UsoPetisco segundaTentativa =
                service.utilizarPetisco(
                        partida,
                        jogador,
                        TipoPetiscos.OVO_COLORIDO,
                        3,
                        FasePartida.EM_ANDAMENTO,
                        null
                );

        assertEquals(
                ResultadoUsoPetisco.PETISCO_INDISPONIVEL,
                segundaTentativa.resultado()
        );
    }

    @Test
    void naoDeveUsarPetiscoNaRodadaErrada() {

        concederPetisco(
                jogador, TipoPetiscos.OVO_COLORIDO
        );

        UsoPetisco uso = service.utilizarPetisco(
                partida,
                jogador,
                TipoPetiscos.OVO_COLORIDO,
                4,
                FasePartida.EM_ANDAMENTO,
                null
        );

        assertEquals(
                ResultadoUsoPetisco.PETISCO_INDISPONIVEL,
                uso.resultado()
        );
    }

    @Test
    void devePermitirQueAmbosUtilizemTorresmo() {

        concederPetisco(
                jogador, TipoPetiscos.TORRESMO
        );

        concederPetisco(
                adversario, TipoPetiscos.TORRESMO
        );

        UsoPetisco primeiro = service.utilizarPetisco(
                partida,
                jogador,
                TipoPetiscos.TORRESMO,
                3,
                FasePartida.PREPARACAO_RODADA,
                null
        );

        UsoPetisco segundo = service.utilizarPetisco(
                partida,
                adversario,
                TipoPetiscos.TORRESMO,
                3,
                FasePartida.PREPARACAO_RODADA,
                null
        );

        assertTrue(primeiro.sucesso());
        assertTrue(segundo.sucesso());

        assertEquals(7, jogador.quantidadePecas());
        assertEquals(7, adversario.quantidadePecas());

        assertEquals(
                14,
                partida.getBaralho().quantidadePecas()
        );
    }
}
