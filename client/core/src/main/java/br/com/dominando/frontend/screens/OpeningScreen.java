package br.com.dominando.frontend.screens;
import br.com.dominando.frontend.settings.GameSettings;
import br.com.dominando.frontend.ui.OpeningDialogs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;

import br.com.dominando.frontend.ui.OpeningButtons;

public class OpeningScreen extends ScreenAdapter {

    private static final float WIDTH = 1920f;
    private static final float HEIGHT = 1080f;

    private final Color background = Color.valueOf("F0DCB2");
    private final Stage stage;
    private final Texture backgroundTexture;
    private final Image backgroundImage;
    private final Texture logoTexture;
    private final OpeningButtons buttons;
    private final OpeningDialogs openingDialogs;
    private final Group loadingGroup = new Group();
    private final Image[] loadingCaps = new Image[3];
    private final Texture loadingCapTexture;
    private final BitmapFont loadingFont;
    private final Actor enterButton;
    private final Actor settingsButton;
    private boolean simulateConnectionFailure = false;

    public OpeningScreen(Runnable onEnter) {
        stage = new Stage(new ExtendViewport(WIDTH, HEIGHT));

        backgroundTexture = loadTexture("ui/opening/fundo-engradados.png");
        logoTexture = loadTexture("ui/opening/logo-desce-uma.png");

        backgroundImage = new Image(backgroundTexture);
        backgroundImage.setScaling(Scaling.fill);
        backgroundImage.setBounds(0f, 0f, WIDTH, HEIGHT);
        backgroundImage.setTouchable(Touchable.disabled);
        stage.addActor(backgroundImage);
        addImage(logoTexture, 534.002f, 70f, 851.775f, 620f);

        openingDialogs = new OpeningDialogs(new GameSettings(), () -> startLoading(onEnter));

        buttons = new OpeningButtons();
        enterButton = buttons.createEnterButton(() -> startLoading(onEnter));
        settingsButton = buttons.createSettingsButton(() -> openingDialogs.open(stage));
        stage.addActor(enterButton);
        stage.addActor(settingsButton);

        loadingCapTexture = loadTexture("ui/waiting/tampinha.png");

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/NunitoSans-Regular.ttf"));

        try {
            FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.size = 28;
            parameter.minFilter = Texture.TextureFilter.Linear;
            parameter.magFilter = Texture.TextureFilter.Linear;
            loadingFont = generator.generateFont(parameter);
        } finally {
            generator.dispose();
        }

        float capHeight = 40f * 64f / 60f;

        for (int i = 0; i < loadingCaps.length; i++) {
            Image cap = new Image(loadingCapTexture);
            cap.setScaling(Scaling.stretch);
            cap.setBounds(888f + i * 52f, 362f - capHeight, 40f, capHeight);

            loadingCaps[i] = cap;
            loadingGroup.addActor(cap);
        }

        Label loadingLabel = new Label("PREPARANDO O BAR...", new Label.LabelStyle(loadingFont, Color.valueOf("3f2412")));
        loadingLabel.setAlignment(Align.center);
        loadingLabel.setBounds(734f, 257f, 452f, 40f);
        loadingGroup.addActor(loadingLabel);
        loadingGroup.setTouchable(Touchable.disabled);
        loadingGroup.setVisible(false);
        stage.addActor(loadingGroup);
    }

    private void startLoading(Runnable onEnter) {
        enterButton.setVisible(false);
        settingsButton.setVisible(false);
        loadingGroup.setVisible(true);

        for (int i = 0; i < loadingCaps.length; i++) {
            Image cap = loadingCaps[i];
            cap.addAction(Actions.sequence(
                Actions.delay(i * 0.30f),
                Actions.moveBy(0f, 12f, 0.15f),
                Actions.moveBy(0f, -12f, 0.15f)
            ));
        }

        loadingGroup.addAction(Actions.sequence(
            Actions.delay(1.2f),
            Actions.run(() -> finishLoading(onEnter))
        ));
    }

    private void finishLoading(Runnable onEnter) {
        if (simulateConnectionFailure) {
            simulateConnectionFailure = false;
            resetLoading();
            openingDialogs.openConnectionError(stage);
            return;
        }

        onEnter.run();
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
        if (width <= 0 || height <= 0) {
            return;
        }

        stage.getViewport().update(width, height, false);

        stage.getCamera().position.set(WIDTH / 2f, HEIGHT / 2f, 0f);
        stage.getCamera().update();

        float visibleWidth = stage.getViewport().getWorldWidth();
        float visibleHeight = stage.getViewport().getWorldHeight();

        backgroundImage.setBounds((WIDTH - visibleWidth) / 2f, (HEIGHT - visibleHeight) / 2f, visibleWidth, visibleHeight);
        settingsButton.setPosition((WIDTH + visibleWidth) / 2f - 156f, (HEIGHT + visibleHeight) / 2f - 144f);
    }

    private void resetLoading() {
        loadingGroup.clearActions();
        loadingGroup.setVisible(false);
        enterButton.setVisible(true);
        settingsButton.setVisible(true);

        for (Image cap : loadingCaps) {
            cap.clearActions();
            cap.setY(HEIGHT - 718f - cap.getHeight());
        }
    }

    @Override
    public void hide() {
        openingDialogs.close();
        resetLoading();

        if (Gdx.input.getInputProcessor() == stage) {
            Gdx.input.setInputProcessor(null);
        }
    }

    @Override
    public void dispose() {
        hide();

        openingDialogs.dispose();
        stage.dispose();
        buttons.dispose();
        backgroundTexture.dispose();
        logoTexture.dispose();
        loadingFont.dispose();
        loadingCapTexture.dispose();
    }
}
