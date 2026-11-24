package br.com.dominando.model;

public class InventarioPetiscos {
    private TipoPetiscos petiscoAtual;
    private TipoPetiscos petiscoUltimaLoja;

    private int ultimaLojaCompra;
    private int rodadaDisponivel;


    public InventarioPetiscos(){
        this.petiscoAtual = null;
        this.petiscoUltimaLoja = null;
        this.ultimaLojaCompra = 0;
        this.rodadaDisponivel = 0;
    }

    public boolean comprouNestaLoja(int numeroLoja) {
        return ultimaLojaCompra == numeroLoja;
    }

    public boolean comprouMesmoPetiscoNaLojaAnterior(TipoPetiscos tipo, int numeroLoja) {
        return petiscoUltimaLoja == tipo && ultimaLojaCompra == numeroLoja - 1;
    }

    public boolean possuiPetisco() {
        return petiscoAtual != null;
    }

    public boolean podeUsarPetisco(TipoPetiscos tipo, int numeroRodada) {
        return tipo != null && petiscoAtual == tipo && rodadaDisponivel == numeroRodada;
    }

    public void registrarCompra(TipoPetiscos tipo, int numeroLoja, int numeroRodadaDisponivel) {
        if (tipo == null) {
            throw new IllegalArgumentException("Petisco inválido.");
        }

        if (possuiPetisco()) {
            throw new IllegalStateException("O jogador já possui um petisco.");
        }

        if (comprouNestaLoja(numeroLoja)) {
            throw new IllegalStateException("O jogador já comprou nesta loja.");
        }

        if (comprouMesmoPetiscoNaLojaAnterior(tipo, numeroLoja)) {
            throw new IllegalStateException("Não pode comprar o mesmo petisco em duas lojas consecutivas.");
        }

        petiscoAtual = tipo;
        petiscoUltimaLoja = tipo;
        ultimaLojaCompra = numeroLoja;
        rodadaDisponivel = numeroRodadaDisponivel;
    }

    public void consumirPetisco(TipoPetiscos tipo, int numeroRodada) {
        if (!podeUsarPetisco(tipo, numeroRodada)) {
            throw new IllegalStateException("Petisco indisponível para utilização.");
        }

        petiscoAtual = null;
        rodadaDisponivel = 0;
    }

    public void expirarPetisco(int rodadaEncerrada) {
        if (possuiPetisco() && rodadaDisponivel <= rodadaEncerrada) {

            petiscoAtual = null;
            rodadaDisponivel = 0;
        }
    }


    public TipoPetiscos getPetiscoAtual() {
        return petiscoAtual;
    }
    public TipoPetiscos getPetiscoUltimaLoja() {
        return petiscoUltimaLoja;
    }
    public int getUltimaLojaCompra() {
        return ultimaLojaCompra;
    }
    public int getRodadaDisponivel() {
        return rodadaDisponivel;
    }
}
