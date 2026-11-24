package petiscos;

import br.com.dominando.grpc.*;
import br.com.dominando.model.*;
import br.com.dominando.server.*;

import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import br.com.dominando.grpc.*;
import static org.junit.jupiter.api.Assertions.*;

public class DesceUmaPetiscosTest {
    private Server servidor;
    private ManagedChannel canalVitor;
    private ManagedChannel canalYago;
    private DesceUmaServiceGrpc.DesceUmaServiceBlockingStub stubVitor;
    private DesceUmaServiceGrpc.DesceUmaServiceBlockingStub stubYago;

    private SalaService salaService;

    private String codigoSala;
    private String tokenVitor;
    private String tokenYago;

    // CONFIGURAÇÃO DO SERVIDOR E DOS CLIENTES //

    @BeforeEach
    void configurar() throws Exception {

        salaService = new SalaService();

        servidor = ServerBuilder.forPort(0)
                .addService(new DesceUmaGrpcService(salaService))
                .build()
                .start();

        int porta = servidor.getPort();

        canalVitor = ManagedChannelBuilder
                .forAddress("localhost", porta)
                .usePlaintext()
                .build();

        canalYago = ManagedChannelBuilder
                .forAddress("localhost", porta)
                .usePlaintext()
                .build();

        stubVitor = DesceUmaServiceGrpc
                .newBlockingStub(canalVitor);

        stubYago = DesceUmaServiceGrpc
                .newBlockingStub(canalYago);

        CriarSalaResponse criacao = stubVitor.criarSala(
                CriarSalaRequest.newBuilder()
                        .setNomeCriador("Vitor")
                        .build()
        );

        codigoSala = criacao.getCodigoSala();
        tokenVitor = criacao.getTokenJogador();

        EntrarSalaResponse entrada = stubYago.entrarSala(
                EntrarSalaRequest.newBuilder()
                        .setCodigoSala(codigoSala)
                        .setNomeJogador("Yago")
                        .build()
        );

        tokenYago = entrada.getTokenJogador();

        IniciarPartidaResponse inicio =
                stubVitor.iniciarPartida(
                        IniciarPartidaRequest.newBuilder()
                                .setCodigoSala(codigoSala)
                                .setTokenCriador(tokenVitor)
                                .build()
                );

        assertTrue(inicio.getIniciada());

        System.out.println(
                "\n========================================"
        );

        System.out.println(
                "[TESTE] Sala criada: " + codigoSala
        );

        System.out.println(
                "[TESTE] Servidor iniciado na porta: " + porta
        );

        System.out.println(
                "========================================"
        );
    }

    @AfterEach
    void encerrar() throws Exception {

        if (canalVitor != null) {
            canalVitor.shutdownNow();
            canalVitor.awaitTermination(5, TimeUnit.SECONDS);
        }

        if (canalYago != null) {
            canalYago.shutdownNow();
            canalYago.awaitTermination(5, TimeUnit.SECONDS);
        }

        if (servidor != null) {
            servidor.shutdownNow();
            servidor.awaitTermination(5, TimeUnit.SECONDS);
        }

        System.out.println("[TESTE] Servidor encerrado.");
    }

    // MÉTODOS AUXILIARES

    private EstadoJogoRequest criarRequest(String token) {

        return EstadoJogoRequest.newBuilder()
                .setCodigoSala(codigoSala)
                .setTokenJogador(token)
                .build();
    }

    private EstadoJogoResponse estadoVitor() {

        return stubVitor
                .withDeadlineAfter(5, TimeUnit.SECONDS)
                .consultarEstadoJogo(criarRequest(tokenVitor));
    }

    private EstadoJogoResponse estadoYago() {

        return stubYago
                .withDeadlineAfter(5, TimeUnit.SECONDS)
                .consultarEstadoJogo(criarRequest(tokenYago));
    }

    private EstadoJogoResponse consultarEstado(String nome) {

        return nome.equals("Vitor")
                ? estadoVitor()
                : estadoYago();
    }

    private JogadorMensagem buscarJogador(
            EstadoJogoResponse estado,
            String nome) {

        return estado.getJogadoresList()
                .stream()
                .filter(j -> j.getNome().equals(nome))
                .findFirst()
                .orElseThrow();
    }

    private String tokenDe(String nome) {

        return nome.equals("Vitor")
                ? tokenVitor
                : tokenYago;
    }

    private DesceUmaServiceGrpc.DesceUmaServiceBlockingStub
    stubDe(String nome) {

        return (nome.equals("Vitor") ? stubVitor : stubYago)
                .withDeadlineAfter(5, TimeUnit.SECONDS);
    }

    private String adversarioDe(String nome) {

        return nome.equals("Vitor")
                ? "Yago"
                : "Vitor";
    }

    private ComprarPetiscoResponse comprarPetisco(
            String nome,
            TipoPetiscoMensagem tipo) {

        return stubDe(nome).comprarPetisco(
                ComprarPetiscoRequest.newBuilder()
                        .setCodigoSala(codigoSala)
                        .setTokenJogador(tokenDe(nome))
                        .setTipo(tipo)
                        .build()
        );
    }

    private UtilizarPetiscoResponse utilizarPetisco(
            String nome,
            TipoPetiscoMensagem tipo,
            Integer indicePeca) {

        UtilizarPetiscoRequest.Builder request =
                UtilizarPetiscoRequest.newBuilder()
                        .setCodigoSala(codigoSala)
                        .setTokenJogador(tokenDe(nome))
                        .setTipo(tipo);

        if (indicePeca != null) {
            request.setIndicePeca(indicePeca);
        }

        return stubDe(nome).utilizarPetisco(request.build());
    }

    private EstadoJogoResponse aguardarFase(
            FasePartidaMensagem faseEsperada,
            int limiteSegundos) throws InterruptedException {

        long prazo = System.nanoTime()
                + TimeUnit.SECONDS.toNanos(limiteSegundos);

        while (System.nanoTime() - prazo < 0) {

            EstadoJogoResponse estado = estadoVitor();

            if (estado.getFase() == faseEsperada) {
                return estado;
            }

            Thread.sleep(100);
        }

        fail("Tempo excedido aguardando fase: " + faseEsperada);

        return null;
    }

    // SIMULAR O ENCERRAMENTO DE UMA RODADA

    private String finalizarRodada() {

        Sala sala = salaService.buscarSala(codigoSala);

        assertNotNull(sala);

        String nomeVencedor;

        synchronized (sala) {

            Partida partida = sala.getPartidaGeral()
                    .getPartidaAtual();

            assertTrue(
                    partida.getMesa().getPecasMesa().isEmpty(),
                    "Este método deve ser usado no início da rodada."
            );

            Jogador jogador = partida.getJogadorVez();

            nomeVencedor = jogador.getNome();

            List<Peca> mao = List.copyOf(jogador.getMao());

            // Prioriza a maior bucha da mão.
            // Caso não exista, seleciona a maior peça.

            Peca escolhida = mao.stream()
                    .filter(Peca::bucha)
                    .max((a, b) -> Integer.compare(
                            a.getLadoA(),
                            b.getLadoA()
                    ))
                    .orElseGet(() -> mao.stream()
                            .max((a, b) -> Integer.compare(
                                    a.getLadoA() + a.getLadoB(),
                                    b.getLadoA() + b.getLadoB()
                            ))
                            .orElseThrow()
                    );

            jogador.limparMao();

            for (Peca peca : mao) {

                if (peca != escolhida) {
                    partida.getBaralho().devolverPeca(peca);
                }
            }

            jogador.adicionarPeca(escolhida);
        }

        AcaoJogoResponse resposta =
                stubDe(nomeVencedor).jogarPeca(
                        JogarPecaRequest.newBuilder()
                                .setCodigoSala(codigoSala)
                                .setTokenJogador(
                                        tokenDe(nomeVencedor)
                                )
                                .setIndicePeca(0)
                                .setLado(LadoJogada.ESQUERDA)
                                .build()
                );

        assertEquals(
                "PARTIDA_ENCERRADA",
                resposta.getResultado()
        );

        System.out.println(
                "[TESTE] Vencedor da rodada: " + nomeVencedor
        );

        return nomeVencedor;
    }

    // AVANÇAR ATÉ A PRIMEIRA LOJA

    private String abrirPrimeiraLoja() {

        String vencedorPrimeira = finalizarRodada();

        EstadoJogoResponse estado = estadoVitor();

        assertEquals(2, estado.getNumeroRodada());

        assertEquals(
                FasePartidaMensagem.EM_ANDAMENTO,
                estado.getFase()
        );

        assertEquals(
                2,
                buscarJogador(
                        estado, vencedorPrimeira
                ).getPontosPetiscos()
        );

        System.out.println(
                "[OK] Vencedor recebeu 2 moedas."
        );

        String vencedorSegunda = finalizarRodada();

        assertEquals(
                vencedorPrimeira,
                vencedorSegunda
        );

        estado = estadoVitor();

        assertEquals(
                FasePartidaMensagem.LOJA_PETISCOS,
                estado.getFase()
        );

        assertEquals(2, estado.getNumeroRodada());

        assertEquals(
                4,
                buscarJogador(
                        estado, vencedorSegunda
                ).getPontosPetiscos()
        );

        assertEquals(3, estado.getPetiscosLojaCount());

        assertTrue(
                estado.getTempoLojaRestanteSegundos() > 0
        );

        assertTrue(
                estado.getTempoLojaRestanteSegundos() <= 30
        );

        System.out.println(
                "[OK] Vencedor acumulou 4 moedas."
        );

        System.out.println(
                "[OK] Loja aberta após a segunda rodada."
        );

        return vencedorSegunda;
    }

    // TESTE 1 - OVO COLORIDO E REGRAS DA LOJA

    @Test
    void deveComprarEUtilizarOvoColorido() throws Exception {

        System.out.println("\n[TESTE] OVO COLORIDO");

        String vencedor = abrirPrimeiraLoja();

        String perdedor = adversarioDe(vencedor);

        // Impede jogadas durante a loja

        StatusRuntimeException erro = assertThrows(
                StatusRuntimeException.class,
                () -> stubDe(vencedor).passarVez(
                        AcaoSimplesRequest.newBuilder()
                                .setCodigoSala(codigoSala)
                                .setTokenJogador(tokenDe(vencedor))
                                .build()
                )
        );

        assertEquals(
                Status.Code.FAILED_PRECONDITION,
                erro.getStatus().getCode()
        );

        System.out.println(
                "[OK] Jogadas bloqueadas durante a loja."
        );

        // Perdedor não recebeu moedas

        assertEquals(
                0,
                buscarJogador(
                        estadoVitor(), perdedor
                ).getPontosPetiscos()
        );

        ComprarPetiscoResponse compraSemSaldo =
                comprarPetisco(
                        perdedor,
                        TipoPetiscoMensagem.OVO_COLORIDO
                );

        assertEquals(
                "SALDO_INSUFICIENTE",
                compraSemSaldo.getResultado()
        );

        System.out.println(
                "[OK] Compra sem saldo recusada."
        );

        // Compra do Ovo Colorido

        ComprarPetiscoResponse compra = comprarPetisco(
                vencedor,
                TipoPetiscoMensagem.OVO_COLORIDO
        );

        assertEquals(
                "COMPRA_REALIZADA",
                compra.getResultado()
        );

        EstadoJogoResponse estado =
                consultarEstado(vencedor);

        assertEquals(
                2,
                buscarJogador(
                        estado, vencedor
                ).getPontosPetiscos()
        );

        assertEquals(
                TipoPetiscoMensagem.OVO_COLORIDO,
                estado.getMeuPetisco()
        );

        assertTrue(estado.getJaCompreiNestaLoja());

        assertFalse(estado.getPossoUtilizarPetisco());

        System.out.println(
                "[OK] Ovo Colorido comprado por 2 moedas."
        );

        // Segunda compra deve ser recusada

        ComprarPetiscoResponse segundaCompra =
                comprarPetisco(
                        vencedor,
                        TipoPetiscoMensagem.OVO_COLORIDO
                );

        assertEquals(
                "JA_COMPROU_NESTA_LOJA",
                segundaCompra.getResultado()
        );

        System.out.println(
                "[OK] Limite de uma compra respeitado."
        );

        // Privacidade do inventário

        assertEquals(
                TipoPetiscoMensagem.PETISCO_NAO_INFORMADO,
                consultarEstado(perdedor).getMeuPetisco()
        );

        System.out.println(
                "[OK] Inventário privado preservado."
        );

        System.out.println(
                "[TESTE] Aguardando fechamento da loja..."
        );

        aguardarFase(
                FasePartidaMensagem.EM_ANDAMENTO,
                35
        );

        estado = consultarEstado(vencedor);

        assertEquals(3, estado.getNumeroRodada());

        assertEquals(7, estado.getMinhaMaoCount());

        assertTrue(estado.getPossoUtilizarPetisco());

        List<PecaMensagem> maoAdversario =
                consultarEstado(perdedor)
                        .getMinhaMaoList();

        // Utiliza o Ovo Colorido

        UtilizarPetiscoResponse uso = utilizarPetisco(
                vencedor,
                TipoPetiscoMensagem.OVO_COLORIDO,
                null
        );

        assertEquals(
                "PETISCO_UTILIZADO",
                uso.getResultado()
        );

        assertTrue(uso.hasPecaRevelada());

        assertEquals(
                5,
                uso.getDuracaoRevelacaoSegundos()
        );

        assertTrue(
                maoAdversario.contains(
                        uso.getPecaRevelada()
                )
        );

        System.out.println(
                "[OK] Peça revelada: "
                        + uso.getPecaRevelada().getLadoA()
                        + "|"
                        + uso.getPecaRevelada().getLadoB()
        );

        System.out.println(
                "[OK] Duração da revelação: 5 segundos."
        );

        // Petisco deve ter sido consumido

        assertEquals(
                TipoPetiscoMensagem.PETISCO_NAO_INFORMADO,
                consultarEstado(vencedor).getMeuPetisco()
        );

        // Não pode utilizar novamente

        UtilizarPetiscoResponse segundoUso =
                utilizarPetisco(
                        vencedor,
                        TipoPetiscoMensagem.OVO_COLORIDO,
                        null
                );

        assertEquals(
                "PETISCO_INDISPONIVEL",
                segundoUso.getResultado()
        );

        System.out.println(
                "[OK] Segunda utilização recusada."
        );

        System.out.println(
                "[SUCESSO] Todos os testes do Ovo Colorido passaram!"
        );
    }

    // TESTE 2 - COXINHA

    @Test
    void deveComprarEUtilizarCoxinha() throws Exception {

        System.out.println("\n[TESTE] COXINHA");

        String vencedor = abrirPrimeiraLoja();

        String adversario = adversarioDe(vencedor);

        ComprarPetiscoResponse compra =
                comprarPetisco(
                        vencedor,
                        TipoPetiscoMensagem.COXINHA
                );

        assertEquals(
                "COMPRA_REALIZADA",
                compra.getResultado()
        );

        assertEquals(
                0,
                buscarJogador(
                        consultarEstado(vencedor),
                        vencedor
                ).getPontosPetiscos()
        );

        System.out.println(
                "[OK] Coxinha comprada por 4 moedas."
        );

        System.out.println(
                "[TESTE] Aguardando fechamento da loja..."
        );

        aguardarFase(
                FasePartidaMensagem.EM_ANDAMENTO,
                35
        );

        EstadoJogoResponse antes =
                consultarEstado(vencedor);

        PecaMensagem pecaAntiga =
                antes.getMinhaMao(0);

        int quantidadeMao = antes.getMinhaMaoCount();

        int quantidadeCava = antes.getPecasBaralho();

        List<PecaMensagem> maoAdversario =
                consultarEstado(adversario)
                        .getMinhaMaoList();

        // Não informou o índice da peça

        UtilizarPetiscoResponse usoInvalido =
                utilizarPetisco(
                        vencedor,
                        TipoPetiscoMensagem.COXINHA,
                        null
                );

        assertEquals(
                "PECA_INVALIDA",
                usoInvalido.getResultado()
        );

        assertEquals(
                TipoPetiscoMensagem.COXINHA,
                consultarEstado(vencedor).getMeuPetisco()
        );

        System.out.println(
                "[OK] Utilização sem índice recusada."
        );

        // Troca a primeira peça

        UtilizarPetiscoResponse uso =
                utilizarPetisco(
                        vencedor,
                        TipoPetiscoMensagem.COXINHA,
                        0
                );

        assertEquals(
                "PETISCO_UTILIZADO",
                uso.getResultado()
        );

        EstadoJogoResponse depois =
                consultarEstado(vencedor);

        PecaMensagem pecaNova =
                depois.getMinhaMao(0);

        assertNotEquals(pecaAntiga, pecaNova);

        assertEquals(
                quantidadeMao,
                depois.getMinhaMaoCount()
        );

        assertEquals(
                quantidadeCava,
                depois.getPecasBaralho()
        );

        assertEquals(
                maoAdversario,
                consultarEstado(adversario)
                        .getMinhaMaoList()
        );

        assertEquals(
                TipoPetiscoMensagem.PETISCO_NAO_INFORMADO,
                depois.getMeuPetisco()
        );

        assertFalse(uso.hasPecaRevelada());

        System.out.println(
                "[OK] Peça antiga: "
                        + pecaAntiga.getLadoA()
                        + "|"
                        + pecaAntiga.getLadoB()
        );

        System.out.println(
                "[OK] Peça nova: "
                        + pecaNova.getLadoA()
                        + "|"
                        + pecaNova.getLadoB()
        );

        System.out.println(
                "[OK] Quantidade da mão preservada."
        );

        System.out.println(
                "[OK] Quantidade da cava preservada."
        );

        System.out.println(
                "[OK] Mão do adversário preservada."
        );

        System.out.println(
                "[SUCESSO] Todos os testes da Coxinha passaram!"
        );
    }

    // TESTE 3 - TORRESMO

    @Test
    void deveComprarEUtilizarTorresmo() throws Exception {

        System.out.println("\n[TESTE] TORRESMO");

        String vencedor = abrirPrimeiraLoja();

        String adversario = adversarioDe(vencedor);

        ComprarPetiscoResponse compra =
                comprarPetisco(
                        vencedor,
                        TipoPetiscoMensagem.TORRESMO
                );

        assertEquals(
                "COMPRA_REALIZADA",
                compra.getResultado()
        );

        assertEquals(
                0,
                buscarJogador(
                        consultarEstado(vencedor),
                        vencedor
                ).getPontosPetiscos()
        );

        System.out.println(
                "[OK] Torresmo comprado por 4 moedas."
        );

        System.out.println(
                "[TESTE] Aguardando fechamento da loja..."
        );

        aguardarFase(
                FasePartidaMensagem.PREPARACAO_RODADA,
                35
        );

        EstadoJogoResponse antes =
                consultarEstado(vencedor);

        assertEquals(3, antes.getNumeroRodada());

        assertTrue(
                antes.getTempoPreparacaoRestanteSegundos() > 0
        );

        assertFalse(antes.getEstouPreparado());

        assertTrue(
                consultarEstado(adversario)
                        .getEstouPreparado()
        );

        assertTrue(antes.getPossoUtilizarPetisco());

        int quantidadeCava = antes.getPecasBaralho();

        List<PecaMensagem> maoAdversario =
                consultarEstado(adversario)
                        .getMinhaMaoList();

        System.out.println(
                "[OK] Fase de preparação iniciada."
        );

        // Não pode jogar durante a preparação

        StatusRuntimeException erro = assertThrows(
                StatusRuntimeException.class,
                () -> stubDe(vencedor).jogarPeca(
                        JogarPecaRequest.newBuilder()
                                .setCodigoSala(codigoSala)
                                .setTokenJogador(
                                        tokenDe(vencedor)
                                )
                                .setIndicePeca(0)
                                .setLado(LadoJogada.ESQUERDA)
                                .build()
                )
        );

        assertEquals(
                Status.Code.FAILED_PRECONDITION,
                erro.getStatus().getCode()
        );

        System.out.println(
                "[OK] Jogadas bloqueadas durante a preparação."
        );

        System.out.println(
                "[TESTE] Mão antes do Torresmo: "
                        + formatarMao(antes.getMinhaMaoList())
        );

        UtilizarPetiscoResponse uso =
                utilizarPetisco(
                        vencedor,
                        TipoPetiscoMensagem.TORRESMO,
                        null
                );

        assertEquals(
                "PETISCO_UTILIZADO",
                uso.getResultado()
        );

        EstadoJogoResponse depois =
                consultarEstado(vencedor);

        assertEquals(
                FasePartidaMensagem.EM_ANDAMENTO,
                depois.getFase()
        );

        assertEquals(7, depois.getMinhaMaoCount());

        assertEquals(
                quantidadeCava,
                depois.getPecasBaralho()
        );

        assertEquals(0, depois.getPecasMesaCount());

        assertEquals(
                maoAdversario,
                consultarEstado(adversario)
                        .getMinhaMaoList()
        );

        assertEquals(
                TipoPetiscoMensagem.PETISCO_NAO_INFORMADO,
                depois.getMeuPetisco()
        );

        assertTrue(depois.getEstouPreparado());

        assertFalse(uso.hasPecaRevelada());

        System.out.println(
                "[TESTE] Mão depois do Torresmo: "
                        + formatarMao(depois.getMinhaMaoList())
        );

        System.out.println(
                "[OK] Torresmo utilizado."
        );

        System.out.println(
                "[OK] Quantidade de peças preservada."
        );

        System.out.println(
                "[OK] Adversário não foi afetado."
        );

        System.out.println(
                "[OK] Rodada iniciada automaticamente."
        );

        System.out.println(
                "[SUCESSO] Todos os testes do Torresmo passaram!"
        );
    }

    // TESTE 4 - TEMPORIZADOR DA PREPARAÇÃO

    @Test
    void deveEncerrarPreparacaoAutomaticamente()
            throws Exception {

        System.out.println(
                "\n[TESTE] TEMPORIZADOR DA PREPARAÇÃO"
        );

        String vencedor = abrirPrimeiraLoja();

        assertEquals(
                "COMPRA_REALIZADA",
                comprarPetisco(
                        vencedor,
                        TipoPetiscoMensagem.TORRESMO
                ).getResultado()
        );

        aguardarFase(
                FasePartidaMensagem.PREPARACAO_RODADA,
                35
        );

        System.out.println(
                "[TESTE] Aguardando 15 segundos de preparação..."
        );

        aguardarFase(
                FasePartidaMensagem.EM_ANDAMENTO,
                20
        );

        EstadoJogoResponse estado =
                consultarEstado(vencedor);

        assertEquals(3, estado.getNumeroRodada());

        assertTrue(estado.getEstouPreparado());

        assertFalse(estado.getPossoUtilizarPetisco());

        UtilizarPetiscoResponse uso =
                utilizarPetisco(
                        vencedor,
                        TipoPetiscoMensagem.TORRESMO,
                        null
                );

        assertEquals(
                "FASE_INVALIDA",
                uso.getResultado()
        );

        System.out.println(
                "[OK] Preparação encerrada automaticamente."
        );

        System.out.println(
                "[OK] Torresmo bloqueado após o prazo."
        );

        System.out.println(
                "[SUCESSO] Temporizador da preparação funcionando!"
        );
    }

    // TESTE 5 - CONFIRMAÇÃO DA PREPARAÇÃO

    @Test
    void deveConfirmarPreparacaoSemUsarTorresmo()
            throws Exception {

        System.out.println(
                "\n[TESTE] CONFIRMAÇÃO DA PREPARAÇÃO"
        );

        String vencedor = abrirPrimeiraLoja();

        assertEquals(
                "COMPRA_REALIZADA",
                comprarPetisco(
                        vencedor,
                        TipoPetiscoMensagem.TORRESMO
                ).getResultado()
        );

        aguardarFase(
                FasePartidaMensagem.PREPARACAO_RODADA,
                35
        );

        ConfirmarPreparacaoResponse resposta =
                stubDe(vencedor).confirmarPreparacao(
                        AcaoSimplesRequest.newBuilder()
                                .setCodigoSala(codigoSala)
                                .setTokenJogador(
                                        tokenDe(vencedor)
                                )
                                .build()
                );

        assertTrue(resposta.getConfirmado());

        EstadoJogoResponse estado =
                consultarEstado(vencedor);

        assertEquals(
                FasePartidaMensagem.EM_ANDAMENTO,
                estado.getFase()
        );

        assertFalse(estado.getPossoUtilizarPetisco());

        assertTrue(estado.getEstouPreparado());

        System.out.println(
                "[OK] Preparação confirmada sem utilizar Torresmo."
        );

        System.out.println(
                "[OK] Rodada iniciada sem aguardar o temporizador."
        );

        System.out.println(
                "[SUCESSO] Confirmação da preparação funcionando!"
        );
    }

    // TESTE 6 - DESCONEXÃO E RECONEXÃO DURANTE A LOJA

    @Test
    void deveReconectarDuranteLoja() throws Exception {

        System.out.println(
                "\n[TESTE] RECONEXÃO DURANTE A LOJA"
        );

        ObservadorEstado observadorVitor =
                new ObservadorEstado();

        ObservadorEstado observadorYago =
                new ObservadorEstado();

        ObservadorEstado reconexao =
                new ObservadorEstado();

        try {

            observadorVitor.iniciar(
                    canalVitor,
                    criarRequest(tokenVitor)
            );

            observadorYago.iniciar(
                    canalYago,
                    criarRequest(tokenYago)
            );

            observadorVitor.aguardar(
                    estado -> estado.getJogadoresList()
                            .stream()
                            .allMatch(JogadorMensagem::getConectado),
                    5
            );

            observadorYago.aguardar(
                    estado -> estado.getJogadoresList()
                            .stream()
                            .allMatch(JogadorMensagem::getConectado),
                    5
            );

            System.out.println(
                    "[OK] Ambos os jogadores conectados."
            );

            String vencedor = abrirPrimeiraLoja();

            ObservadorEstado observadorVencedor =
                    vencedor.equals("Vitor")
                            ? observadorVitor
                            : observadorYago;

            ObservadorEstado observadorAdversario =
                    vencedor.equals("Vitor")
                            ? observadorYago
                            : observadorVitor;

            EstadoJogoResponse loja =
                    observadorVencedor.aguardar(
                            estado -> estado.getFase()
                                    == FasePartidaMensagem.LOJA_PETISCOS,
                            5
                    );

            assertEquals(3, loja.getPetiscosLojaCount());

            assertEquals(
                    "COMPRA_REALIZADA",
                    comprarPetisco(
                            vencedor,
                            TipoPetiscoMensagem.OVO_COLORIDO
                    ).getResultado()
            );

            observadorVencedor.aguardar(
                    EstadoJogoResponse::getJaCompreiNestaLoja,
                    5
            );

            System.out.println(
                    "[OK] Compra recebida pelo streaming."
            );

            // Desconecta o jogador que comprou

            observadorVencedor.cancelar();

            observadorAdversario.aguardar(
                    estado -> estado.getFase()
                            == FasePartidaMensagem.LOJA_PETISCOS
                            && !buscarJogador(
                            estado, vencedor
                    ).getConectado(),
                    5
            );

            System.out.println(
                    "[OK] Desconexão identificada."
            );

            ManagedChannel canalReconexao =
                    vencedor.equals("Vitor")
                            ? canalVitor
                            : canalYago;

            reconexao.iniciar(
                    canalReconexao,
                    criarRequest(tokenDe(vencedor))
            );

            EstadoJogoResponse recuperado =
                    reconexao.aguardar(
                            estado -> estado.getFase()
                                    == FasePartidaMensagem.LOJA_PETISCOS
                                    && estado.getJaCompreiNestaLoja()
                                    && buscarJogador(
                                    estado, vencedor
                            ).getConectado(),
                            5
                    );

            assertEquals(
                    TipoPetiscoMensagem.OVO_COLORIDO,
                    recuperado.getMeuPetisco()
            );

            assertEquals(
                    2,
                    buscarJogador(
                            recuperado, vencedor
                    ).getPontosPetiscos()
            );

            assertTrue(
                    recuperado.getTempoLojaRestanteSegundos() > 0
            );

            assertTrue(
                    recuperado.getTempoLojaRestanteSegundos()
                            <= loja.getTempoLojaRestanteSegundos()
            );

            observadorAdversario.aguardar(
                    estado -> buscarJogador(
                            estado, vencedor
                    ).getConectado(),
                    5
            );

            System.out.println(
                    "[OK] Reconexão realizada."
            );

            System.out.println(
                    "[OK] Saldo e petisco preservados."
            );

            System.out.println(
                    "[OK] Temporizador da loja preservado."
            );

            // A loja deve fechar mesmo após a reconexão

            EstadoJogoResponse novaRodada =
                    reconexao.aguardar(
                            estado -> estado.getFase()
                                    == FasePartidaMensagem.EM_ANDAMENTO
                                    && estado.getNumeroRodada() == 3,
                            35
                    );

            assertEquals(
                    7,
                    novaRodada.getMinhaMaoCount()
            );

            EstadoJogoResponse estadoAdversario =
                    observadorAdversario.aguardar(
                            estado -> estado.getFase()
                                    == FasePartidaMensagem.EM_ANDAMENTO
                                    && estado.getNumeroRodada() == 3,
                            5
                    );

            assertEquals(
                    7,
                    estadoAdversario.getMinhaMaoCount()
            );

            // As mãos não devem compartilhar peças

            for (PecaMensagem peca :
                    novaRodada.getMinhaMaoList()) {

                assertFalse(
                        estadoAdversario.getMinhaMaoList()
                                .contains(peca)
                );
            }

            assertEquals(
                    TipoPetiscoMensagem.PETISCO_NAO_INFORMADO,
                    estadoAdversario.getMeuPetisco()
            );

            assertEquals(
                    TipoPetiscoMensagem.OVO_COLORIDO,
                    novaRodada.getMeuPetisco()
            );

            System.out.println(
                    "[OK] Nova rodada recebida pelos dois clientes."
            );

            System.out.println(
                    "[OK] Privacidade das mãos preservada."
            );

            System.out.println(
                    "[SUCESSO] Reconexão durante a loja funcionando!"
            );

        } finally {

            observadorVitor.cancelar();
            observadorYago.cancelar();
            reconexao.cancelar();
        }
    }

    // FORMATAÇÃO DAS PEÇAS PARA EXIBIÇÃO NO CONSOLE

    private String formatarMao(List<PecaMensagem> mao) {

        StringBuilder texto = new StringBuilder();

        for (PecaMensagem peca : mao) {

            texto.append("[")
                    .append(peca.getLadoA())
                    .append("|")
                    .append(peca.getLadoB())
                    .append("] ");
        }

        return texto.toString().trim();
    }

    // OBSERVADOR DO STREAMING

    private static class ObservadorEstado
            implements StreamObserver<EstadoJogoResponse> {

        private final BlockingQueue<EstadoJogoResponse> estados =
                new LinkedBlockingQueue<>();

        private Context.CancellableContext contexto;

        public void iniciar(
                ManagedChannel canal,
                EstadoJogoRequest request) {

            contexto = Context.current().withCancellation();

            contexto.run(() ->
                    DesceUmaServiceGrpc.newStub(canal)
                            .acompanharJogo(request, this)
            );
        }

        @Override
        public void onNext(EstadoJogoResponse estado) {

            estados.offer(estado);
        }

        @Override
        public void onError(Throwable throwable) {

            System.out.println(
                    "[STREAM] " + throwable.getMessage()
            );
        }

        @Override
        public void onCompleted() {

            System.out.println("[STREAM] Encerrado.");
        }

        public void cancelar() {

            if (contexto != null) {
                contexto.cancel(null);
            }
        }

        public EstadoJogoResponse aguardar(
                Predicate<EstadoJogoResponse> condicao,
                int segundos) throws InterruptedException {

            long prazo = System.nanoTime()
                    + TimeUnit.SECONDS.toNanos(segundos);

            while (true) {

                long restante = prazo - System.nanoTime();

                if (restante <= 0) {
                    break;
                }

                EstadoJogoResponse estado = estados.poll(
                        restante,
                        TimeUnit.NANOSECONDS
                );

                if (estado == null) {
                    break;
                }

                if (condicao.test(estado)) {
                    return estado;
                }
            }

            fail(
                    "Estado esperado não recebido pelo streaming."
            );

            return null;
        }
    }
}
