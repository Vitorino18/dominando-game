package br.com.dominando.frontend.screens;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class MatchScreen extends ScreenAdapter {
    private static final float WIDTH = 1920f;
    private static final float HEIGHT = 1080f;
    private final Stage stage;
    private final Texture backgroundTexture;
    private Image background;
    private float entranceTime;

    private void updateEntrance() {
        float fadeProgress = Math.min(entranceTime / 0.8f, 1f);
        float fade = fadeProgress * (3f - 2f * fadeProgress);

        background.getColor().a = fade;

        float moveProgress = Math.min(entranceTime / 2f, 1f);
        float movement = moveProgress * moveProgress * (3f - 2f * moveProgress);
        float scale = 1f + 0.12f * movement;
        float width = WIDTH * scale;
        float height = HEIGHT * scale;
        float x = (WIDTH - width) * 0.5f;
        float y = (HEIGHT - height) * 0.45f;

        background.setBounds(x, y, width, height);
    }
    public MatchScreen() {
        backgroundTexture = new Texture(Gdx.files.internal("ui/match/cenario-chegada.png"));
        backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        stage = new Stage(new FitViewport(WIDTH, HEIGHT));

        background = new Image(backgroundTexture);
        background.setBounds(0, 0, WIDTH, HEIGHT);
        background.setTouchable(Touchable.disabled);

        stage.addActor(background);
    }

    @Override
    public void show() {
        entranceTime = 0f;
        updateEntrance();

        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        float dt = Math.min(delta, 1f / 30f);

        entranceTime = Math.min(entranceTime + dt, 2f);
        updateEntrance();

        ScreenUtils.clear(0f, 0f, 0f, 1f);

        stage.getViewport().apply();
        stage.act(Math.min(delta, 1f / 30f));
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
        hide();
        stage.dispose();
        backgroundTexture.dispose();
    }
}
