package br.com.dominando.frontend;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Objects;

public class FrontendApplication extends Application {
    @Override
    public void start(Stage stage) {
        Label titulo = new Label("Desçe Uma - A Game Bar");
        titulo.getStyleClass().add("prototype-title");

        Label subtitulo = new Label("prototipo visual do frontend");
        subtitulo.getStyleClass().add("prototype-subtitle");

        VBox conteudo = new VBox(8, titulo, subtitulo);
        conteudo.setAlignment(Pos.CENTER);

        StackPane raiz = new StackPane(conteudo);
        raiz.getStyleClass().add("app-root");

        Scene cena = new Scene(raiz, 1280,720);

        String css = Objects.requireNonNull(
                getClass().getResource("/br/com/dominando/frontend/css/app.css"), "Arquivo CSS nao encontrado"
        ).toExternalForm();

        cena.getStylesheets().add(css);

        stage.setTitle("Desce Uma - A Game Bar");
        stage.setMinWidth(960);
        stage.setMinHeight(540);
        stage.setScene(cena);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
