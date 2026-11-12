package br.com.dominando.controller;

import br.com.dominando.model.*;

import java.util.List;
import java.util.Scanner;

public class GameController {
    private final PartidaGeral partidaGeral;
    private final Scanner scan;

    public GameController(PartidaGeral partidaGeral, Scanner scan){
        this.partidaGeral = partidaGeral;
        this.scan = scan;
    }
    public void iniciar() {
        partidaGeral.inicioCompeticao();

        System.out.println("\n================================");
        System.out.println("         Desce Uma");
        System.out.println("==================================");

        while (!partidaGeral.competicaoEncerrada()) {
            Partida partida = partidaGeral.getPartidaAtual();

            if (partida.terminouRodada() || partida.rodadaTravada()) {
                finalizarRodada();
                continue;
            }

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

            if (partida.terminouRodada() || partida.rodadaTravada()) {
                finalizarRodada();
            }
        }
        mostrarCampeao();
    }

    private Partida getPartidaAtual() {
        return partidaGeral.getPartidaAtual();
    }

    private void mostrarEstadoDaPartida() {
        Partida partida = getPartidaAtual();
        System.out.println("Mesa:" + partida.getMesa().getPecasMesa());

        System.out.println("\nBaralho: " + partida.getBaralho().quantidadePecas() + " peças.");

        System.out.println("\nJogador da vez: " + partida.getJogadorVez().getNome());
    }
    
    private void mostrarMao(){
        Jogador jogador = getPartidaAtual().getJogadorVez();
        System.out.println("\nSua mão:");

        for (int i = 0; i < jogador.quantidadePecas(); i++) {
            System.out.println(i + " -> " + jogador.getPeca(i));
        }
    }
//////////////////////////////////////////
    private void mostrarMenu() {
        System.out.println("""
            Escolha uma ação:
            1 - Jogar peça
            2 - Comprar peça
            3 - Passar a vez

            """);
    }

////////////////////////////////////
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
    /////////////////////////////////////////////////////////
    private Integer escolherIndicePeca() {
        while (true) {
            System.out.print("Escolha o índice da peça: ");
            String entrada = scan.nextLine().trim();

            try {
                int indice = Integer.parseInt(entrada);

                if (indice >= 0 && indice < getPartidaAtual().getJogadorVez().quantidadePecas()) {
                    return indice;
                }

            } catch (NumberFormatException e) {

            }
            System.out.println("Índice inválido.");
        }
    }
//////////////////////////
    public ResultadoJogada executarAcao(AcaoJogo acao, Integer indicePeca, LadoMesa lado){
        Partida partida = getPartidaAtual();

        switch (acao) {
            case JOGAR_PECA:
                Peca peca = partida.getJogadorVez().getPeca(indicePeca);
                return partida.jogar(peca, lado);
            case COMPRAR_PECA:
                return partida.comprarPeca();
            case PASSAR_VEZ:
                return partida.passarVez();
            default:
                throw new IllegalArgumentException("Ação inválida.");
        }
    }
///////////////////////////////
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
/////////////////////////////////////////
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
            case POSSUI_JOGADA_VALIDA:
                System.out.println("Voce possui jogada valida e não pode comprar a peça");
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
            case NAO_PODE_PASSAR_SEM_COMPRAR:
                System.out.println("Você precisa comprar uma peça antes de passar a vez.");
                break;
            case VEZ_PASSADA :
                System.out.println("Você passou a vez.");
                break;
            case NAO_PODE_PASSAR:
                System.out.println("Você possui uma jogada valida e não pode passar a vez.");
                break;
            case PARTIDA_ENCERRADA :
                System.out.println("A rodada terminou.");
                break;
        }
    }
//////////////////////////////////////////
    private void mostrarPlacar(){
        System.out.println("Placar do jogo:");

        for (Jogador jogador : partidaGeral.getJogadores()){
            System.out.printf("Jogador: %100s | Tampinhas: %d/5 | petiscos: %d | Pontos: %d | Gatos: %-2d%n",
                    jogador.getNome(),
                    jogador.getVitorias(),
                    jogador.getPontosPetiscos(),
                    jogador.getPontuacao(),
                    jogador.getErros()
            );
        }
        System.out.println("============================");
    }
//////////////////////////////////////////////
    private void mostrarCampeao() {
        Jogador campeao = partidaGeral.getCampeao();

        System.out.println("\n========== FIM DE JOGO ==========");
        System.out.println("CAMPEÃO: " + campeao.getNome());
        System.out.println("Tampinhas: " + campeao.getVitorias());
        System.out.println("Pontos de petiscos: " + campeao.getPontosPetiscos());

        mostrarPlacar();
    }
/////////////////////////////
    private void finalizarRodada() {
        Partida partida = getPartidaAtual();
        Jogador vencedor = null;
        boolean empate = false;

        if (partida.terminouRodada()) {
            vencedor = partida.getVencedor();
        } else if (partida.rodadaTravada()) {
            List<Jogador> vencedores = partida.getVencedorRodadaTravada();

            if (vencedores.size() == 1) {
                vencedor = vencedores.getFirst();
            } else {
                empate = true;
            }
        }

        if (empate) {
            System.out.println("\nPartida empatada!");
            System.out.println("Nenhuma tampinha será concedida.");
        } else {
            System.out.println("\nVencedor da partida: " + vencedor.getNome());
        }

        // Registra a vitória ou reinicia a partida empatada.
        partidaGeral.verificarFimPartida();

        mostrarPlacar();

        if (!partidaGeral.competicaoEncerrada()) {
            System.out.println("\nIniciando nova partida...");
            System.out.println("Primeiro jogador: " + partidaGeral.getPartidaAtual().getJogadorVez().getNome());
        }
    }
}


