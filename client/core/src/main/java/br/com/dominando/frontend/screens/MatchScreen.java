package br.com.dominando.frontend.screens;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import org.w3c.dom.Text;

public class MatchScreen extends ScreenAdapter {
    private static final float WIDTH = 1920f;
    private static final float HEIGHT = 1080f;
    private final Stage stage;
    private final Texture backgroundTexture;
    private Image background;
    private float entranceTime;

    private final Texture seatedTexture;
    private final Image seatedBackground;

    private final Texture floorTexture;
    private final Texture tableTexture;
    private final Image floorBackground;
    private final Image tableImage;


    private void updateEntrance() {
        float fadeProgress = Math.min(entranceTime / 0.8f, 1f);
        float fade = fadeProgress * (3f - 2f * fadeProgress);

        background.getColor().a = fade;

        float moveProgress = Math.min(entranceTime / 2f, 1f);
        float movement = moveProgress * moveProgress * (3f - 2f * moveProgress);
        float scale = 1f + 0.12f * movement;
        float width = WIDTH * scale;
        float height = HEIGHT * scale;

        background.setBounds((WIDTH - width) * 0.5f, (HEIGHT - height) * 0.45f, width, height);

        float seatedProgress = Math.max(0f, Math.min((entranceTime - 2f) / 0.9f, 1f));
        background.setVisible(seatedProgress < 1f);

        float seatedExit = Math.max(0f, Math.min((entranceTime - 3.5f) / 0.35f, 1f));

        seatedBackground.getColor().a = smooth(seatedProgress) * (1f - smooth(seatedExit));

        float topProgress = Math.max(0f, Math.min((entranceTime - 3.85f) / 0.55f, 1f));
        float topAlpha = smooth(topProgress);
        boolean showTop = entranceTime >= 3.85f;
        floorBackground.setVisible(showTop);
        tableImage.setVisible(showTop);

        floorBackground.getColor().a = topAlpha;
        tableImage.getColor().a = topAlpha;
    }

    private float smooth(float progress) {
        return progress * progress * (3f - 2f * progress);
    }

    public MatchScreen() {
        backgroundTexture = new Texture(Gdx.files.internal("ui/match/cenario-chegada.png"));
        backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        stage = new Stage(new FitViewport(WIDTH, HEIGHT));

        background = new Image(backgroundTexture);
        background.setBounds(0, 0, WIDTH, HEIGHT);
        background.setTouchable(Touchable.disabled);

        stage.addActor(background);

        seatedTexture = new Texture(Gdx.files.internal("ui/match/cenario-sentado.png"));
        seatedTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        seatedBackground = new Image(seatedTexture);
        seatedBackground.setBounds(0f, 0f, WIDTH, HEIGHT);
        seatedBackground.setTouchable(Touchable.disabled);
        seatedBackground.getColor().a = 0f;

        stage.addActor(seatedBackground);

        floorTexture = new Texture(Gdx.files.internal("ui/match/chao-vista-superior.png"));
        floorTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        tableTexture = new Texture(Gdx.files.internal("ui/match/mesa-vista-superior.png"));
        tableTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        floorBackground = new Image(floorTexture);
        floorBackground.setBounds(0f, 0f, WIDTH, HEIGHT);
        floorBackground.setTouchable(Touchable.disabled);
        floorBackground.getColor().a = 0f;
        floorBackground.setVisible(false);

        stage.addActor(floorBackground);

        tableImage = new Image(tableTexture);
        tableImage.setBounds(404f, HEIGHT - 25f - 1053f, 1140f, 1053f);
        tableImage.setTouchable(Touchable.disabled);
        tableImage.getColor().a = 0f;
        tableImage.setVisible(false);

        stage.addActor(tableImage);
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

        entranceTime = Math.min(entranceTime + dt, 4.4f);
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
        seatedTexture.dispose();
        floorTexture.dispose();
        tableTexture.dispose();
    }
}
