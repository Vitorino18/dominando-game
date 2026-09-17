import static org.junit.jupiter.api.Assertions.*;

import br.com.dominando.service.MesaService;
import org.junit.jupiter.api.Test;

import br.com.dominando.model.LadoMesa;
import br.com.dominando.model.Mesa;
import br.com.dominando.model.Peca;

public class MesaTest {

    @Test
    void primeiraJogadaSempreValida() {
        Mesa mesa = new Mesa();
        MesaService mesaService = new MesaService(mesa);

        assertTrue( mesaService.jogarPeca(new Peca(6,6), LadoMesa.DIREITA));
    }

    @Test
    void deveAceitarJogadaCompativel() {
        Mesa mesa = new Mesa();
        MesaService mesaService = new MesaService(mesa);
        mesaService.jogarPeca(new Peca(6,6), LadoMesa.DIREITA);

        assertTrue(mesaService.jogarPeca(new Peca(6,3), LadoMesa.DIREITA));
    }

    @Test
    void deveRecusarJogadaInvalida() {
        Mesa mesa = new Mesa();
        MesaService mesaService = new MesaService(mesa);

        mesaService.jogarPeca(new Peca(3,6), LadoMesa.DIREITA);

        assertFalse(mesaService.jogarPeca(new Peca(2,3), LadoMesa.DIREITA));
    }
}
