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
import com.badlogic.gdx.scenes.scene2d.ui.Slider;

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
    private final Group exitOverlay = new Group();
    private final Group reconnectOverlay = new Group();
    private final Group opponentReconnectOverlay = new Group();
    private final Slider musicVolumeSlider;
    private final Label musicVolumePercent;


    public OpeningDialogs(GameSettings settings, Runnable onRetry) {
        this(settings, onRetry, null);
    }

    public OpeningDialogs(GameSettings settings, Runnable onRetry, Runnable onLeaveMatch) {
        this.settings = settings;
        float extraHeight = 60f + (onLeaveMatch == null ? 0f : 96f);
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

        Image panelBackground = new Image(createRoundedBackground(640, 448 + (int) extraHeight, 24, 4, cream, 8));
        panelBackground.setTouchable(Touchable.disabled);
        panelBackground.setBounds(640f, 308f - extraHeight / 2f, 640f, 456f + extraHeight);
        overlay.addActor(panelBackground);

        Table content = new Table();
        content.setBounds(684f, 360f - extraHeight / 2f, 552f, 360f + extraHeight);
        overlay.addActor(content);

        Label title = new Label("CONFIGURAÇÕES", new Label.LabelStyle(titleFont, brown));
        content.add(title).colspan(2).height(48f).padBottom(24f);
        content.row();

        musicButton = createSwitch();
        effectsButton = createSwitch();
        vibrationButton = createSwitch();

        addOption(content, "Música", musicButton);

        Slider.SliderStyle volumeStyle = new Slider.SliderStyle();
        volumeStyle.background = createRoundedBackground(
            210, 12, 6, 2, Color.valueOf("D6BC90"), 0
        );
        volumeStyle.knob = createRoundedBackground(
            26, 26, 13, 3, Color.valueOf("F0B639"), 0
        );

        musicVolumeSlider = new Slider(0f, 100f, 5f, false, volumeStyle);
        musicVolumePercent = new Label("", new Label.LabelStyle(labelFont, brown));
        musicVolumePercent.setAlignment(Align.right);

        Table volumeRow = new Table();
        volumeRow.add(new Label("Volume da música", new Label.LabelStyle(labelFont, brown)))
            .expandX().left();
        volumeRow.add(musicVolumeSlider).width(210f).height(48f);
        volumeRow.add(musicVolumePercent).width(72f).padLeft(16f);
        content.add(volumeRow).colspan(2).width(552f).height(48f).padBottom(12f);
        content.row();

        musicVolumeSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                int volume = Math.round(musicVolumeSlider.getValue());
                settings.setMusicVolume(volume);
                musicVolumePercent.setText(volume + "%");
            }
        });

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
        if (onLeaveMatch != null) {
            buildExitOverlay(onLeaveMatch);
            buildReconnectionOverlays();

            TextButton.TextButtonStyle exitStyle = new TextButton.TextButtonStyle(backStyle);
            exitStyle.fontColor = Color.valueOf("9B332B");
            exitStyle.up = createRoundedBackground(552, 72, 18, 5, cream, 8);

            TextButton exitButton = new TextButton("SAIR DA MESA", exitStyle);
            exitButton.padBottom(8f);
            content.row();
            content.add(exitButton).colspan(2).width(552f).height(80f).padTop(16f);

            exitButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    Stage stage = overlay.getStage();
                    close();
                    stage.addActor(exitOverlay);
                }
            });
        }

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
        buildMessageOverlay(connectionOverlay, "SEM CONEXÃO",
            "Você está sem conexão. Verifique sua internet e\ntente novamente.");

        addDialogButton(connectionOverlay, "VOLTAR", 684f, cream,
            Color.valueOf("234730"), this::close);
        addDialogButton(connectionOverlay, "TENTAR DE NOVO", 972f,
            Color.valueOf("F0B639"), brown, () -> {
                close();
                onRetry.run();
            });
    }

    private void buildExitOverlay(Runnable onLeaveMatch) {
        buildMessageOverlay(exitOverlay, "SAIR DA MESA?",
            "Se sair, a partida atual será encerrada.");

        addDialogButton(exitOverlay, "CONTINUAR", 684f, cream,
            Color.valueOf("234730"), this::close);
        addDialogButton(exitOverlay, "SAIR DA MESA", 972f,
            Color.valueOf("F0B639"), brown, onLeaveMatch);
    }

    private void buildReconnectionOverlays() {
        buildMessageOverlay(reconnectOverlay, "RECONECTANDO...",
            "Estamos tentando recuperar sua conexão com a mesa.", 224);

        buildMessageOverlay(opponentReconnectOverlay, "AGUARDANDO ADVERSÁRIO",
            "O adversário perdeu a conexão.\nA partida continua quando ele voltar.", 224);
    }

    private void buildMessageOverlay(Group target, String heading, String description) {
        buildMessageOverlay(target, heading, description, 320);
    }

    private void buildMessageOverlay(Group target, String heading, String description, int panelHeight) {
        float panelTop = (HEIGHT + panelHeight) / 2f;
        target.setSize(WIDTH, HEIGHT);
        target.setTouchable(Touchable.enabled);

        Image dimmer = new Image(createSolid(new Color(0f, 0f, 0f, 0.5f)));
        dimmer.setBounds(0f, 0f, WIDTH, HEIGHT);
        target.addActor(dimmer);

        Image panel = new Image(createRoundedBackground(640, panelHeight, 24, 4, cream, 8));
        panel.setBounds(640f, panelTop - panelHeight - 8f, 640f, panelHeight + 8f);
        target.addActor(panel);

        Label title = new Label(heading, new Label.LabelStyle(titleFont, brown));
        title.setAlignment(Align.center);
        title.setBounds(684f, panelTop - 92f, 552f, 48f);
        target.addActor(title);

        Label message = new Label(description, new Label.LabelStyle(labelFont, brown));
        message.setFontScale(24f / 28f);
        message.setAlignment(Align.center);
        message.setWrap(true);
        message.setBounds(684f, panelTop - 180f, 552f, 64f);
        target.addActor(message);
    }

    private void addDialogButton(
        Group target, String text, float x, Color fill, Color textColor, Runnable onClick
    ) {
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
        target.addActor(button);
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
        int volume = settings.getMusicVolume();
        musicVolumeSlider.setValue(volume);
        musicVolumePercent.setText(volume + "%");
        effectsButton.setChecked(settings.isSoundEffectsEnabled());
        vibrationButton.setChecked(settings.isVibrationEnabled());
        stage.addActor(overlay);
        overlay.toFront();

        stage.setKeyboardFocus(null);
        stage.setScrollFocus(null);
    }
    public boolean isExitConfirmationOpen() {
        return exitOverlay.getStage() != null;
    }

    public void openReconnection(Stage stage, boolean waitingForOpponent) {
        close();
        stage.cancelTouchFocus();
        stage.setKeyboardFocus(null);
        stage.setScrollFocus(null);
        stage.addActor(waitingForOpponent ? opponentReconnectOverlay : reconnectOverlay);
    }

    public boolean isReconnectionOpen() {
        return reconnectOverlay.getStage() != null
            || opponentReconnectOverlay.getStage() != null;
    }

    public void closeReconnection() {
        reconnectOverlay.remove();
        opponentReconnectOverlay.remove();
    }

    public void close() {
        overlay.remove();
        connectionOverlay.remove();
        exitOverlay.remove();
        closeReconnection();
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
