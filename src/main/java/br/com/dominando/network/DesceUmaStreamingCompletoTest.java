package br.com.dominando.network;

import br.com.dominando.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class DesceUmaStreamingCompletoTest {
    private static final int PORTA = 50051;

    public static void main(String[] args) throws Exception {

        ManagedChannel canal = ManagedChannelBuilder
                .forAddress("localhost", PORTA)
                .usePlaintext()
                .build();

        try {
            var cliente = DesceUmaServiceGrpc.newBlockingStub(canal);
            var streaming = DesceUmaServiceGrpc.newStub(canal);

            System.out.println("=== TESTE COMPLETO DE STREAMING ===");

            CriarSalaResponse criacao = cliente.criarSala(
                    CriarSalaRequest.newBuilder()
                            .setNomeCriador("Vitor")
                            .build()
            );

            String codigo = criacao.getCodigoSala();
            String tokenVitor = criacao.getTokenJogador();

            EntrarSalaResponse entrada = cliente.entrarSala(
                    EntrarSalaRequest.newBuilder()
                            .setCodigoSala(codigo)
                            .setNomeJogador("Yago")
                            .build()
            );

            String tokenYago = entrada.getTokenJogador();

            System.out.println("Sala: " + codigo);
            System.out.println("Jogadores: Vitor e Yago");

            // Iniciamos antes de abrir os streams.
            cliente.iniciarPartida(
                    IniciarPartidaRequest.newBuilder()
                            .setCodigoSala(codigo)
                            .setTokenCriador(tokenVitor)
                            .build()
            );

            System.out.println("Partida iniciada!");

            ClienteObservador vitor = new ClienteObservador("Vitor");
            ClienteObservador yago = new ClienteObservador("Yago");

            EstadoJogoRequest pedidoVitor =
                    criarPedido(codigo, tokenVitor);

            EstadoJogoRequest pedidoYago =
                    criarPedido(codigo, tokenYago);

            // Inscrever os dois clientes.
            streaming.acompanharJogo(pedidoVitor, vitor);
            streaming.acompanharJogo(pedidoYago, yago);

            vitor.aguardarAtualizacao();
            yago.aguardarAtualizacao();

            System.out.println("\n=== ESTADO INICIAL ===");
            imprimirEstado(vitor);
            imprimirEstado(yago);

            // Executar até quatro jogadas válidas.
            for (int rodada = 1; rodada <= 4; rodada++) {

                EstadoJogoResponse estado = vitor.getEstado();
                String nomeDaVez = estado.getJogadorDaVez();

                ClienteObservador jogadorAtual =
                        nomeDaVez.equals("Vitor") ? vitor : yago;

                String tokenAtual =
                        nomeDaVez.equals("Vitor")
                                ? tokenVitor : tokenYago;

                EstadoJogoResponse estadoJogador =
                        jogadorAtual.getEstado();

                JogadaEscolhida jogada =
                        escolherJogada(estadoJogador);

                if (jogada == null) {
                    System.out.println(
                            "\nNenhuma jogada disponível. " +
                                    "Encerrando sequência de jogadas."
                    );
                    break;
                }

                System.out.println(
                        "\n=== JOGADA " + rodada + " ==="
                );

                System.out.println(
                        nomeDaVez + " vai jogar a peça: " +
                                jogada.peca()
                );

                int antesVitor = vitor.getQuantidadeMensagens();
                int antesYago = yago.getQuantidadeMensagens();

                AcaoJogoResponse resposta = cliente.jogarPeca(
                        JogarPecaRequest.newBuilder()
                                .setCodigoSala(codigo)
                                .setTokenJogador(tokenAtual)
                                .setIndicePeca(jogada.indice())
                                .setLado(jogada.lado())
                                .build()
                );

                System.out.println(
                        "Resultado: " + resposta.getResultado()
                );

                if (!resposta.getResultado()
                        .equals("JOGADA_VALIDA")
                        && !resposta.getResultado()
                        .equals("PARTIDA_ENCERRADA")) {
                    System.out.println(
                            "Jogada não aceita. " +
                                    "Encerrando sequência."
                    );
                    break;
                }

                vitor.aguardarQuantidade(antesVitor + 1);
                yago.aguardarQuantidade(antesYago + 1);

                imprimirEstado(vitor);
                imprimirEstado(yago);

                if (resposta.getCompeticaoEncerrada()) {
                    System.out.println(
                            "Campeão: " + resposta.getCampeao()
                    );
                    break;
                }
            }

            System.out.println("\n=== TESTE DE RECONEXÃO ===");

            // Simula Yago perdendo sua conexão de streaming.
            // Utilizamos um canal separado para poder encerrá-lo.
            testarReconexao(codigo, tokenYago);

            System.out.println("\n=== TESTE FINALIZADO ===");

        } finally {
            canal.shutdownNow();
            canal.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private static void testarReconexao(
            String codigo,
            String tokenYago) throws Exception {

        EstadoJogoRequest pedido = criarPedido(
                codigo, tokenYago
        );

        ManagedChannel canalTemporario = ManagedChannelBuilder
                .forAddress("localhost", PORTA)
                .usePlaintext()
                .build();

        try {
            ClienteObservador observador =
                    new ClienteObservador("Yago - conexão temporária");

            var stub = DesceUmaServiceGrpc.newStub(canalTemporario);

            stub.acompanharJogo(pedido, observador);
            observador.aguardarAtualizacao();

            System.out.println(
                    "Yago recebeu estado na conexão temporária."
            );

        } finally {
            canalTemporario.shutdownNow();
            canalTemporario.awaitTermination(5, TimeUnit.SECONDS);
        }

        ManagedChannel novoCanal = ManagedChannelBuilder
                .forAddress("localhost", PORTA)
                .usePlaintext()
                .build();

        try {
            ClienteObservador reconectado =
                    new ClienteObservador("Yago reconectado");

            var novoStub = DesceUmaServiceGrpc.newStub(novoCanal);

            novoStub.acompanharJogo(pedido, reconectado);
            reconectado.aguardarAtualizacao();

            System.out.println(
                    "Reconexão realizada! Estado recebido:"
            );

            imprimirEstado(reconectado);

        } finally {
            novoCanal.shutdownNow();
            novoCanal.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private static EstadoJogoRequest criarPedido(
            String codigo,
            String token) {

        return EstadoJogoRequest.newBuilder()
                .setCodigoSala(codigo)
                .setTokenJogador(token)
                .build();
    }

    private static JogadaEscolhida escolherJogada(
            EstadoJogoResponse estado) {

        if (estado.getMinhaMaoCount() == 0) {
            return null;
        }

        // Primeira jogada: maior bucha da mão.
        if (estado.getPecasMesaCount() == 0) {

            int indice = -1;
            int maiorBucha = -1;

            for (int i = 0; i < estado.getMinhaMaoCount(); i++) {
                PecaMensagem peca = estado.getMinhaMao(i);

                if (peca.getLadoA() == peca.getLadoB()
                        && peca.getLadoA() > maiorBucha) {

                    indice = i;
                    maiorBucha = peca.getLadoA();
                }
            }

            if (indice >= 0) {
                return new JogadaEscolhida(
                        indice,
                        LadoJogada.DIREITA,
                        estado.getMinhaMao(indice).toString()
                );
            }

            // Caso não tenha bucha, escolhe maior soma.
            int maiorSoma = -1;

            for (int i = 0; i < estado.getMinhaMaoCount(); i++) {
                PecaMensagem peca = estado.getMinhaMao(i);
                int soma = peca.getLadoA() + peca.getLadoB();

                if (soma > maiorSoma) {
                    maiorSoma = soma;
                    indice = i;
                }
            }

            return new JogadaEscolhida(
                    indice,
                    LadoJogada.DIREITA,
                    estado.getMinhaMao(indice).toString()
            );
        }

        int esquerda =
                estado.getPecasMesa(0).getLadoA();

        int direita =
                estado.getPecasMesa(
                        estado.getPecasMesaCount() - 1
                ).getLadoB();

        for (int i = 0; i < estado.getMinhaMaoCount(); i++) {
            PecaMensagem peca = estado.getMinhaMao(i);

            if (peca.getLadoA() == esquerda
                    || peca.getLadoB() == esquerda) {

                return new JogadaEscolhida(
                        i,
                        LadoJogada.ESQUERDA,
                        peca.toString()
                );
            }

            if (peca.getLadoA() == direita
                    || peca.getLadoB() == direita) {

                return new JogadaEscolhida(
                        i,
                        LadoJogada.DIREITA,
                        peca.toString()
                );
            }
        }

        return null;
    }

    private static void imprimirEstado(
            ClienteObservador observador) {

        EstadoJogoResponse estado = observador.getEstado();

        System.out.println(
                "\n[" + observador.getNome() + "]"
        );

        System.out.println(
                "Jogador da vez: " + estado.getJogadorDaVez()
        );

        System.out.println(
                "Peças na mão: " + estado.getMinhaMaoCount()
        );

        System.out.println(
                "Peças na mesa: " + estado.getPecasMesaCount()
        );

        System.out.println(
                "Peças no cava: " + estado.getPecasBaralho()
        );

        System.out.println(
                "Atualizações: " +
                        observador.getQuantidadeMensagens()
        );
    }

    private record JogadaEscolhida(
            int indice,
            LadoJogada lado,
            String peca) {
    }

    private static class ClienteObservador
            implements StreamObserver<EstadoJogoResponse> {

        private final String nome;
        private final AtomicReference<EstadoJogoResponse> estado =
                new AtomicReference<>();

        private final AtomicInteger mensagens =
                new AtomicInteger();

        private final Object monitor = new Object();

        private volatile Throwable erro;

        ClienteObservador(String nome) {
            this.nome = nome;
        }

        @Override
        public void onNext(EstadoJogoResponse resposta) {
            estado.set(resposta);
            int numero = mensagens.incrementAndGet();

            System.out.println(
                    "[STREAM " + nome + "] Atualização #" + numero
            );

            synchronized (monitor) {
                monitor.notifyAll();
            }
        }

        @Override
        public void onError(Throwable throwable) {
            erro = throwable;

            synchronized (monitor) {
                monitor.notifyAll();
            }
        }

        @Override
        public void onCompleted() {
            synchronized (monitor) {
                monitor.notifyAll();
            }
        }

        public void aguardarAtualizacao()
                throws InterruptedException {

            aguardarQuantidade(1);
        }

        public void aguardarQuantidade(int quantidade)
                throws InterruptedException {

            long limite = System.nanoTime()
                    + TimeUnit.SECONDS.toNanos(10);

            synchronized (monitor) {
                while (mensagens.get() < quantidade
                        && erro == null) {

                    long restante =
                            limite - System.nanoTime();

                    if (restante <= 0) {
                        throw new IllegalStateException(
                                "Timeout aguardando streaming de " + nome
                        );
                    }

                    TimeUnit.NANOSECONDS.timedWait(
                            monitor, restante
                    );
                }
            }

            if (erro != null) {
                throw new IllegalStateException(
                        "Erro no streaming de " + nome, erro
                );
            }
        }

        public EstadoJogoResponse getEstado() {
            return estado.get();
        }

        public int getQuantidadeMensagens() {
            return mensagens.get();
        }

        public String getNome() {
            return nome;
        }
    }

}
