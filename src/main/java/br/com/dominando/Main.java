
package br.com.dominando;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Main extends Application{
    @Override
    public void start (Stage stage){

        // texto que pode ser criado //
        Label titulo = new Label("dominado");

        StackPane raiz = new StackPane();
        raiz.getChildren().add(titulo);

        // seta como pode ser a tela e o que pode ser colocada nela //
        Scene cena = new Scene(raiz,1000, 800);

        // cria a tela e o que vai ser impressa nela. //
        stage.setTitle("jogo");
        stage.setScene(cena);
        stage.show();
        }

    // inicia o aplicativo //
    public static void main(String[] args) {
        launch();
    }
}