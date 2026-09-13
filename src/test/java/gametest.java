import br.com.dominando.model.Baralho;
import br.com.dominando.model.Jogador;
import br.com.dominando.model.Peca;

public class gametest {
    public static void main(String[] args) {
        Baralho baralho = new Baralho();
        baralho.embaralhar();

        Jogador jogador1 = new Jogador("Vitor");
        Jogador jogador2 = new Jogador("yago");

        for (int i = 0; i < 7; i++) {
            jogador1.comprarDoBaralho(baralho);
            jogador2.comprarDoBaralho(baralho);
        }

        System.out.println(jogador1);
        System.out.println(jogador2);


       Peca primeirapeca = jogador1.getMao().get(0);
       Peca primeirapeca2 = jogador2.getMao().get(0);

       System.out.println("valor total da peimeira peça do " + jogador1.getNome() + " " + primeirapeca.valorTotal());
       System.out.println("valor total da peimeira peça do " + jogador2.getNome() + " " + primeirapeca2.valorTotal());

       System.out.println("valores totais de cada peca de " + jogador1.getNome() + ":");
       System.out.print( "pecas: ");
       for (Peca pecas : jogador1.getMao()){
           System.out.print(pecas.valorTotal() + " ");
       }
       System.out.println();

       System.out.println("valores totais de cada peca de " + jogador2.getNome() + ":");
       System.out.print( "pecas: ");
       for (Peca pecas : jogador2.getMao()){
           System.out.print(pecas.valorTotal() + " ");
       }
        System.out.println();

       System.out.println("Peças restantes no baralho: " + baralho.quantidadePecas());
    }
}
