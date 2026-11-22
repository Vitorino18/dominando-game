package br.com.dominando.network;

import br.com.dominando.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class DesceUmaStreamingTeste {
    private static final String SERVIDOR = "localhost";
    private static final int PORTA = 50051;

    public static void main(String[] args) throws Exception {

        ManagedChannel canal = ManagedChannelBuilder
                .forAddress(SERVIDOR, PORTA)
                .usePlaintext()
                .build();

        try {
            // Cliente para chamadas RPC unárias
            DesceUmaServiceGrpc.DesceUmaServiceBlockingStub cliente =
                    DesceUmaServiceGrpc.newBlockingStub(canal);

            // Cliente para receber streaming
            DesceUmaServiceGrpc.DesceUmaServiceStub streaming =
                    DesceUmaServiceGrpc.newStub(canal);

            System.out.println("=== TESTE DE SERVER STREAMING ===");

            // 1. Criar sala
            CriarSalaResponse criacao = cliente.criarSala(
                    CriarSalaRequest.newBuilder()
                            .setNomeCriador("Vitor")
                            .build()
            );

            String codigo = criacao.getCodigoSala();
            String tokenVitor = criacao.getTokenJogador();

            System.out.println("Sala criada: " + codigo);

            // 2. Entrar com Yago
            EntrarSalaResponse entrada = cliente.entrarSala(
                    EntrarSalaRequest.newBuilder()
                            .setCodigoSala(codigo)
                            .setNomeJogador("Yago")
                            .build()
            );

            String tokenYago = entrada.getTokenJogador();

            System.out.println("Yago entrou na sala!");

            // 3. Preparar recebimento das atualizações
            CountDownLatch estadosIniciais = new CountDownLatch(2);
            CountDownLatch atualizacoesJogada = new CountDownLatch(2);

            AtomicInteger mensagensVitor = new AtomicInteger();
            AtomicInteger mensagensYago = new AtomicInteger();

            AtomicReference<EstadoJogoResponse> estadoVitor =
                    new AtomicReference<>();

            AtomicReference<EstadoJogoResponse> estadoYago =
                    new AtomicReference<>();

            // 4. Abrir streaming de Vitor
            streaming.acompanharJogo(
                    EstadoJogoRequest.newBuilder()
                            .setCodigoSala(codigo)
                            .setTokenJogador(tokenVitor)
                            .build(),

                    criarObservador(
                            "Vitor",
                            mensagensVitor,
                            estadoVitor,
                            estadosIniciais,
                            atualizacoesJogada
                    )
            );

            // 5. Abrir streaming de Yago
            streaming.acompanharJogo(
                    EstadoJogoRequest.newBuilder()
                            .setCodigoSala(codigo)
                            .setTokenJogador(tokenYago)
                            .build(),

                    criarObservador(
                            "Yago",
                            mensagensYago,
                            estadoYago,
                            estadosIniciais,
                            atualizacoesJogada
                    )
            );

            System.out.println("\nStreams abertos!");

            // 6. Iniciar partida
            cliente.iniciarPartida(
                    IniciarPartidaRequest.newBuilder()
                            .setCodigoSala(codigo)
                            .setTokenCriador(tokenVitor)
                            .build()
            );

            System.out.println("Partida iniciada!");

            // Esperar ambos receberem o estado inicial
            if (!estadosIniciais.await(10, TimeUnit.SECONDS)) {
                System.out.println(
                        "ERRO: nem todos receberam o estado inicial."
                );
                return;
            }

            System.out.println(
                    "\nOs dois jogadores receberam o estado inicial!"
            );

            // 7. Identificar jogador da vez
            EstadoJogoResponse estadoAtual = estadoVitor.get();

            String jogadorDaVez = estadoAtual.getJogadorDaVez();

            System.out.println(
                    "\nJogador da vez: " + jogadorDaVez
            );

            String tokenDaVez;
            EstadoJogoResponse estadoDaVez;

            if (jogadorDaVez.equals("Vitor")) {
                tokenDaVez = tokenVitor;
                estadoDaVez = estadoVitor.get();
            } else {
                tokenDaVez = tokenYago;
                estadoDaVez = estadoYago.get();
            }

            // 8. Escolher a maior bucha
            int indicePeca = encontrarPecaInicial(estadoDaVez);

            if (indicePeca < 0) {
                throw new IllegalStateException(
                        "Nenhuma peça encontrada."
                );
            }

            PecaMensagem peca = estadoDaVez.getMinhaMao(indicePeca);

            System.out.println(
                    "Peça escolhida: ["
                            + peca.getLadoA()
                            + "|"
                            + peca.getLadoB()
                            + "]"
            );

            // 9. Executar jogada unária
            AcaoJogoResponse resultado = cliente.jogarPeca(
                    JogarPecaRequest.newBuilder()
                            .setCodigoSala(codigo)
                            .setTokenJogador(tokenDaVez)
                            .setIndicePeca(indicePeca)
                            .setLado(LadoJogada.DIREITA)
                            .build()
            );

            System.out.println(
                    "\nResultado da jogada: "
                            + resultado.getResultado()
            );

            // 10. Esperar as atualizações automáticas
            if (!atualizacoesJogada.await(10, TimeUnit.SECONDS)) {
                System.out.println(
                        "ERRO: nem todos receberam a atualização."
                );
                return;
            }

            System.out.println("\n==============================");
            System.out.println("       TESTE CONCLUÍDO");
            System.out.println("==============================");

            System.out.println(
                    "Atualizações recebidas por Vitor: "
                            + mensagensVitor.get()
            );

            System.out.println(
                    "Atualizações recebidas por Yago: "
                            + mensagensYago.get()
            );

            System.out.println(
                    "Peças na mesa para Vitor: "
                            + estadoVitor.get().getPecasMesaCount()
            );

            System.out.println(
                    "Peças na mesa para Yago: "
                            + estadoYago.get().getPecasMesaCount()
            );

            System.out.println(
                    "Peças no cava: "
                            + estadoVitor.get().getPecasBaralho()
            );

        } finally {
            canal.shutdownNow();
            canal.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private static StreamObserver<EstadoJogoResponse> criarObservador(
            String nome,
            AtomicInteger contador,
            AtomicReference<EstadoJogoResponse> estadoAtual,
            CountDownLatch estadosIniciais,
            CountDownLatch atualizacoesJogada) {

        return new StreamObserver<>() {

            @Override
            public void onNext(EstadoJogoResponse estado) {

                int numeroMensagem = contador.incrementAndGet();

                estadoAtual.set(estado);

                System.out.println(
                        "\n[STREAM " + nome + "] "
                                + "Atualização #" + numeroMensagem
                );

                System.out.println(
                        "Jogador da vez: "
                                + estado.getJogadorDaVez()
                );

                System.out.println(
                        "Peças na minha mão: "
                                + estado.getMinhaMaoCount()
                );

                System.out.println(
                        "Peças na mesa: "
                                + estado.getPecasMesaCount()
                );

                System.out.println(
                        "Peças no cava: "
                                + estado.getPecasBaralho()
                );

                if (numeroMensagem == 1) {
                    estadosIniciais.countDown();
                } else if (numeroMensagem == 2) {
                    atualizacoesJogada.countDown();
                }
            }

            @Override
            public void onError(Throwable erro) {
                System.out.println(
                        "[STREAM " + nome + "] Erro: "
                                + erro.getMessage()
                );
            }

            @Override
            public void onCompleted() {
                System.out.println(
                        "[STREAM " + nome + "] Encerrado."
                );
            }
        };
    }

    private static int encontrarPecaInicial(
            EstadoJogoResponse estado) {

        int indiceEscolhido = -1;
        int maiorBucha = -1;

        for (int i = 0; i < estado.getMinhaMaoCount(); i++) {

            PecaMensagem peca = estado.getMinhaMao(i);

            if (peca.getLadoA() == peca.getLadoB()
                    && peca.getLadoA() > maiorBucha) {

                maiorBucha = peca.getLadoA();
                indiceEscolhido = i;
            }
        }

        if (indiceEscolhido != -1) {
            return indiceEscolhido;
        }

        int maiorSoma = -1;

        for (int i = 0; i < estado.getMinhaMaoCount(); i++) {

            PecaMensagem peca = estado.getMinhaMao(i);
            int soma = peca.getLadoA() + peca.getLadoB();

            if (soma > maiorSoma) {
                maiorSoma = soma;
                indiceEscolhido = i;
            }
        }

        return indiceEscolhido;
    }
}
