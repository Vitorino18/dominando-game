import br.com.dominando.model.Baralho;
import br.com.dominando.model.Jogador;
import br.com.dominando.model.Partida;
import br.com.dominando.model.Peca;
import br.com.dominando.controller.GameController;

import java.util.List;

public class gametest {
    public static void main(String[] args) {
        Partida partida = new Partida(List.of("Vitor", "Yago"));

        GameController gamer = new GameController(partida);

        gamer.iniciar();
    }
}
