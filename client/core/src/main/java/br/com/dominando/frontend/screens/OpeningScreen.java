package br.com.dominando.frontend.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import org.w3c.dom.Text;

public class OpeningScreen extends ScreenAdapter {

    private static final float WIDTH = 1920f;
    private static final float HEIGHT = 1080f;

    private final Color background = Color.valueOf("F0DCB2");
    private final Stage stage;
    private final Texture backgroundTexture;
    private final Texture logoTexture;

    public OpeningScreen() {
        stage = new Stage(new FitViewport(WIDTH, HEIGHT));

        backgroundTexture = loadTexture("ui/opening/fundo-engradados.png");
        logoTexture = loadTexture("ui/opening/logo-desce-uma.png");

        addImage(backgroundTexture, 0f, 0f, 1920f, 1081f);
        addImage(logoTexture, 534.002f, 70f, 851.775f, 620f);
    }

    private Texture loadTexture(String path) {
        Texture texture = new Texture(Gdx.files.internal(path));

        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        return texture;
    }

    private void addImage(Texture texture, float x, float top, float width, float height) {
        Image image = new Image(texture);
        image.setScaling(Scaling.stretch);

        float y = HEIGHT - top - height;

        image.setBounds(x, y, width, height);
        image.setTouchable(Touchable.disabled);

        stage.addActor(image);
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
        backgroundTexture.dispose();
        logoTexture.dispose();
    }
}
