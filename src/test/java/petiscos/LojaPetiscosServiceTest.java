package petiscos;
import br.com.dominando.model.*;
import br.com.dominando.service.LojaPetiscosService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LojaPetiscosServiceTest {
    private Jogador jogador;
    private LojaPetiscos loja;
    private LojaPetiscosService service;

    @BeforeEach
    void configurar() {
        jogador = new Jogador("Vitor");
        loja = new LojaPetiscos();
        service = new LojaPetiscosService(loja);
    }

    @Test
    void deveReceberDuasMoedasPorVitoria() {
        jogador.registrarVitoria();

        assertEquals(2, jogador.getPontosPetiscos());
        assertEquals(1, jogador.getVitorias());
    }

    @Test
    void deveAcumularMoedas() {
        jogador.registrarVitoria();
        jogador.registrarVitoria();

        assertEquals(4, jogador.getPontosPetiscos());
    }

    @Test
    void deveComprarOvoColorido() {
        jogador.registrarVitoria();
        loja.abrir(2);

        ResultadoPetiscosLoja resultado = service.comprarPetisco(jogador, TipoPetiscos.OVO_COLORIDO);

        assertEquals(ResultadoPetiscosLoja.COMPRA_REALIZADA, resultado);
        assertEquals(0, jogador.getPontosPetiscos());
        assertEquals(TipoPetiscos.OVO_COLORIDO, jogador.getInventarioPetiscos().getPetiscoAtual());
        assertEquals(3, jogador.getInventarioPetiscos().getRodadaDisponivel());
    }

    @Test
    void naoDeveComprarSemSaldo() {
        loja.abrir(2);

        assertEquals(ResultadoPetiscosLoja.SALDO_INSUFICIENTE, service.comprarPetisco(jogador, TipoPetiscos.COXINHA));
        assertFalse(jogador.getInventarioPetiscos().possuiPetisco());
    }

    @Test
    void naoDeveComprarComLojaFechada() {
        jogador.registrarVitoria();

        assertEquals(ResultadoPetiscosLoja.LOJA_FECHADA, service.comprarPetisco(jogador, TipoPetiscos.OVO_COLORIDO));
        assertEquals(2, jogador.getPontosPetiscos());
    }

    @Test
    void naoDeveComprarDoisPetiscosNaMesmaLoja() {
        jogador.registrarVitoria();
        jogador.registrarVitoria();
        loja.abrir(2);

        service.comprarPetisco(jogador, TipoPetiscos.OVO_COLORIDO);

        assertEquals(ResultadoPetiscosLoja.JA_COMPROU_NESTA_LOJA, service.comprarPetisco(jogador, TipoPetiscos.COXINHA));
    }

    @Test
    void naoDeveComprarMesmoPetiscoEmLojasConsecutivas() {
        jogador.registrarVitoria();
        jogador.registrarVitoria();

        loja.abrir(2);

        service.comprarPetisco(jogador, TipoPetiscos.OVO_COLORIDO);

        loja.fechar();

        jogador.getInventarioPetiscos().expirarPetisco(3);

        loja.abrir(4);

        assertEquals(ResultadoPetiscosLoja.PETISCO_REPETIDO, service.comprarPetisco(jogador, TipoPetiscos.OVO_COLORIDO));
    }

    @Test
    void devePermitirRepetirPetiscoSePularUmaLoja() {
        jogador.registrarVitoria();
        jogador.registrarVitoria();

        loja.abrir(2);

        service.comprarPetisco(jogador, TipoPetiscos.OVO_COLORIDO);

        loja.fechar();

        jogador.getInventarioPetiscos().expirarPetisco(3);

        loja.abrir(4);
        loja.fechar();

        loja.abrir(6);

        assertEquals(ResultadoPetiscosLoja.COMPRA_REALIZADA, service.comprarPetisco(jogador, TipoPetiscos.OVO_COLORIDO));
    }

    @Test
    void deveExpirarPetiscoAoFinalDaRodadaSeguinte() {
        jogador.registrarVitoria();

        loja.abrir(2);

        service.comprarPetisco(jogador, TipoPetiscos.OVO_COLORIDO);

        jogador.getInventarioPetiscos().expirarPetisco(3);

        assertFalse(jogador.getInventarioPetiscos().possuiPetisco());
    }

    @Test
    void deveConsumirPetiscoApenasNaRodadaCorreta() {
        jogador.registrarVitoria();

        loja.abrir(2);

        service.comprarPetisco(jogador, TipoPetiscos.OVO_COLORIDO);

        InventarioPetiscos inventario = jogador.getInventarioPetiscos();

        assertFalse(inventario.podeUsarPetisco(TipoPetiscos.OVO_COLORIDO, 2));
        assertTrue(inventario.podeUsarPetisco(TipoPetiscos.OVO_COLORIDO, 3));

        inventario.consumirPetisco(TipoPetiscos.OVO_COLORIDO, 3);

        assertFalse(inventario.possuiPetisco());
    }

    @Test
    void naoDeveAbrirLojaEmRodadaImpar() {
        assertThrows(IllegalArgumentException.class, () -> loja.abrir(3));
    }

    @Test
    void deveAbrirLojaEmRodadaPar() {
        loja.abrir(2);

        assertTrue(loja.isAberta());
        assertEquals(1, loja.getNumeroLoja());
        assertEquals(3, loja.getProximaRodada());
    }
}
