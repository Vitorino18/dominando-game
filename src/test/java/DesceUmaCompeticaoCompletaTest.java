import br.com.dominando.grpc.*;
import br.com.dominando.model.*;
import br.com.dominando.server.*;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class DesceUmaCompeticaoCompletaTest {

    private Server servidor;

    private ManagedChannel canalVitor;
    private ManagedChannel canalYago;

    private DesceUmaServiceGrpc.DesceUmaServiceBlockingStub stubVitor;
    private DesceUmaServiceGrpc.DesceUmaServiceBlockingStub stubYago;

    private SalaService salaService;

    private String codigoSala;
    private String tokenVitor;
    private String tokenYago;

    // CONFIGURAÇÃO

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

        CriarSalaResponse criacao = stubVitor
                .withDeadlineAfter(5, TimeUnit.SECONDS)
                .criarSala(
                        CriarSalaRequest.newBuilder()
                                .setNomeCriador("Vitor")
                                .build()
                );

        codigoSala = criacao.getCodigoSala();
        tokenVitor = criacao.getTokenJogador();

        EntrarSalaResponse entrada = stubYago
                .withDeadlineAfter(5, TimeUnit.SECONDS)
                .entrarSala(
                        EntrarSalaRequest.newBuilder()
                                .setCodigoSala(codigoSala)
                                .setNomeJogador("Yago")
                                .build()
                );

        tokenYago = entrada.getTokenJogador();

        IniciarPartidaResponse inicio = stubVitor
                .withDeadlineAfter(5, TimeUnit.SECONDS)
                .iniciarPartida(
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

    private Sala getSala() {

        Sala sala = salaService.buscarSala(codigoSala);

        assertNotNull(sala);

        return sala;
    }

    private String tokenDe(String nome) {

        return nome.equals("Vitor")
                ? tokenVitor
                : tokenYago;
    }

    private String adversarioDe(String nome) {

        return nome.equals("Vitor")
                ? "Yago"
                : "Vitor";
    }

    private DesceUmaServiceGrpc.DesceUmaServiceBlockingStub
    stubDe(String nome) {

        return (nome.equals("Vitor") ? stubVitor : stubYago)
                .withDeadlineAfter(5, TimeUnit.SECONDS);
    }

    private EstadoJogoResponse consultarEstado(String nome) {

        return stubDe(nome).consultarEstadoJogo(
                EstadoJogoRequest.newBuilder()
                        .setCodigoSala(codigoSala)
                        .setTokenJogador(tokenDe(nome))
                        .build()
        );
    }

    private EstadoJogoResponse estado() {

        return consultarEstado("Vitor");
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

    private int saldo(String nome) {

        return buscarJogador(
                estado(), nome
        ).getPontosPetiscos();
    }

    private int vitorias(String nome) {

        return buscarJogador(
                estado(), nome
        ).getVitorias();
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
            TipoPetiscoMensagem tipo) {

        return stubDe(nome).utilizarPetisco(
                UtilizarPetiscoRequest.newBuilder()
                        .setCodigoSala(codigoSala)
                        .setTokenJogador(tokenDe(nome))
                        .setTipo(tipo)
                        .build()
        );
    }

    private AcaoSimplesRequest acaoSimples(String nome) {

        return AcaoSimplesRequest.newBuilder()
                .setCodigoSala(codigoSala)
                .setTokenJogador(tokenDe(nome))
                .build();
    }

    private EstadoJogoResponse aguardarFase(
            FasePartidaMensagem faseEsperada,
            int limiteSegundos) throws InterruptedException {

        long prazo = System.nanoTime()
                + TimeUnit.SECONDS.toNanos(limiteSegundos);

        while (System.nanoTime() - prazo < 0) {

            EstadoJogoResponse atual = estado();

            if (atual.getFase() == faseEsperada) {
                return atual;
            }

            Thread.sleep(100);
        }

        fail("Tempo excedido aguardando fase: "
                + faseEsperada);

        return null;
    }

    private void aguardarFechamentoLoja()
            throws InterruptedException {

        System.out.println(
                "[TESTE] Aguardando fechamento da loja..."
        );

        aguardarFase(
                FasePartidaMensagem.EM_ANDAMENTO,
                35
        );
    }

    // PREPARAÇÃO CONTROLADA DAS RODADAS

    private Peca escolherPeca(Jogador jogador) {

        return jogador.getMao()
                .stream()
                .filter(Peca::bucha)
                .max(Comparator.comparingInt(Peca::getLadoA))
                .orElseGet(() -> jogador.getMao()
                        .stream()
                        .max(Comparator.comparingInt(
                                p -> p.getLadoA() + p.getLadoB()
                        ))
                        .orElseThrow()
                );
    }

    private void deixarUmaPeca(
            Partida partida,
            Jogador jogador,
            Peca escolhida) {

        List<Peca> mao = List.copyOf(jogador.getMao());

        jogador.limparMao();

        for (Peca peca : mao) {

            if (peca != escolhida) {
                partida.getBaralho().devolverPeca(peca);
            }
        }

        jogador.adicionarPeca(escolhida);
    }

    private AcaoJogoResponse jogarPrimeiraPeca(String nome) {

        return stubDe(nome).jogarPeca(
                JogarPecaRequest.newBuilder()
                        .setCodigoSala(codigoSala)
                        .setTokenJogador(tokenDe(nome))
                        .setIndicePeca(0)
                        .setLado(LadoJogada.ESQUERDA)
                        .build()
        );
    }

    private String finalizarRodada() {

        Sala sala = getSala();

        String vencedor;

        synchronized (sala) {

            Partida partida = sala.getPartidaGeral()
                    .getPartidaAtual();

            assertTrue(
                    partida.getMesa().getPecasMesa().isEmpty()
            );

            Jogador jogador = partida.getJogadorVez();

            vencedor = jogador.getNome();

            Peca escolhida = escolherPeca(jogador);

            deixarUmaPeca(partida, jogador, escolhida);
        }

        AcaoJogoResponse resposta =
                jogarPrimeiraPeca(vencedor);

        assertEquals(
                "PARTIDA_ENCERRADA",
                resposta.getResultado()
        );

        System.out.println(
                "[TESTE] Vencedor da rodada: " + vencedor
        );

        return vencedor;
    }

    /*
     * Permite escolher o vencedor a partir da segunda
     * rodada, preparando uma sequência de duas jogadas
     * compatíveis.
     *
     * Não modifica o código de produção.
     */

    private void finalizarRodadaComVencedor(String vencedor) {

        Sala sala = getSala();

        String jogadorInicial = estado().getJogadorDaVez();

        if (jogadorInicial.equals(vencedor)) {

            assertEquals(vencedor, finalizarRodada());

            return;
        }

        synchronized (sala) {

            Partida partida = sala.getPartidaGeral()
                    .getPartidaAtual();

            assertTrue(
                    partida.getMesa().getPecasMesa().isEmpty()
            );

            assertTrue(
                    sala.getPartidaGeral().getNumeroRodada() > 1
            );

            Jogador inicial = partida.getJogadorVez();

            Jogador desejado = partida.getJogadores()
                    .stream()
                    .filter(j -> j.getNome().equals(vencedor))
                    .findFirst()
                    .orElseThrow();

            // Reúne as 28 peças para preparar o cenário.

            List<Peca> todas = new ArrayList<>();

            for (Jogador jogador : partida.getJogadores()) {

                todas.addAll(jogador.retirarTodasPecas());
            }

            while (partida.getBaralho().quantidadePecas() > 0) {

                todas.add(
                        partida.getBaralho().comprarPeca()
                );
            }

            assertEquals(28, todas.size());

            Peca primeira = retirarPeca(todas, 6, 6);
            Peca reserva = retirarPeca(todas, 0, 0);
            Peca ultima = retirarPeca(todas, 6, 5);

            inicial.adicionarPeca(primeira);
            inicial.adicionarPeca(reserva);

            desejado.adicionarPeca(ultima);

            partida.getBaralho().devolverPecas(todas);
        }

        AcaoJogoResponse primeira =
                jogarPrimeiraPeca(jogadorInicial);

        assertEquals(
                "JOGADA_VALIDA",
                primeira.getResultado()
        );

        AcaoJogoResponse ultima =
                jogarPrimeiraPeca(vencedor);

        assertEquals(
                "PARTIDA_ENCERRADA",
                ultima.getResultado()
        );

        System.out.println(
                "[TESTE] Vencedor da rodada: " + vencedor
        );
    }

    private Peca retirarPeca(
            List<Peca> pecas,
            int ladoA,
            int ladoB) {

        for (int i = 0; i < pecas.size(); i++) {

            Peca peca = pecas.get(i);

            boolean corresponde =
                    (peca.getLadoA() == ladoA
                            && peca.getLadoB() == ladoB)
                            ||
                            (peca.getLadoA() == ladoB
                                    && peca.getLadoB() == ladoA);

            if (corresponde) {

                return pecas.remove(i);
            }
        }

        throw new IllegalStateException(
                "Peça não encontrada: " + ladoA + "|" + ladoB
        );
    }

    private String abrirPrimeiraLoja() {

        String vencedor = finalizarRodada();

        assertEquals(2, saldo(vencedor));
        assertEquals(1, vitorias(vencedor));

        finalizarRodadaComVencedor(vencedor);

        assertEquals(4, saldo(vencedor));
        assertEquals(2, vitorias(vencedor));

        assertEquals(
                FasePartidaMensagem.LOJA_PETISCOS,
                estado().getFase()
        );

        assertEquals(2, estado().getNumeroRodada());

        System.out.println(
                "[OK] Primeira loja aberta."
        );

        return vencedor;
    }

    // TESTE 1 - EXPIRAÇÃO E COMPRAS CONSECUTIVAS

    @Test
    void deveExpirarPetiscoEBloquearCompraConsecutiva()
            throws Exception {

        System.out.println(
                "\n[TESTE] EXPIRAÇÃO E COMPRAS CONSECUTIVAS"
        );

        String vencedor = abrirPrimeiraLoja();

        assertEquals(
                "COMPRA_REALIZADA",
                comprarPetisco(
                        vencedor,
                        TipoPetiscoMensagem.OVO_COLORIDO
                ).getResultado()
        );

        aguardarFechamentoLoja();

        assertEquals(3, estado().getNumeroRodada());

        assertEquals(
                TipoPetiscoMensagem.OVO_COLORIDO,
                consultarEstado(vencedor).getMeuPetisco()
        );

        // Finaliza a rodada 3 sem utilizar o petisco.

        finalizarRodadaComVencedor(vencedor);

        assertEquals(4, estado().getNumeroRodada());

        assertEquals(
                TipoPetiscoMensagem.PETISCO_NAO_INFORMADO,
                consultarEstado(vencedor).getMeuPetisco()
        );

        assertFalse(
                consultarEstado(vencedor)
                        .getPossoUtilizarPetisco()
        );

        assertEquals(
                "PETISCO_INDISPONIVEL",
                utilizarPetisco(
                        vencedor,
                        TipoPetiscoMensagem.OVO_COLORIDO
                ).getResultado()
        );

        System.out.println(
                "[OK] Petisco expirou após a rodada 3."
        );

        // Finaliza a rodada 4.

        finalizarRodadaComVencedor(vencedor);

        assertEquals(
                FasePartidaMensagem.LOJA_PETISCOS,
                estado().getFase()
        );

        assertEquals(4, estado().getNumeroRodada());

        assertEquals(6, saldo(vencedor));

        ComprarPetiscoResponse repetido =
                comprarPetisco(
                        vencedor,
                        TipoPetiscoMensagem.OVO_COLORIDO
                );

        assertEquals(
                "PETISCO_REPETIDO",
                repetido.getResultado()
        );

        assertEquals(6, saldo(vencedor));

        System.out.println(
                "[OK] Compra repetida recusada."
        );

        // Um petisco diferente deve ser permitido.

        ComprarPetiscoResponse diferente =
                comprarPetisco(
                        vencedor,
                        TipoPetiscoMensagem.COXINHA
                );

        assertEquals(
                "COMPRA_REALIZADA",
                diferente.getResultado()
        );

        assertEquals(2, saldo(vencedor));

        System.out.println(
                "[OK] Compra de petisco diferente permitida."
        );

        aguardarFechamentoLoja();

        assertEquals(5, estado().getNumeroRodada());

        System.out.println(
                "[SUCESSO] Expiração e restrições funcionando!"
        );
    }

    // TESTE 2 - PULAR UMA LOJA

    @Test
    void devePermitirMesmoPetiscoDepoisDePularUmaLoja()
            throws Exception {

        System.out.println(
                "\n[TESTE] PULAR UMA LOJA"
        );

        String vencedor = abrirPrimeiraLoja();

        String adversario = adversarioDe(vencedor);

        assertEquals(
                "COMPRA_REALIZADA",
                comprarPetisco(
                        vencedor,
                        TipoPetiscoMensagem.OVO_COLORIDO
                ).getResultado()
        );

        aguardarFechamentoLoja();

        // Rodadas 3 e 4 vencidas pelo adversário.

        finalizarRodadaComVencedor(adversario);
        finalizarRodadaComVencedor(adversario);

        assertEquals(4, estado().getNumeroRodada());

        assertEquals(
                FasePartidaMensagem.LOJA_PETISCOS,
                estado().getFase()
        );

        assertEquals(2, vitorias(vencedor));
        assertEquals(2, vitorias(adversario));

        // Não compra nada na segunda loja.

        System.out.println(
                "[TESTE] Pulando a loja da rodada 4."
        );

        aguardarFechamentoLoja();

        // Rodada 5 vencida pelo comprador original.

        finalizarRodadaComVencedor(vencedor);

        // Rodada 6 vencida pelo adversário.

        finalizarRodadaComVencedor(adversario);

        assertEquals(6, estado().getNumeroRodada());

        assertEquals(
                FasePartidaMensagem.LOJA_PETISCOS,
                estado().getFase()
        );

        assertEquals(3, vitorias(vencedor));
        assertEquals(3, vitorias(adversario));

        assertEquals(4, saldo(vencedor));

        ComprarPetiscoResponse compra =
                comprarPetisco(
                        vencedor,
                        TipoPetiscoMensagem.OVO_COLORIDO
                );

        assertEquals(
                "COMPRA_REALIZADA",
                compra.getResultado()
        );

        assertEquals(2, saldo(vencedor));

        System.out.println(
                "[OK] Mesmo petisco comprado novamente."
        );

        aguardarFechamentoLoja();

        assertEquals(7, estado().getNumeroRodada());

        System.out.println(
                "[SUCESSO] Restrição reiniciada após pular loja!"
        );
    }

    // PREPARAR UM EMPATE CONTROLADO

    private void prepararEmpate() {

        Sala sala = getSala();

        synchronized (sala) {

            Partida partida = sala.getPartidaGeral()
                    .getPartidaAtual();

            List<Peca> todas = new ArrayList<>();

            for (Jogador jogador : partida.getJogadores()) {

                todas.addAll(jogador.retirarTodasPecas());
            }

            while (partida.getBaralho().quantidadePecas() > 0) {

                todas.add(
                        partida.getBaralho().comprarPeca()
                );
            }

            assertEquals(28, todas.size());

            assertTrue(
                    partida.getMesa().getPecasMesa().isEmpty()
            );

            Jogador primeiro = partida.getJogadores().get(0);
            Jogador segundo = partida.getJogadores().get(1);

            // As duas mãos possuem soma igual a 4.

            primeiro.adicionarPeca(
                    retirarPeca(todas, 4, 0)
            );

            segundo.adicionarPeca(
                    retirarPeca(todas, 3, 1)
            );

            // Cria uma mesa com as duas pontas iguais a 6.

            Peca mesa = retirarPeca(todas, 6, 6);

            MesaServiceParaTeste.colocarPeca(partida, mesa);

            /*
             * As peças restantes ficam fora do cenário
             * artificial para simular a cava esgotada.
             * Uma nova Partida restaurará as 28 peças.
             */

            assertEquals(
                    0,
                    partida.getBaralho().quantidadePecas()
            );

            assertEquals(
                    primeiro.valorTotalMao(),
                    segundo.valorTotalMao()
            );

            assertTrue(partida.rodadaTravada());
        }
    }

    /*
     * Reaproveita o serviço de mesa do projeto para
     * preparar o cenário artificial de empate.
     */

    private static class MesaServiceParaTeste {

        static void colocarPeca(Partida partida, Peca peca) {

            br.com.dominando.service.MesaService mesaService =
                    new br.com.dominando.service.MesaService(
                            partida.getMesa()
                    );

            mesaService.jogarPeca(
                    peca,
                    LadoMesa.ESQUERDA
            );
        }
    }

    // TESTE 3 - EMPATE E PRESERVAÇÃO DO PETISCO

    @Test
    void deveRepetirRodadaEmpatadaEPreservarPetisco()
            throws Exception {

        System.out.println(
                "\n[TESTE] EMPATE E PRESERVAÇÃO DO PETISCO"
        );

        String vencedor = abrirPrimeiraLoja();

        String adversario = adversarioDe(vencedor);

        assertEquals(
                "COMPRA_REALIZADA",
                comprarPetisco(
                        vencedor,
                        TipoPetiscoMensagem.OVO_COLORIDO
                ).getResultado()
        );

        aguardarFechamentoLoja();

        assertEquals(3, estado().getNumeroRodada());

        int saldoVencedorAntes = saldo(vencedor);
        int saldoAdversarioAntes = saldo(adversario);

        int vitoriasVencedorAntes = vitorias(vencedor);
        int vitoriasAdversarioAntes = vitorias(adversario);

        Partida partidaAnterior =
                getSala().getPartidaGeral().getPartidaAtual();

        prepararEmpate();

        String jogadorDaVez = estado().getJogadorDaVez();

        // O passe pelo gRPC dispara a verificação
        // de encerramento da rodada travada.

        AcaoJogoResponse resposta =
                stubDe(jogadorDaVez).passarVez(
                        acaoSimples(jogadorDaVez)
                );

        assertEquals(
                "VEZ_PASSADA",
                resposta.getResultado()
        );

        EstadoJogoResponse depois = estado();

        assertEquals(3, depois.getNumeroRodada());

        assertEquals(
                FasePartidaMensagem.EM_ANDAMENTO,
                depois.getFase()
        );

        assertNotSame(
                partidaAnterior,
                getSala().getPartidaGeral().getPartidaAtual()
        );

        assertEquals(
                saldoVencedorAntes,
                saldo(vencedor)
        );

        assertEquals(
                saldoAdversarioAntes,
                saldo(adversario)
        );

        assertEquals(
                vitoriasVencedorAntes,
                vitorias(vencedor)
        );

        assertEquals(
                vitoriasAdversarioAntes,
                vitorias(adversario)
        );

        assertEquals(
                TipoPetiscoMensagem.OVO_COLORIDO,
                consultarEstado(vencedor).getMeuPetisco()
        );

        assertEquals(
                7,
                consultarEstado(vencedor).getMinhaMaoCount()
        );

        assertEquals(
                7,
                consultarEstado(adversario).getMinhaMaoCount()
        );

        assertEquals(0, depois.getPecasMesaCount());
        assertEquals(14, depois.getPecasBaralho());

        System.out.println(
                "[OK] Número da rodada preservado."
        );

        System.out.println(
                "[OK] Nenhuma moeda distribuída."
        );

        System.out.println(
                "[OK] Nenhuma tampinha distribuída."
        );

        System.out.println(
                "[OK] Petisco preservado após empate."
        );

        System.out.println(
                "[OK] Peças redistribuídas corretamente."
        );

        // Confirma que o petisco ainda pode ser utilizado.

        assertTrue(
                consultarEstado(vencedor)
                        .getPossoUtilizarPetisco()
        );

        UtilizarPetiscoResponse uso =
                utilizarPetisco(
                        vencedor,
                        TipoPetiscoMensagem.OVO_COLORIDO
                );

        assertEquals(
                "PETISCO_UTILIZADO",
                uso.getResultado()
        );

        assertTrue(uso.hasPecaRevelada());

        System.out.println(
                "[SUCESSO] Empate tratado corretamente!"
        );
    }

    // TESTE 4 - COMPETIÇÃO COMPLETA DE 9 RODADAS

    @Test
    void deveAbrirTodasAsLojasEEncerrarCompeticao()
            throws Exception {

        System.out.println(
                "\n[TESTE] COMPETIÇÃO COMPLETA"
        );

        List<Integer> lojasAbertas = new ArrayList<>();

        String primeiroVencedor = finalizarRodada();

        String segundoVencedor =
                adversarioDe(primeiroVencedor);

        assertEquals(2, estado().getNumeroRodada());

        /*
         * Alterna os vencedores:
         *
         * Rodada 1: primeiro jogador
         * Rodada 2: segundo jogador
         * Rodada 3: primeiro jogador
         * Rodada 4: segundo jogador
         * Rodada 5: primeiro jogador
         * Rodada 6: segundo jogador
         * Rodada 7: primeiro jogador
         * Rodada 8: segundo jogador
         * Rodada 9: primeiro jogador
         */

        for (int rodada = 2; rodada <= 9; rodada++) {

            assertEquals(
                    rodada,
                    estado().getNumeroRodada()
            );

            String vencedor = rodada % 2 == 0
                    ? segundoVencedor
                    : primeiroVencedor;

            finalizarRodadaComVencedor(vencedor);

            EstadoJogoResponse atual = estado();

            if (rodada == 9) {

                assertTrue(atual.getCompeticaoEncerrada());

                assertEquals(
                        FasePartidaMensagem.ENCERRADA,
                        atual.getFase()
                );

                assertEquals(
                        primeiroVencedor,
                        atual.getCampeao()
                );

                break;
            }

            if (rodada % 2 == 0) {

                assertEquals(
                        FasePartidaMensagem.LOJA_PETISCOS,
                        atual.getFase()
                );

                assertEquals(
                        rodada,
                        atual.getNumeroRodada()
                );

                assertEquals(
                        3,
                        atual.getPetiscosLojaCount()
                );

                lojasAbertas.add(rodada);

                System.out.println(
                        "[OK] Loja aberta após rodada " + rodada
                );

                aguardarFechamentoLoja();

                assertEquals(
                        rodada + 1,
                        estado().getNumeroRodada()
                );

            } else {

                assertEquals(
                        FasePartidaMensagem.EM_ANDAMENTO,
                        atual.getFase()
                );

                assertEquals(
                        rodada + 1,
                        atual.getNumeroRodada()
                );

                assertEquals(
                        0,
                        atual.getPetiscosLojaCount()
                );

                System.out.println(
                        "[OK] Nenhuma loja após rodada " + rodada
                );
            }
        }

        assertEquals(
                List.of(2, 4, 6, 8),
                lojasAbertas
        );

        EstadoJogoResponse finalCompeticao = estado();

        assertTrue(
                finalCompeticao.getCompeticaoEncerrada()
        );

        assertEquals(
                primeiroVencedor,
                finalCompeticao.getCampeao()
        );

        assertEquals(5, vitorias(primeiroVencedor));
        assertEquals(4, vitorias(segundoVencedor));

        assertEquals(10, saldo(primeiroVencedor));
        assertEquals(8, saldo(segundoVencedor));

        assertEquals(
                0,
                finalCompeticao.getPetiscosLojaCount()
        );

        assertEquals(
                0,
                finalCompeticao.getTempoLojaRestanteSegundos()
        );

        System.out.println(
                "[OK] Todas as lojas abertas corretamente: "
                        + lojasAbertas
        );

        System.out.println(
                "[OK] Campeão: " + primeiroVencedor
        );

        System.out.println(
                "[OK] Placar final: 5 x 4"
        );

        // Não pode jogar após o encerramento.

        StatusRuntimeException erroJogada = assertThrows(
                StatusRuntimeException.class,
                () -> stubDe(primeiroVencedor).passarVez(
                        acaoSimples(primeiroVencedor)
                )
        );

        assertEquals(
                Status.Code.FAILED_PRECONDITION,
                erroJogada.getStatus().getCode()
        );

        // Não pode comprar após o encerramento.

        StatusRuntimeException erroCompra = assertThrows(
                StatusRuntimeException.class,
                () -> comprarPetisco(
                        primeiroVencedor,
                        TipoPetiscoMensagem.OVO_COLORIDO
                )
        );

        assertEquals(
                Status.Code.FAILED_PRECONDITION,
                erroCompra.getStatus().getCode()
        );

        System.out.println(
                "[OK] Jogadas bloqueadas após o campeonato."
        );

        System.out.println(
                "[OK] Compras bloqueadas após o campeonato."
        );

        System.out.println(
                "[SUCESSO] Competição completa funcionando!"
        );
    }

    // TESTE 5 - CAMPEONATO ENCERRADO EM RODADA PAR

    @Test
    void naoDeveAbrirLojaQuandoCompeticaoEncerrarEmRodadaPar()
            throws Exception {

        System.out.println(
                "\n[TESTE] CAMPEONATO ENCERRADO EM RODADA PAR"
        );

        String campeao = finalizarRodada();

        String adversario = adversarioDe(campeao);

        // Rodada 2: adversário vence.

        finalizarRodadaComVencedor(adversario);

        assertEquals(
                FasePartidaMensagem.LOJA_PETISCOS,
                estado().getFase()
        );

        aguardarFechamentoLoja();

        // Rodadas 3 e 4: campeão vence.

        finalizarRodadaComVencedor(campeao);
        finalizarRodadaComVencedor(campeao);

        assertEquals(3, vitorias(campeao));

        assertEquals(
                FasePartidaMensagem.LOJA_PETISCOS,
                estado().getFase()
        );

        aguardarFechamentoLoja();

        // Rodada 5: quarta vitória.

        finalizarRodadaComVencedor(campeao);

        assertEquals(4, vitorias(campeao));

        // Rodada 6: quinta vitória e fim da competição.

        finalizarRodadaComVencedor(campeao);

        EstadoJogoResponse atual = estado();

        assertEquals(6, atual.getNumeroRodada());

        assertEquals(
                FasePartidaMensagem.ENCERRADA,
                atual.getFase()
        );

        assertTrue(atual.getCompeticaoEncerrada());

        assertEquals(campeao, atual.getCampeao());

        assertEquals(5, vitorias(campeao));
        assertEquals(1, vitorias(adversario));

        assertEquals(10, saldo(campeao));
        assertEquals(2, saldo(adversario));

        assertEquals(0, atual.getPetiscosLojaCount());

        assertEquals(
                0,
                atual.getTempoLojaRestanteSegundos()
        );

        synchronized (getSala()) {

            assertFalse(
                    getSala().getPartidaGeral().isLojaAberta()
            );
        }

        System.out.println(
                "[OK] Campeonato encerrado na rodada 6."
        );

        System.out.println(
                "[OK] Placar final: 5 x 1."
        );

        System.out.println(
                "[OK] Nenhuma loja aberta após o campeonato."
        );

        System.out.println(
                "[SUCESSO] Encerramento em rodada par funcionando!"
        );
    }
}
