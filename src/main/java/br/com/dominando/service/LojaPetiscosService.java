package br.com.dominando.service;

import br.com.dominando.model.*;
public class LojaPetiscosService {

    private final LojaPetiscos loja;
    public LojaPetiscosService(LojaPetiscos loja) {
        this.loja = loja;
    }

    public ResultadoPetiscosLoja comprarPetisco(Jogador jogador, TipoPetiscos tipo) {
        if (!loja.isAberta()) {
            return ResultadoPetiscosLoja.LOJA_FECHADA;
        }
        if (jogador == null) {
            return ResultadoPetiscosLoja.JOGADOR_INVALIDO;
        }
        if (tipo == null) {
            return ResultadoPetiscosLoja.PETISCO_INVALIDO;
        }

        InventarioPetiscos inventario = jogador.getInventarioPetiscos();
        int numeroLoja = loja.getNumeroLoja();

        // Verifica se já comprou nesta loja //
        if (inventario.comprouNestaLoja(numeroLoja)) {
            return ResultadoPetiscosLoja.JA_COMPROU_NESTA_LOJA;
        }

        // Verifica se comprou o mesmo na loja anterior //
        if (inventario.comprouMesmoPetiscoNaLojaAnterior(tipo, numeroLoja)) {
            return ResultadoPetiscosLoja.PETISCO_REPETIDO;
        }

        // Verifica se já possui um petisco //
        if (inventario.possuiPetisco()) {
            return ResultadoPetiscosLoja.JA_POSSUI_PETISCO;
        }

        // Verifica o saldo do jogador //
        if (!jogador.possuiSaldo(tipo.getCusto())) {
            return ResultadoPetiscosLoja.SALDO_INSUFICIENTE;
        }
        
        inventario.registrarCompra(tipo, numeroLoja, loja.getProximaRodada());
        jogador.gastarPontosPetiscos(tipo.getCusto());

        return ResultadoPetiscosLoja.COMPRA_REALIZADA;
    }

    public LojaPetiscos getLoja() {
        return loja;
    }
}
