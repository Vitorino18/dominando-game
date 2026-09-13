import org.junit.jupiter.api.Test;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;
import br.com.dominando.model.Peca;
import br.com.dominando.model.Jogador;
import br.com.dominando.model.Baralho;

public class BaralhoTest {
    @Test
    void deveCriar28Pecas() {
        Baralho baralho = new Baralho();
        assertEquals(28, baralho.quantidadePecas());
    }

    @Test
    void naoDeveExistirPecasDuplicadas() {
        Baralho baralho = new Baralho();
        HashSet<Peca> conjunto = new HashSet<>(baralho.getPecas());
        assertEquals(28, conjunto.size());
    }

    @Test
    void comprarPecaDiminuiQuantidade() {
        Baralho baralho = new Baralho();
        baralho.comprarPeca();
        assertEquals(27, baralho.quantidadePecas());
    }

    @Test
    void comprarpecadobaralho(){
        Baralho baralho = new Baralho();
        Jogador jogador = new Jogador("vitor");
        jogador.comprarDoBaralho(baralho);
    }
}
