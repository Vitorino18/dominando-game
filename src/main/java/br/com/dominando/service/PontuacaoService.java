package br.com.dominando.service;

import br.com.dominando.model.Jogador;
import java.util.List;
import java.util.ArrayList;

// pontos da rodada //
public class PontuacaoService {

    public List<Jogador> vencedorRodadaTravada(List<Jogador> jogadores) {
        int menorPontuacao = Integer.MAX_VALUE;
        List<Jogador> vencedores = new ArrayList<>();

        for (Jogador jogador : jogadores) {
            int pontos = jogador.valorTotalMao();

            if (pontos < menorPontuacao) {
                menorPontuacao = pontos;
                vencedores.clear();
                vencedores.add(jogador);
            } else if (pontos == menorPontuacao) {
                vencedores.add(jogador);
            }
        }
        return vencedores;
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
