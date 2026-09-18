package br.com.dominando.controller;

import br.com.dominando.model.*;
import java.util.Scanner;

public class GameController {
    private final Partida partida;
    private final Scanner scan;

    public GameController(Partida partida){
        this.partida = partida;
        this.scan = new Scanner(System.in);
    }
    public void iniciar() {
        partida.iniciarPartida();

        System.out.println("\n================================");
        System.out.println("         Desce Uma");
        System.out.println("==================================");

        while (!partida.terminouRodada() && !partida.rodadaTravada()) {
            mostrarEstadoDaPartida();
            mostrarMao();
            mostrarMenu();

            AcaoJogo acao = lerAcaoJogador();

            Integer indice = null;
            LadoMesa lado = null;

            if (acao == AcaoJogo.JOGAR_PECA) {
                indice = escolherIndicePeca();
                lado = escolherLado();
            }

            ResultadoJogada resultado = executarAcao(acao, indice, lado);
            tratarResultado(resultado);
        }

        finalizarRodada();
    }


    private void mostrarEstadoDaPartida() {
        System.out.println("Mesa:" + partida.getMesa().getPecasMesa());

        System.out.println("\nBaralho: " + partida.getBaralho().quantidadePecas() + " peças.");

        System.out.println("\nJogador da vez: " + partida.getJogadorVez().getNome());

    }
    
    private void mostrarMao(){
        System.out.println("\nSua mão:");

        int indicePeca = 0;

        for (Peca peca : partida.getJogadorVez().getMao()){
            System.out.println(indicePeca + " -> " + peca);
            indicePeca++;

        }
    }

    private void mostrarMenu() {
        System.out.println("""
            Escolha uma ação:
            1 - Jogar peça
            2 - Comprar peça
            3 - Passar a vez

            """);
    }


    private LadoMesa escolherLado() {
        while (true) {
            System.out.println("""
                Escolha o lado:
                E - Esquerda
                D - Direita
                
                """);

            String lado = scan.nextLine().trim().toUpperCase();

            switch (lado) {
                case "E":
                    return LadoMesa.ESQUERDA;
                case "D":
                    return LadoMesa.DIREITA;
                default:
                    System.out.println("Lado inválido.");
            }
        }
    }
    private Integer escolherIndicePeca() {
        while (true) {
            System.out.print("Escolha o índice da peça: ");

            int indicePeca = scan.nextInt();
            scan.nextLine();

            if (indicePeca >= 0 && indicePeca < partida.getJogadorVez().quantidadePecas()){
                return indicePeca;
            }

            System.out.println("Índice inválido.");
        }
    }

    public ResultadoJogada executarAcao(AcaoJogo acao, Integer indicePeca, LadoMesa lado){
        switch (acao) {
            case JOGAR_PECA:
                Peca peca = partida.getJogadorVez().getPeca(indicePeca);
                return partida.jogar(peca, lado);

            case COMPRAR_PECA:
                return partida.comprarPeca();

            case PASSAR_VEZ:
                partida.passarVez();
                return ResultadoJogada.VEZ_PASSADA;

            default:
                throw new IllegalArgumentException("Ação inválida.");
        }
    }

    private AcaoJogo lerAcaoJogador() {
        while (true) {
            System.out.print("Escolha uma ação: ");

            String opcao = scan.nextLine().trim();
            switch (opcao) {
                case "1":
                    return AcaoJogo.JOGAR_PECA;
                case "2":
                    return AcaoJogo.COMPRAR_PECA;
                case "3":
                    return AcaoJogo.PASSAR_VEZ;
                default:
                    System.out.println("Opção inválida.");
            }
        }
    }

    private void tratarResultado(ResultadoJogada resultado) {
        switch (resultado) {
            case JOGADA_VALIDA :
                    System.out.println("Peça jogada com sucesso.");
                    break;
            case GATO :
                    System.out.println(" MIAAALLL, GATO! perdeu a vez.");
                    break;
            case NAO_POSSUI_PECA :
                    System.out.println("Essa peça não pertence ao jogador.");
                    break;
            case COMPRA_REALIZADA :
                    System.out.println("Você comprou uma peça.");
                    break;
            case COMPROU_JA_REALIZADA:
                System.out.println("voce ja comprou uma peça no turno");
                break;
            case BARALHO_VAZIO:
                System.out.println("O baralho está vazio");
                break;
            case VEZ_PASSADA :
                    System.out.println("Você passou a vez.");
                    break;
            case PARTIDA_ENCERRADA :
                    System.out.println("A rodada terminou.");
                    break;
        }
    }

    private void mostrarPlacar(){
        System.out.println("Placar do jogo:");
        for (Jogador jogador : partida.getJogadores()){
            System.out.printf("Jogador: %10s | pontos: %-3d | peças: %-2d | gatos: %-2d%n",
                    jogador.getNome(), jogador.getPontuacao(), jogador.quantidadePecas(), jogador.getErros());
        }
    }


    private void finalizarRodada() {
        Jogador vencedor;

        if (partida.terminouRodada()) {
            vencedor = partida.getVencedor();
            System.out.println("\n Fim da rodada!");
        } else {
            vencedor = partida.getVencedorRodadaTravada();
            System.out.println("\n Rodada travada!");
        }

        int pontos = partida.calcularPontosRodada(vencedor);

        System.out.println("Vencedor: " + vencedor.getNome());
        System.out.println("Pontos ganhos: " + pontos);

        mostrarPlacar();
    }
}



/*
private void comprarPeca(){
    if(!partida.podeComprar()){
        System.out.println(" O baralho acabou. ");
        return;
    }

    partida.comprarPeca();

    System.out.println("""
            Você comprou uma peça.
            Agora escolha jogar ou passar.
            """);
}
private void passarVez(){
    partida.passarVez();
    System.out.println("Você passou a vez.");
}
*/


