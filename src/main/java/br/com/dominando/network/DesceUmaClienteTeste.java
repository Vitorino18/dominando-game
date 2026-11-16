
package br.com.dominando.network;

import br.com.dominando.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.HashMap;
import java.util.Map;

public class DesceUmaClienteTeste {

    private static final String JOGADOR_1 = "Vitor";
    private static final String JOGADOR_2 = "Yago";
    private static final int LIMITE_ACOES = 2000;

    public static void main(String[] args) {

        ManagedChannel canal = ManagedChannelBuilder
                .forAddress("localhost", 50051)
                .usePlaintext()
                .build();

        try {
            DesceUmaServiceGrpc.DesceUmaServiceBlockingStub cliente =
                    DesceUmaServiceGrpc.newBlockingStub(canal);

            CriarSalaResponse criacao = cliente.criarSala(
                    CriarSalaRequest.newBuilder()
                            .setNomeCriador(JOGADOR_1)
                            .build()
            );

            String codigo = criacao.getCodigoSala();
            String tokenJogador1 = criacao.getTokenJogador();

            System.out.println("Sala criada: " + codigo);

            EntrarSalaResponse entrada = cliente.entrarSala(
                    EntrarSalaRequest.newBuilder()
                            .setCodigoSala(codigo)
                            .setNomeJogador(JOGADOR_2)
                            .build()
            );

            String tokenJogador2 = entrada.getTokenJogador();

            System.out.println("Segundo jogador entrou!");

            ConsultarSalaResponse consulta = cliente.consultarSala(
                    ConsultarSalaRequest.newBuilder()
                            .setCodigoSala(codigo)
                            .setTokenJogador(tokenJogador1)
                            .build()
            );

            System.out.println(
                    "Jogadores: " + consulta.getNomesJogadoresList()
            );

            System.out.println(
                    "Pode iniciar: " + consulta.getPodeIniciar()
            );

            cliente.iniciarPartida(
                    IniciarPartidaRequest.newBuilder()
                            .setCodigoSala(codigo)
                            .setTokenCriador(tokenJogador1)
                            .build()
            );

            System.out.println("Competição iniciada!");

            simularCompeticao(
                    cliente,
                    codigo,
                    tokenJogador1,
                    tokenJogador2
            );

        } catch (StatusRuntimeException e) {
            System.out.println("Erro gRPC: " + e.getStatus());
        } finally {
            canal.shutdown();
        }
    }

    private static void simularCompeticao(
            DesceUmaServiceGrpc.DesceUmaServiceBlockingStub cliente,
            String codigo,
            String tokenJogador1,
            String tokenJogador2) {

        Map<String, Integer> vitoriasAnteriores = new HashMap<>();

        EstadoJogoResponse estadoInicial = consultarEstado(
                cliente, codigo, tokenJogador1
        );

        atualizarVitorias(estadoInicial, vitoriasAnteriores);

        int numeroPartida = 1;

        System.out.println("\n==============================");
        System.out.println("       PARTIDA " + numeroPartida);
        System.out.println("==============================");

        for (int numeroAcao = 1;
             numeroAcao <= LIMITE_ACOES;
             numeroAcao++) {

            EstadoJogoResponse estado = consultarEstado(
                    cliente, codigo, tokenJogador1
            );

            if (estado.getCompeticaoEncerrada()) {
                mostrarCampeao(estado);
                return;
            }

            String nomeDaVez = estado.getJogadorDaVez();

            String tokenDaVez;

            if (nomeDaVez.equals(JOGADOR_1)) {
                tokenDaVez = tokenJogador1;
            } else if (nomeDaVez.equals(JOGADOR_2)) {
                tokenDaVez = tokenJogador2;
            } else {
                throw new IllegalStateException(
                        "Jogador desconhecido: " + nomeDaVez
                );
            }

            EstadoJogoResponse estadoJogador = consultarEstado(
                    cliente, codigo, tokenDaVez
            );

            System.out.println("\n--- Ação " + numeroAcao + " ---");
            System.out.println("Jogador da vez: " + nomeDaVez);
            System.out.println(
                    "Peças na mão: " + estadoJogador.getMinhaMaoCount()
            );
            System.out.println(
                    "Peças na mesa: " + estadoJogador.getPecasMesaCount()
            );

            // Quantidade de peças no cava
            System.out.println(
                    "Peças no cava: " + estadoJogador.getPecasBaralho()
            );

            boolean jogou = tentarJogar(
                    cliente, codigo, tokenDaVez, estadoJogador
            );

            if (jogou) {

                EstadoJogoResponse depois = consultarEstado(
                        cliente, codigo, tokenJogador1
                );

                if (verificarVencedor(
                        depois, vitoriasAnteriores, numeroPartida)) {

                    numeroPartida++;

                    if (!depois.getCompeticaoEncerrada()) {
                        mostrarInicioPartida(numeroPartida, depois);
                    }
                }

                if (depois.getCompeticaoEncerrada()) {
                    mostrarCampeao(depois);
                    return;
                }

                continue;
            }

            AcaoJogoResponse compra = cliente.comprarPeca(
                    AcaoSimplesRequest.newBuilder()
                            .setCodigoSala(codigo)
                            .setTokenJogador(tokenDaVez)
                            .build()
            );

            System.out.println(
                    "Compra: " + compra.getResultado()
            );

            if (compra.getResultado().equals("COMPRA_REALIZADA")) {

                EstadoJogoResponse novoEstado = consultarEstado(
                        cliente, codigo, tokenDaVez
                );

                System.out.println(
                        "Peças no cava após compra: "
                                + novoEstado.getPecasBaralho()
                );

                if (tentarJogar(
                        cliente, codigo, tokenDaVez, novoEstado)) {

                    EstadoJogoResponse depois = consultarEstado(
                            cliente, codigo, tokenJogador1
                    );

                    if (verificarVencedor(
                            depois, vitoriasAnteriores, numeroPartida)) {

                        numeroPartida++;

                        if (!depois.getCompeticaoEncerrada()) {
                            mostrarInicioPartida(numeroPartida, depois);
                        }
                    }

                    if (depois.getCompeticaoEncerrada()) {
                        mostrarCampeao(depois);
                        return;
                    }

                    continue;
                }
            }

            AcaoJogoResponse passagem = cliente.passarVez(
                    AcaoSimplesRequest.newBuilder()
                            .setCodigoSala(codigo)
                            .setTokenJogador(tokenDaVez)
                            .build()
            );

            System.out.println(
                    "Passar vez: " + passagem.getResultado()
            );

            EstadoJogoResponse depois = consultarEstado(
                    cliente, codigo, tokenJogador1
            );

            if (verificarVencedor(
                    depois, vitoriasAnteriores, numeroPartida)) {

                numeroPartida++;

                if (!depois.getCompeticaoEncerrada()) {
                    mostrarInicioPartida(numeroPartida, depois);
                }
            }

            if (depois.getCompeticaoEncerrada()) {
                mostrarCampeao(depois);
                return;
            }

            if (!passagem.getResultado().equals("VEZ_PASSADA")) {
                System.out.println(
                        "Passagem recusada. Interrompendo para análise."
                );
                return;
            }
        }

        System.out.println(
                "\nLimite de ações atingido. Verifique as regras."
        );
    }

    private static boolean verificarVencedor(
            EstadoJogoResponse estado,
            Map<String, Integer> vitoriasAnteriores,
            int numeroPartida) {

        boolean houveVencedor = false;

        for (JogadorMensagem jogador : estado.getJogadoresList()) {

            int vitoriasAntes = vitoriasAnteriores.getOrDefault(
                    jogador.getNome(), 0
            );

            if (jogador.getVitorias() > vitoriasAntes) {

                System.out.println("\n================================");
                System.out.println(
                        "VENCEDOR DA PARTIDA " + numeroPartida
                );
                System.out.println(
                        "Jogador: " + jogador.getNome()
                );
                System.out.println(
                        "Vitórias acumuladas: " + jogador.getVitorias()
                );
                System.out.println(
                        "Pontos: " + jogador.getPontuacao()
                );
                System.out.println("================================");

                houveVencedor = true;
            }
        }

        atualizarVitorias(estado, vitoriasAnteriores);

        return houveVencedor;
    }

    private static void atualizarVitorias(
            EstadoJogoResponse estado,
            Map<String, Integer> vitoriasAnteriores) {

        for (JogadorMensagem jogador : estado.getJogadoresList()) {
            vitoriasAnteriores.put(
                    jogador.getNome(),
                    jogador.getVitorias()
            );
        }
    }

    private static void mostrarInicioPartida(
            int numeroPartida,
            EstadoJogoResponse estado) {

        System.out.println("\n==============================");
        System.out.println("       PARTIDA " + numeroPartida);
        System.out.println("==============================");

        System.out.println(
                "Jogador inicial: " + estado.getJogadorDaVez()
        );

        System.out.println(
                "Peças no cava: " + estado.getPecasBaralho()
        );
    }

    private static void mostrarCampeao(
            EstadoJogoResponse estado) {

        System.out.println("\n==============================");
        System.out.println("     COMPETIÇÃO ENCERRADA");
        System.out.println("==============================");

        System.out.println(
                "CAMPEÃO: " + estado.getCampeao()
        );

        System.out.println("\n=== PLACAR FINAL ===");

        for (JogadorMensagem jogador : estado.getJogadoresList()) {

            System.out.println(
                    jogador.getNome()
                            + " | Vitórias: " + jogador.getVitorias()
                            + " | Pontos: " + jogador.getPontuacao()
                            + " | Petiscos: " + jogador.getPontosPetiscos()
            );
        }
    }

    private static boolean tentarJogar(
            DesceUmaServiceGrpc.DesceUmaServiceBlockingStub cliente,
            String codigo,
            String token,
            EstadoJogoResponse estado) {

        if (estado.getPecasMesaCount() == 0) {

            int indice = encontrarPecaInicial(estado);

            return executarJogada(
                    cliente, codigo, token,
                    indice, LadoJogada.DIREITA
            );
        }

        PecaMensagem primeira = estado.getPecasMesa(0);
        PecaMensagem ultima = estado.getPecasMesa(
                estado.getPecasMesaCount() - 1
        );

        int pontaEsquerda = primeira.getLadoA();
        int pontaDireita = ultima.getLadoB();

        for (int i = 0; i < estado.getMinhaMaoCount(); i++) {

            PecaMensagem peca = estado.getMinhaMao(i);

            if (peca.getLadoA() == pontaEsquerda
                    || peca.getLadoB() == pontaEsquerda) {

                return executarJogada(
                        cliente, codigo, token,
                        i, LadoJogada.ESQUERDA
                );
            }

            if (peca.getLadoA() == pontaDireita
                    || peca.getLadoB() == pontaDireita) {

                return executarJogada(
                        cliente, codigo, token,
                        i, LadoJogada.DIREITA
                );
            }
        }

        return false;
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

    private static boolean executarJogada(
            DesceUmaServiceGrpc.DesceUmaServiceBlockingStub cliente,
            String codigo,
            String token,
            int indice,
            LadoJogada lado) {

        if (indice < 0) {
            return false;
        }

        AcaoJogoResponse resposta = cliente.jogarPeca(
                JogarPecaRequest.newBuilder()
                        .setCodigoSala(codigo)
                        .setTokenJogador(token)
                        .setIndicePeca(indice)
                        .setLado(lado)
                        .build()
        );

        System.out.println(
                "Jogada: " + resposta.getResultado()
        );

        return resposta.getResultado().equals("JOGADA_VALIDA")
                || resposta.getResultado().equals("PARTIDA_ENCERRADA");
    }

    private static EstadoJogoResponse consultarEstado(
            DesceUmaServiceGrpc.DesceUmaServiceBlockingStub cliente,
            String codigo,
            String token) {

        return cliente.consultarEstadoJogo(
                EstadoJogoRequest.newBuilder()
                        .setCodigoSala(codigo)
                        .setTokenJogador(token)
                        .build()
        );
    }
}