package br.com.dominando.server;

public class ConfiguracaoServidor {
    public static final int MAX_SALAS = 50;
    public static final int MAX_JOGADORES_POR_SALA = 2;

    public static final int MAX_TAMANHO_NOME = 40;
    public static final int MAX_STREAMS_POR_JOGADOR = 2;

    public static final int TEMPO_SALA_AGUARDANDO_MINUTOS = 1; //
    public static final int TEMPO_DESCONECTADOS_MINUTOS = 5;
    public static final int TEMPO_INATIVIDADE_MINUTOS = 15;
    public static final int TEMPO_SALA_ENCERRADA_MINUTOS = 2;

    public static final int INTERVALO_LIMPEZA_SEGUNDOS = 5; //

    public static final int MAX_REQUISICOES_POR_SEGUNDO = 20;

    private ConfiguracaoServidor() {
    }

}
