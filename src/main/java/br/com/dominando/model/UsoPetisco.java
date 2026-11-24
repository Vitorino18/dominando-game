package br.com.dominando.model;
// record para o petisco ovo colorido //
public record UsoPetisco( ResultadoUsoPetisco resultado, Peca pecaRevelada, int duracaoRevelacaoSegundos) {

    public static UsoPetisco resultado(ResultadoUsoPetisco resultado) {
        return new UsoPetisco(resultado, null, 0);
    }

    public static UsoPetisco revelarPeca(Peca peca) {
        return new UsoPetisco(ResultadoUsoPetisco.PETISCO_UTILIZADO, peca, 5);
    }

    public boolean sucesso() {
        return resultado == ResultadoUsoPetisco.PETISCO_UTILIZADO;
    }
}
