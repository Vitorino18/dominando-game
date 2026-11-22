package br.com.dominando.controller;

import br.com.dominando.model.Jogador;
import br.com.dominando.model.Partida;
import br.com.dominando.model.PartidaGeral;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MenuDoJogoController {
    private  final Scanner scan = new Scanner(System.in);

    private String nomeJogador = "jogador";

    public void iniciarMenu(){

        while (true){
            mostraMenu();
           String escolha = scan.nextLine().trim();

           switch (escolha) {
               case "1" :
                   iniciarJogo();
                   break;
               case "2":
                   abrirOpcoes();
                   break;
               case "3":
                   mudarNome();
                   break;
               case "4":
                   sair();
                   return;

               default:
                   System.out.println("opção invalida");
           }
        }
    }


    public void mostraMenu(){
        System.out.println("""
                =======================
                        Desce Uma
                =======================
                """);
        System.out.println("Jogador: " + nomeJogador);

        System.out.println("""
            1 - Iniciar Jogo
            2 - Opções
            3 - Alterar Nome
            4 - Sair
            """);
        System.out.println("Escolha: " );
    }

    public void mudarNome(){
        System.out.println("Digite o seu nome");

        String nome = scan.nextLine().trim();
        if (nome.isBlank()) {
            System.out.println("O nome não pode ser vazio.");
            return;
        }

        nomeJogador = nome;
        System.out.println("Nome alterado com sucesso!");
    }

    public void iniciarJogo(){
        List<String> nomesJogadores = new ArrayList<>();
        nomesJogadores.add(nomeJogador);

        int quantidade = solicitarQuantidadeJogadores();

        for (int i = 2; i <= quantidade; i++) {
            String nome;

            do {
                System.out.print("Nome do jogador " + i + ": ");
                nome = scan.nextLine().trim();

                if (nome.isBlank()) {
                    System.out.println("O nome não pode ser vazio.");
                } else if (nomesJogadores.contains(nome)) {
                    System.out.println("Esse nome já está sendo usado.");
                }
            } while (nome.isBlank() || nomesJogadores.contains(nome));
            nomesJogadores.add(nome);
        }

        PartidaGeral partidaGeral = new PartidaGeral(nomesJogadores);
        GameController jogo = new GameController(partidaGeral, scan);

       jogo.iniciar();
    }

    private int solicitarQuantidadeJogadores() {
        while (true) {
            System.out.print("Quantidade de jogadores (2 a 4): ");
            String entrada = scan.nextLine().trim();

            try {
                int quantidade = Integer.parseInt(entrada);

                if (quantidade >= 2 && quantidade <= 4) {
                    return quantidade;
                }
            } catch (NumberFormatException e) {
            }

            System.out.println("Digite uma quantidade válida entre 2 e 4.");
        }
    }

    private void abrirOpcoes() {
        System.out.println("""
    ===== OPÇÕES =====
    Volume (Em breve)
    Idioma (Em breve)
    Tema (Em breve)

    Pressione ENTER para voltar.
    """);
        scan.nextLine();
    }

    private void sair() {
        System.out.println("""
    Obrigado por jogar Desce Uma!
    Até a próxima.
    """);
    }



}
