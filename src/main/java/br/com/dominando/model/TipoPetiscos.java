package br.com.dominando.model;

public enum TipoPetiscos {
    OVO_COLORIDO("Ovo Colorido", 2, "Revela uma peça aleatória do adversário por 5 segundos."),
    COXINHA("Coxinha", 4, "Troca uma peça da mão por uma peça aleatória do cava."),
    TORRESMO("Torresmo", 4, "Permite embaralhar novamente a mão e redistribuir as peças para a mão.");

    private final String nome;
    private final int custo;
    private final String descricao;

    TipoPetiscos(String nome, int custo, String descricao) {
        this.nome = nome;
        this.custo = custo;
        this.descricao = descricao;
    }

    public String getNome() {
        return nome;
    }
    public int getCusto() {
        return custo;
    }
    public String getDescricao() {
        return descricao;
    }
}
