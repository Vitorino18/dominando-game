package br.com.dominando.model;

public enum ResultadoJogada {
    // JOGADAS //
    JOGADA_VALIDA,
    GATO,
    NAO_POSSUI_PECA,
    POSSUI_JOGADA_VALIDA,

    // BARALHO //
    COMPROU_JA_REALIZADA,
    BARALHO_VAZIO,
    COMPRA_REALIZADA,

    // PASSAR //
    NAO_PODE_PASSAR,
    VEZ_PASSADA,
    NAO_PODE_PASSAR_SEM_COMPRAR,

    // FIM DA PARTIDA //
    PARTIDA_ENCERRADA
}
