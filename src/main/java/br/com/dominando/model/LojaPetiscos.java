package br.com.dominando.model;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LojaPetiscos {
    public static final int DURACAO_SEGUNDOS = 30;
    private boolean aberta;
    private int numeroLoja;
    private int rodadaEncerrada;
    private long prazoNanos;

    public LojaPetiscos() {
        this.aberta = false;
        this.numeroLoja = 0;
        this.rodadaEncerrada = 0;
        this.prazoNanos = 0;
    }

    public void abrir(int rodadaEncerrada) {
        if (aberta) {
            throw new IllegalStateException("A loja já está aberta.");
        }
        if (rodadaEncerrada <= 0 || rodadaEncerrada % 2 != 0 || rodadaEncerrada <= this.rodadaEncerrada) {
            throw new IllegalArgumentException("Rodada inválida para abertura da loja.");
        }

        this.rodadaEncerrada = rodadaEncerrada;
        numeroLoja++;
        aberta = true;
        prazoNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(DURACAO_SEGUNDOS);
    }

    public void fechar() {
        aberta = false;
        prazoNanos = 0;
    }

    public boolean isAberta() {
        return aberta && !tempoExpirado();
    }

    public boolean tempoExpirado() {
        return aberta && System.nanoTime() - prazoNanos >= 0;
    }

    public long getTempoRestanteSegundos() {
        if (!isAberta()) {
            return 0;
        }
        long restante = Math.max(0, prazoNanos - System.nanoTime());
        return (restante + 999_999_999L) / 1_000_000_000L;
    }

    public int getNumeroLoja() {
        return numeroLoja;
    }
    public int getRodadaEncerrada() {
        return rodadaEncerrada;
    }
    public int getProximaRodada() {
        return rodadaEncerrada + 1;
    }
    public List<TipoPetiscos> getPetiscosDisponiveis() {
        return List.of(TipoPetiscos.values());
    }

}
