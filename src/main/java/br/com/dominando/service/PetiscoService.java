package br.com.dominando.service;

import br.com.dominando.model.*;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class PetiscoService {
    public UsoPetisco utilizarPetisco(Partida partida, Jogador jogador, TipoPetiscos tipo, int numeroRodada, FasePartida fase, Integer indicePeca){
        if (partida == null || jogador == null) {
            return UsoPetisco.resultado(ResultadoUsoPetisco.JOGADOR_INVALIDO);
        }

        if (tipo == null) {
            return UsoPetisco.resultado(ResultadoUsoPetisco.PETISCO_INVALIDO);
        }

        InventarioPetiscos inventario = jogador.getInventarioPetiscos();

        if (!inventario.podeUsarPetisco(tipo, numeroRodada)) {
            return UsoPetisco.resultado(ResultadoUsoPetisco.PETISCO_INDISPONIVEL);
        }

        UsoPetisco uso;

        if (tipo == TipoPetiscos.TORRESMO) {
            if (fase != FasePartida.PREPARACAO_RODADA || partida.primeiraJogadaRealizada()) {
                return UsoPetisco.resultado(ResultadoUsoPetisco.FASE_INVALIDA);
            }
            uso = utilizarTorresmo(partida, jogador);
        } else {
            if (fase != FasePartida.EM_ANDAMENTO) {
                return UsoPetisco.resultado(ResultadoUsoPetisco.FASE_INVALIDA);
            }
            if (partida.getJogadorVez() != jogador) {
                return UsoPetisco.resultado(ResultadoUsoPetisco.NAO_E_SUA_VEZ);
            }

            if (partida.isTurnoIniciado()) {
                return UsoPetisco.resultado(ResultadoUsoPetisco.TURNO_JA_INICIADO);
            }

            uso = switch (tipo) {
                case OVO_COLORIDO -> utilizarOvoColorido(partida, jogador);
                case COXINHA -> utilizarCoxinha(partida, jogador, indicePeca);
                default -> UsoPetisco.resultado(ResultadoUsoPetisco.PETISCO_INVALIDO);
            };
        }
        // Consome somente se o efeito foi realizado //
        if (uso.sucesso()) {
            inventario.consumirPetisco(tipo, numeroRodada);
        }
        return uso;
    }

    // OVO COLORIDO //
    private UsoPetisco utilizarOvoColorido(Partida partida, Jogador jogador) {
        List<Jogador> adversarios = partida.getJogadores().stream().filter(j -> j != jogador).filter(j -> j.quantidadePecas() > 0).toList();

        if (adversarios.isEmpty()) {
            return UsoPetisco.resultado(ResultadoUsoPetisco.ADVERSARIO_SEM_PECAS);
        }

        Jogador adversario = adversarios.get(ThreadLocalRandom.current().nextInt(adversarios.size()));

        int indice = ThreadLocalRandom.current().nextInt(adversario.quantidadePecas());
        Peca peca = adversario.getPeca(indice);

        Peca pecaRevelada = new Peca(peca.getLadoA(), peca.getLadoB());

        return UsoPetisco.revelarPeca(pecaRevelada);
    }


    // COXINHA //
    private UsoPetisco utilizarCoxinha(Partida partida, Jogador jogador, Integer indicePeca) {
        if (indicePeca == null || jogador.getPeca(indicePeca) == null) {
            return UsoPetisco.resultado(ResultadoUsoPetisco.PECA_INVALIDA);
        }

        Baralho baralho = partida.getBaralho();

        if (baralho.quantidadePecas() == 0) {
            return UsoPetisco.resultado(ResultadoUsoPetisco.BARALHO_VAZIO);
        }

        // Retira uma peça aleatória do cava //
        Peca novaPeca = baralho.comprarPecaAleatoria();

        // Substitui a peça escolhida //
        Peca pecaAntiga = jogador.substituirPeca(indicePeca, novaPeca);

        // Devolve a peça escolhida para o cava //
        baralho.devolverPeca(pecaAntiga);
        baralho.embaralhar();

        return UsoPetisco.resultado(ResultadoUsoPetisco.PETISCO_UTILIZADO);
    }

    // TORRESMO //
    private UsoPetisco utilizarTorresmo(Partida partida, Jogador jogador) {
        Baralho baralho = partida.getBaralho();
        if (baralho.quantidadePecas() == 0) {
            return UsoPetisco.resultado(ResultadoUsoPetisco.BARALHO_VAZIO);
        }

        int quantidade = jogador.quantidadePecas();

        // Retira todas as peças da mão //
        List<Peca> pecasAntigas = jogador.retirarTodasPecas();
        // Devolve as peças à cava //
        baralho.devolverPecas(pecasAntigas);
        // Embaralha todas as peças disponíveis //
        baralho.embaralhar();

        // Distribui a mesma quantidade de peças //
        for (int i = 0; i < quantidade; i++) {
            jogador.comprarDoBaralho(baralho);
        }

        return UsoPetisco.resultado(ResultadoUsoPetisco.PETISCO_UTILIZADO);
    }
}
