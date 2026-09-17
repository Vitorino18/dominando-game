package br.com.dominando.service;

import br.com.dominando.model.Jogador;
import java.util.List;

public class PontuacaoService {

    public Jogador vencedorRodadaTravada(List<Jogador> jogadores) {
        Jogador vencedor = jogadores.getFirst();
        for (Jogador jogador : jogadores) {
            if (jogador.valorTotalMao() < vencedor.valorTotalMao()) {
                vencedor = jogador;
            }
        }

        return vencedor;
    }

    public int calcularPontosRodada(List<Jogador> jogadores, Jogador vencedor) {
        int pontos = 0;

        for (Jogador jogador : jogadores) {
            if (!jogador.equals(vencedor)) {
                pontos += jogador.valorTotalMao();
            }
        }
        vencedor.adicionarPontos(pontos);

        return pontos;
    }

}
