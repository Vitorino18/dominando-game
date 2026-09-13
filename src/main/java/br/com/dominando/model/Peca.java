package br.com.dominando.model;
import java.util.Objects;

public class Peca {
    private final int ladoA;
    private final int ladoB;

    public Peca(int ladoA, int ladoB){
        this.ladoA = ladoA;
        this.ladoB = ladoB;
    }

    public int getLadoA() {
        return ladoA;
    }
    public int getLadoB() {
        return ladoB;
    }

    public boolean combina(int numero){
        return ladoA == numero || ladoB == numero;
    }

    public boolean bucha(){
        return ladoA == ladoB;
    }

    public Peca inverter(){
        return new Peca(ladoB, ladoA);
    }

    public int valorTotal(){
         return ladoA + ladoB;
    }


    @Override
    public String toString() {
        return "[" + ladoA + "|" + ladoB + "]";
    }

    //falta reconhercer a peça invertida como a mesma peça que a original (6-5 = 5-6) //
    @Override
    public boolean equals(Object obj ){
        if(this == obj) return true;
        if (!(obj instanceof Peca)) return false;
        Peca outra = (Peca) obj;
        return ladoA == outra.ladoA && ladoB == outra.ladoB;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ladoA, ladoB);
    }
}
