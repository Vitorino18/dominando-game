package br.com.dominando.frontend.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class OpeningScreen extends ScreenAdapter {

    private static final float WIDTH = 1920f;
    private static final float HEIGHT = 1080f;

    private final Color background = Color.valueOf("F0DCB2");
    private final Stage stage;
    private final BitmapFont font;

    public OpeningScreen() {
        stage = new Stage(new FitViewport(WIDTH, HEIGHT));
        font = new BitmapFont();

        Label.LabelStyle titleStyle =
            new Label.LabelStyle(font, Color.valueOf("234730"));

        Label title = new Label("Desce Uma", titleStyle);
        title.setFontScale(4f);

        Label subtitle = new Label("Base da interface funcionando", titleStyle);
        subtitle.setFontScale(2f);

        Table layout = new Table();
        layout.setFillParent(true);

        layout.add(title).padBottom(32f);
        layout.row();
        layout.add(subtitle);

        stage.addActor(layout);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(background);

        stage.getViewport().apply();
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void hide() {
        if (Gdx.input.getInputProcessor() == stage) {
            Gdx.input.setInputProcessor(null);
        }
    }

    @Override
    public void dispose() {
        stage.dispose();
        font.dispose();
    }
}
