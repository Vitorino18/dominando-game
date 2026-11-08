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
import com.badlogic.gdx.scenes.scene2d.Group;

import br.com.dominando.frontend.ui.DominoAssets;
import br.com.dominando.frontend.ui.DominoTile;

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

    private final DominoAssets dominoAssets;
    private final Group tilesLayer;


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

        tilesLayer.setVisible(entranceTime >= 4.4f);
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

        dominoAssets = new DominoAssets();

        tilesLayer = new Group();
        tilesLayer.setSize(WIDTH,HEIGHT);
        tilesLayer.setTouchable(Touchable.disabled);
        tilesLayer.setVisible(false);

        stage.addActor(tilesLayer);
        createDemoTiles();
    }

    private void createDemoTiles() {
        tilesLayer.clearChildren();

        int[][] demoHand = {
            {6, 6},
            {0, 6},
            {2, 6},
            {1, 3},
            {2, 4},
            {3, 5},
            {0, 0}
        };

        for (int i = 0; i < demoHand.length; i++) {
            DominoTile tile = new DominoTile(dominoAssets, demoHand[i][0], demoHand[i][1]);

            float x = 616f + i * 100f;
            float y = HEIGHT - 820f - 164f;

            tile.setBounds(x, y, 88f, 164f);
            tilesLayer.addActor(tile);
        }

        float opponentWidth = 55f;
        float opponentHeight = 102.5f;
        float gap = 12f;

        float totalWidth = 7f * opponentWidth + 6f * gap;
        float startX = (WIDTH - totalWidth) / 2f;

        for (int i = 0; i < 7; i++) {
            DominoTile tile = new DominoTile(dominoAssets, 0, 0);
            tile.setFaceDown(true);

            tile.setBounds(startX + i * (opponentWidth + gap), HEIGHT - 132f - opponentHeight, opponentWidth, opponentHeight);
            tilesLayer.addActor(tile);
        }
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
        dominoAssets.dispose();
    }
}
