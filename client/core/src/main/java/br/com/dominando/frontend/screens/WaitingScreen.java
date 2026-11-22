package br.com.dominando.frontend.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import br.com.dominando.frontend.ui.GameViewport;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.audio.Sound;

import br.com.dominando.frontend.settings.GameSettings;

import br.com.dominando.frontend.audio.GameAudio;


public class WaitingScreen extends ScreenAdapter {
    private static final float WIDTH = 1920f;
    private static final float HEIGHT = 1080f;

    private final Color cream = Color.valueOf("F0DCB2");
    private final Color brown = Color.valueOf("3F2412");
    private final Color green = Color.valueOf("234730");

    private final Stage stage;
    private final Array<Texture> textures = new Array<>();

    private final BitmapFont labelFont;
    private final BitmapFont codeFont;
    private final BitmapFont buttonFont;

    private final TextButton copyButton;
    private final Label copyFeedback;
    private final Image[] caps = new Image[3];
    private final BitmapFont inviteFont;

    private final String roomCode;

    private final GameSettings gameSettings = new GameSettings();
    private final Sound clickSound;

    private final GameViewport viewport = new GameViewport(WIDTH, HEIGHT);

    private final Image backgroundImage;
    private final Image rightBackground;
    private final Image rightTexture;
    private final Image divider;

    public WaitingScreen(String roomCode, Runnable onCancel){
        if (roomCode == null || roomCode.length() != 6) {
            throw new IllegalArgumentException("Espera-se um código de 6 caracteres");
        }

        this.roomCode = roomCode;
        clickSound = GameAudio.newSound(Gdx.files.internal("audio/clique-botao.wav"));
        stage = new Stage(viewport);

        labelFont = createFont("fonts/NunitoSans-Regular.ttf", 22);
        codeFont = createFont("fonts/NunitoSans-ExtraBold.ttf", 64);
        inviteFont = createFont("fonts/NunitoSans-SemiBold.ttf", 22);
        buttonFont = createFont("fonts/LilitaOne-Regular.ttf", 36);

        backgroundImage = addImage(loadTexture("ui/pairing/fundo-engradados.png"), 0f, 0f, WIDTH, HEIGHT);
        backgroundImage.setScaling(Scaling.fill);

        addImage(loadTexture("ui/pairing/logo-desce-uma.png"), 145f, 64f, 649.822f, 473f);

        rightBackground = addSolid(green, 960f, 0f, 960f, HEIGHT);

        rightTexture = addImage(loadTexture("ui/pairing/textura-verde.png"), 960f, 0f, 960f, HEIGHT);
        rightTexture.setScaling(Scaling.fill);

        divider = addSolid(brown, 960f, 0f, 6f, HEIGHT);

        addImage(loadTexture("ui/waiting/titulo-mesa-criada.png"), 1160f, 48f, 561f, 187f);
        addLabel("ENVIE ESSE CÓDIGO PARA QUEM VAI JOGAR COM VOCÊ", inviteFont, new Color(cream.r, cream.g, cream.b, 0.8f), 1130f, 280f, 620f, 30f);

        TextureRegionDrawable codeBackground = roundedBackground(104, 160, 18, 5, cream, 4);

        for (int i = 0; i < 6; i++) {
            float x = 1088f + i * 120f;

            Image box = new Image(codeBackground);
            box.setTouchable(Touchable.disabled);
            place(box, x, 346f, 104f, 164f);
            stage.addActor(box);

            addLabel(String.valueOf(roomCode.charAt(i)), codeFont, green, x, 346f, 104f, 160f);
        }

        TextButton.TextButtonStyle copyStyle = new TextButton.TextButtonStyle();
        copyStyle.font = buttonFont;
        copyStyle.fontColor = brown;
        copyStyle.up = roundedBackground(500, 112, 18, 5, Color.valueOf("f0b639"), 8);
        copyStyle.down = roundedBackground(500, 112, 18, 5, Color.valueOf("DDA52E"), 8);
        copyStyle.pressedOffsetY = -2f;

        copyButton = new TextButton("COPIAR CÓDIGO", copyStyle);
        copyButton.padBottom(8f);
        place(copyButton, 1190f, 550f, 500f, 120f);
        stage.addActor(copyButton);

        copyFeedback = addLabel("", labelFont, cream, 1130f, 674f, 620f, 30f);

        Texture capTexture = loadTexture("ui/waiting/tampinha.png");

        float capHeight = 60f * capTexture.getHeight() / capTexture.getWidth();

        for (int i = 0; i < caps.length; i++) {
            caps[i] = addImage(capTexture, 1332f + i * 78f, 716f, 60f, capHeight);
        }

        addLabel("Aguardando o outro jogador...", labelFont, Color.valueOf("f0dcd2"), 1130f, 810f, 620f, 30f);

        TextButton.TextButtonStyle cancelStyle = new TextButton.TextButtonStyle();

        cancelStyle.font = labelFont;
        cancelStyle.fontColor = new Color(cream.r, cream.g, cream.b, 0.85f);
        cancelStyle.downFontColor = Color.WHITE;

        TextButton cancelButton = new TextButton("VOLTAR", cancelStyle);

        place(cancelButton, 1290f, 878f, 300f, 72f);
        stage.addActor(cancelButton);

        copyButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                playClickSound();
                copyCode();
            }
        });

        cancelButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                playClickSound();
                onCancel.run();
            }
        });
    }

    private void playClickSound() {
        if (gameSettings.isSoundEffectsEnabled()) {
            GameAudio.play(clickSound, 1f);
        }
    }

    private void copyCode() {
        copyFeedback.clearActions();

        try {
            Gdx.app.getClipboard().setContents(roomCode);
            copyButton.setText("CÓDIGO COPIADO!");
            copyFeedback.setText("");

        } catch (RuntimeException exception) {
            copyButton.setText("COPIAR CÓDIGO");
            copyFeedback.setText("Não foi possível copiar. Tente Novamente");
            Gdx.app.error("WaitingScreen", "Falha ao copiar código", exception);
        }

        copyFeedback.addAction(Actions.sequence(Actions.delay(2f), Actions.run(() -> {
            copyButton.setText("COPIAR CÓDIGO");
            copyFeedback.setText("");
        })));
    }

    private void startLoadingAnimation() {
        for (int i = 0; i < caps.length; i++) {
            Image cap = caps[i];
            cap.clearActions();

            float baseY = HEIGHT - 716f - cap.getHeight();
            cap.setY(baseY);

            cap.addAction(Actions.sequence(
                Actions.delay(i * 0.16f),
                Actions.forever(Actions.sequence(
                    Actions.moveBy(0f, 14f, 0.22f),
                    Actions.moveBy(0f, -14f, 0.22f),
                    Actions.delay(0.48f)
                ))
            ));
        }
    }

    private Texture loadTexture(String path) {
        Texture texture = new Texture(Gdx.files.internal(path));
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return texture;
    }

    private Image addImage(Texture texture, float x, float top, float width, float height) {
        Image image = new Image(texture);
        image.setTouchable(Touchable.disabled);
        place(image, x, top, width, height);
        stage.addActor(image);

        return image;
    }

    private Label addLabel (String text, BitmapFont font, Color color, float x, float top, float width, float height) {
        Label label = new Label(text, new Label.LabelStyle(font, color));
        label.setAlignment(Align.center);
        label.setTouchable(Touchable.disabled);
        place(label, x, top, width, height);
        stage.addActor(label);

        return label;
    }

    private Image addSolid(Color color, float x, float top, float width, float height) {
        Image image = new Image(solid(color));
        image.setTouchable(Touchable.disabled);

        place(image, x, top, width, height);
        stage.addActor(image);

        return image;
    }

    private void place(Actor actor, float x, float top, float width, float height) {
        actor.setBounds(x, HEIGHT - top - height, width, height);
    }
    private BitmapFont createFont(String path, int size) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(path));
        try {
            FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.size = size;
            parameter.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "áàâãéêíóôõúüçÁÀÂÃÉÊÍÓÔÕÚÜÇ";
            parameter.minFilter = Texture.TextureFilter.Linear;
            parameter.magFilter = Texture.TextureFilter.Linear;

            return generator.generateFont(parameter);

        } finally {
            generator.dispose();
        }
    }
    private TextureRegionDrawable solid(Color color) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();

        return drawable(pixmap);
    }
    private TextureRegionDrawable roundedBackground(int width, int height, int radius, int border, Color fill, int shadow) {
        Pixmap pixmap = new Pixmap(width, height + shadow, Pixmap.Format.RGBA8888);
        pixmap.setColor(brown);
        fillRounded(pixmap, 0, shadow, width, height, radius);
        fillRounded(pixmap, 0, 0, width, height, radius);

        pixmap.setColor(fill);
        fillRounded(pixmap, border, border, width - border * 2, height - border * 2, radius - border);
        return drawable(pixmap);
    }
    private void fillRounded(Pixmap pixmap, int x, int y, int width, int height, int radius) {
        pixmap.fillRectangle(x + radius, y, width - radius * 2, height);
        pixmap.fillRectangle(x, y + radius, width, height - radius * 2);
        pixmap.fillCircle(x + radius, y + radius, radius);
        pixmap.fillCircle(x + width - radius - 1, y + radius, radius);
        pixmap.fillCircle(x + radius, y + height - radius - 1, radius);
        pixmap.fillCircle(x + width - radius - 1, y + height - radius - 1, radius);
    }
    private TextureRegionDrawable drawable(Pixmap pixmap) {
        Texture texture;

        try {
            texture = new Texture(pixmap);
        } finally {
            pixmap.dispose();
        }

        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        textures.add(texture);

        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    @Override
    public void show() {
        copyFeedback.clearActions();
        copyFeedback.setText("");
        copyButton.setText("COPIAR CÓDIGO");

        startLoadingAnimation();
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(cream);
        stage.getViewport().apply();
        stage.act(Math.min(delta, 1f/30f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }

        viewport.update(width, height, true);
        viewport.cover(backgroundImage);

        rightBackground.setBounds(WIDTH / 2f, viewport.getBottom(), viewport.getWorldWidth() / 2f, viewport.getWorldHeight());

        rightTexture.setBounds(rightBackground.getX(), rightBackground.getY(), rightBackground.getWidth(), rightBackground.getHeight());

        divider.setBounds(WIDTH / 2f, viewport.getBottom(), 6f, viewport.getWorldHeight());
    }

    @Override
    public void hide() {
        copyFeedback.clearActions();
        for (Image cap :  caps) {
            cap.clearActions();
        }

        if (Gdx.input.getInputProcessor() == stage) {
            Gdx.input.setInputProcessor(null);
        }
    }

    @Override
    public void dispose() {
        hide();
        GameAudio.dispose(clickSound);
        stage.dispose();
        labelFont.dispose();
        codeFont.dispose();
        inviteFont.dispose();
        buttonFont.dispose();
        for (Texture texture : textures) {
            texture.dispose();
        }
        textures.clear();
    }
}
