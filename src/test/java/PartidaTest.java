import static org.junit.jupiter.api.Assertions.*;

import br.com.dominando.model.Partida;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class PartidaTest {
    public static void main(String[] args) {

        Partida partida = new Partida(List.of("Vitor","Yago"));

        partida.iniciarPartida();

        System.out.println("Partida iniciada!");
        System.out.println(partida);
    }
}
