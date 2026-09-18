package br.com.dominando.controller;

import br.com.dominando.model.Jogador;
import br.com.dominando.model.Partida;
import java.util.List;
import java.util.Scanner;

public class MenuDoJogoController {
    private  final Scanner scan = new Scanner(System.in);

    private String nomeJogador = "jogador";

    public void iniciarMenu(){

        while (true){
            mostraMenu();
           String escolha = scan.nextLine();

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

        if (!nome.isEmpty()){
            nomeJogador = nome;
        }
    }

    public void iniciarJogo(){
       Partida partida = new Partida(List.of(nomeJogador, "bot"));
       GameController jogo = new GameController(partida);
       jogo.iniciar();
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
    Obrigado por jogar Dominando!
    Até a próxima.
    """);
    }



}
