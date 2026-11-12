package br.com.dominando.frontend.ui;

import br.com.dominando.frontend.settings.GameSettings;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Align;

public class OpeningDialogs implements Disposable {
    private static final float WIDTH = 1920f;
    private static final float HEIGHT = 1080f;
    private final Color brown = Color.valueOf("3f2412");
    private final Color cream = Color.valueOf("f0dcb2");
    private final GameSettings settings;
    private final Group overlay = new Group();
    private final Array<Texture> textures = new Array<>();
    private final BitmapFont titleFont;
    private final BitmapFont labelFont;
    private final BitmapFont buttonFont;
    private final TextureRegionDrawable switchOn;
    private final TextureRegionDrawable switchOff;
    private final Button musicButton;
    private final Button effectsButton;
    private final Button vibrationButton;
    private final Group connectionOverlay = new Group();


    public OpeningDialogs(GameSettings settings, Runnable onRetry) {
        this.settings = settings;
        titleFont = createFont("fonts/LilitaOne-Regular.ttf", 40);
        labelFont = createFont("fonts/NunitoSans-Regular.ttf", 28);
        buttonFont = createFont("fonts/LilitaOne-Regular.ttf", 28);

        Texture switchTexture = new Texture(Gdx.files.internal("ui/icons/interruptor-ligado.png"));

        switchTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        textures.add(switchTexture);

        switchOn = new TextureRegionDrawable(new TextureRegion(switchTexture));

        Texture offTexture = new Texture(Gdx.files.internal("ui/icons/interruptor-desligado.png"));
        offTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        textures.add(offTexture);
        switchOff = new TextureRegionDrawable(new TextureRegion(offTexture));

        overlay.setSize(WIDTH, HEIGHT);
        overlay.setTouchable(Touchable.enabled);

        Image dimmer = new Image(createSolid(new Color (0f,0f,0f,0.5f)));
        dimmer.setBounds(0f, 0f, WIDTH, HEIGHT);
        dimmer.setTouchable(Touchable.enabled);
        overlay.addActor(dimmer);

        Image panelBackground = new Image(createRoundedBackground(640, 448, 24, 4, cream, 8));
        panelBackground.setTouchable(Touchable.disabled);
        panelBackground.setBounds(640f, 308f, 640f, 456f);
        overlay.addActor(panelBackground);

        Table content = new Table();
        content.setBounds(684f, 360f, 552f, 360f);
        overlay.addActor(content);

        Label title = new Label("CONFIGURAÇÕES", new Label.LabelStyle(titleFont, brown));
        content.add(title).colspan(2).height(48f).padBottom(24f);
        content.row();

        musicButton = createSwitch();
        effectsButton = createSwitch();
        vibrationButton = createSwitch();

        addOption(content, "Música", musicButton);
        addOption(content, "Efeitos Sonoros", effectsButton);
        addOption(content, "Vibração", vibrationButton);

        TextButton.TextButtonStyle backStyle = new TextButton.TextButtonStyle();

        backStyle.font = buttonFont;
        backStyle.fontColor = brown;
        backStyle.up = createRoundedBackground(552, 72, 18, 5, Color.valueOf("F0B639"), 8);
        backStyle.down = createRoundedBackground(552, 72, 18, 5, Color.valueOf("DDA52E"), 8);
        backStyle.pressedOffsetY = -2f;
        TextButton backButton = new TextButton("VOLTAR", backStyle);
        backButton.padBottom(8f);
        content.add(backButton).colspan(2).width(552f).height(80f);

        musicButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                settings.setMusicEnabled(musicButton.isChecked());
            }
        });

        effectsButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                settings.setSoundEffectsEnabled(effectsButton.isChecked());
            }
        });

        vibrationButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                settings.setVibrationEnabled(vibrationButton.isChecked());
            }
        });

        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                close();
            }
        });
        buildConnectionOverlay(onRetry);
    }

    private void buildConnectionOverlay(Runnable onRetry) {
        connectionOverlay.setSize(WIDTH, HEIGHT);
        connectionOverlay.setTouchable(Touchable.enabled);

        Image dimmer = new Image(createSolid(new Color(0f, 0f, 0f, 0.5f)));
        dimmer.setBounds(0f, 0f, WIDTH, HEIGHT);
        connectionOverlay.addActor(dimmer);

        Image panel = new Image(createRoundedBackground(640, 320, 24, 4, cream, 8));
        panel.setBounds(640f, 372f, 640f, 328f);
        connectionOverlay.addActor(panel);

        Label title = new Label("SEM CONEXÃO", new Label.LabelStyle(titleFont, brown));
        title.setAlignment(Align.center);
        title.setBounds(684f, 608f, 552f, 48f);
        connectionOverlay.addActor(title);

        Label message = new Label("Você está sem conexão. Verifique sua internet e\n" + "tente novamente.", new Label.LabelStyle(labelFont, brown));
        message.setFontScale(24f / 28f);
        message.setAlignment(Align.center);
        message.setBounds(684f, 520f, 552f, 64f);
        connectionOverlay.addActor(message);

        addConnectionButton("VOLTAR", 684f, cream, Color.valueOf("234730"), this::close);
        addConnectionButton("TENTAR DE NOVO", 972f, Color.valueOf("F0B639"), brown, () -> {
            close();
            onRetry.run();
        });
    }

    private void addConnectionButton(String text, float x, Color fill, Color textColor, Runnable onClick) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = buttonFont;
        style.fontColor = textColor;
        style.up = createRoundedBackground(272, 72, 18, 5, fill, 8);
        style.pressedOffsetY = -2f;

        TextButton button = new TextButton(text, style);
        button.padBottom(8f);
        button.setBounds(x, 416f, 272f, 80f);
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                onClick.run();
            }
        });
        connectionOverlay.addActor(button);
    }

    public void openConnectionError(Stage stage) {
        stage.addActor(connectionOverlay);
        stage.setKeyboardFocus(null);
        stage.setScrollFocus(null);
    }

    public void open(Stage stage) {
        if (overlay.getStage() != null) {
            return;
        }

        musicButton.setChecked(settings.isMusicEnabled());
        effectsButton.setChecked(settings.isSoundEffectsEnabled());
        vibrationButton.setChecked(settings.isVibrationEnabled());
        stage.addActor(overlay);
        overlay.toFront();

        stage.setKeyboardFocus(null);
        stage.setScrollFocus(null);
    }
    public void close() {
        overlay.remove();
        connectionOverlay.remove();
    }
    private Button createSwitch() {
        Button.ButtonStyle style = new Button.ButtonStyle();
        style.up = switchOff;
        style.down = switchOff;
        style.checked = switchOn;

        Button button = new Button(style);
        button.setProgrammaticChangeEvents(false);
        return button;
    }
    private void addOption(Table table, String text, Button button) {
        Label label = new Label(
            text,
            new Label.LabelStyle(labelFont, brown)
        );

        table.add(label).expandX().fillX().height(48f).padRight(16f).padBottom(24f);
        table.add(button).width(88f).height(48f).padBottom(24f);
        table.row();
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
    private TextureRegionDrawable createSolid(Color color) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        return createDrawable(pixmap);
    }

    private TextureRegionDrawable createRoundedBackground(int width, int height, int radius, int border, Color fill, int shadow) {
        Pixmap pixmap = new Pixmap(width, height + shadow, Pixmap.Format.RGBA8888);

        pixmap.setColor(brown);fillRoundedRectangle(pixmap, 0, shadow, width, height, radius);
        fillRoundedRectangle(pixmap, 0, 0, width, height, radius);
        pixmap.setColor(fill);

        fillRoundedRectangle(pixmap, border, border, width - border * 2, height - border * 2, radius - border);

        return createDrawable(pixmap);
    }

    private void fillRoundedRectangle(Pixmap pixmap, int x, int y, int width, int height, int radius) {
        pixmap.fillRectangle(x + radius, y, width - radius * 2, height);
        pixmap.fillRectangle(x, y + radius, width, height - radius * 2);
        pixmap.fillCircle(x + radius, y + radius, radius);
        pixmap.fillCircle(x + width - radius - 1, y + radius, radius);
        pixmap.fillCircle(x + radius, y + height - radius - 1, radius);
        pixmap.fillCircle(x + width - radius - 1, y + height - radius - 1, radius);
    }

    private TextureRegionDrawable createDrawable(Pixmap pixmap) {
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
    public void dispose() {
        close();

        titleFont.dispose();
        labelFont.dispose();
        buttonFont.dispose();

        for (Texture texture : textures) {
            texture.dispose();
        }

        textures.clear();
    }
}
