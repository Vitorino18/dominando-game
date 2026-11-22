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
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.audio.Sound;

import br.com.dominando.frontend.settings.GameSettings;

import br.com.dominando.frontend.audio.GameAudio;

public class PairingScreen extends ScreenAdapter {

    private static final float WIDTH = 1920f;
    private static final float HEIGHT = 1080f;

    private final Color cream = Color.valueOf("F0DCB2");
    private final Color brown = Color.valueOf("3F2412");
    private final Color green = Color.valueOf("234730");
    private final Color ochre = Color.valueOf("C88E30");

    private final Stage stage;
    private final Array<Texture> textures = new Array<>();

    private final BitmapFont labelFont;
    private final BitmapFont inputFont;
    private final BitmapFont buttonFont;
    private final BitmapFont separatorFont;

    private final TextField nameField;
    private final TextField codeField;
    private final TextButton createButton;
    private final TextButton joinButton;
    private final Label feedback;
    private final BitmapFont codeErrorFont;
    private final Label codeError;
    private final TextField.TextFieldStyle normalCodeStyle;
    private final TextField.TextFieldStyle errorCodeStyle;
    private final Group createLoading = new Group();
    private final Image[] createLoadingCaps = new Image[3];
    private final BitmapFont loadingTitleFont;
    private final BitmapFont loadingMessageFont;
    private Label loadingTitle;
    private Label loadingMessage;
    private final Group tableErrorDialog = new Group();
    private final BitmapFont dialogButtonFont;
    private boolean simulateCreateFailure = false;
    private Label tableErrorTitle;
    private Label tableErrorMessage;
    private Runnable retryTableAction;
    private boolean simulateJoinFailure = false;
    private final BitmapFont nameErrorFont;
    private final Label nameError;
    private final Label separatorLabel;
    private final Label codeLabel;
    private final Image separatorImage;
    private final TextField.TextFieldStyle normalNameStyle;
    private final TextField.TextFieldStyle errorNameStyle;
    private boolean simulateNameRejection = false;
    private final GameSettings gameSettings = new GameSettings();
    private final Sound clickSound;

    public PairingScreen(Runnable onBack, Runnable onCreate) {
        clickSound = GameAudio.newSound(Gdx.files.internal("audio/clique-botao.wav"));

        stage = new Stage(new FitViewport(WIDTH, HEIGHT));

        labelFont = createFont("fonts/NunitoSans-SemiBold.ttf", 22);
        inputFont = createFont("fonts/NunitoSans-SemiBold.ttf", 28);
        buttonFont = createFont("fonts/LilitaOne-Regular.ttf", 36);
        separatorFont = createFont("fonts/LilitaOne-Regular.ttf", 30);
        loadingTitleFont = createFont("fonts/LilitaOne-Regular.ttf", 40);
        loadingMessageFont = createFont("fonts/NunitoSans-Regular.ttf", 24);
        dialogButtonFont = createFont("fonts/LilitaOne-Regular.ttf", 28);

        addAsset("fundo-engradados.png", 0f, 0f, 1920f, 1080f);
        addAsset("logo-desce-uma.png", 145f, 64f, 649.822f, 473f);

        addSolid(green, 960f, 0f, 960f, 1080f);
        addAsset("textura-verde.png", 960f, 0f, 960f, 1080f);
        addSolid(brown, 960f, 0f, 6f, 1080f);

        addAsset("titulo-escolha-mesa.png", 1160f, 48f, 561f, 187f);

        addLabel("SEU NOME", labelFont, cream, 1130f, 264f, 620f, 30f, Align.left);
        nameField = createInput("Digite seu nome", 1130f, 306f);
        nameErrorFont = createFont("fonts/NunitoSans-ExtraBold.ttf", 22);
        nameError = addLabel("Esse nome não foi aceito. Tente outro", nameErrorFont, cream, 1130f, 422f, 620f, 30f, Align.left);
        nameError.setVisible(false);

        normalNameStyle = nameField.getStyle();
        errorNameStyle = new TextField.TextFieldStyle(normalNameStyle);

        createButton = createButton("CRIAR UMA MESA", 1190f, 442f, 500f, 112f);

        separatorImage = addAsset("separador.png", 1228f, 605f, 424f, 4f);

        separatorLabel = addLabel("OU", separatorFont, ochre, 1420f, 590f, 40f, 34f, Align.center);

        codeLabel = addLabel("CÓDIGO DA MESA", labelFont, cream, 1130f, 656f, 620f, 30f, Align.left);

        codeField = createInput("Digite o código", 1130f, 698f);
        joinButton = createButton("ENTRAR NA MESA", 1190f, 834f, 500f, 112f);

        codeErrorFont = createFont("fonts/NunitoSans-Black.ttf", 22);
        codeError = addLabel("Confira o código da mesa", codeErrorFont, cream, 1130f, 814f, 620f, 30f, Align.left);
        codeError.setVisible(false);

        normalCodeStyle = codeField.getStyle();
        errorCodeStyle = new TextField.TextFieldStyle(normalCodeStyle);
        errorCodeStyle.background = createInputBackground(Color.valueOf("9c2f23"));
        errorNameStyle.background = errorCodeStyle.background;

        feedback = addLabel("", labelFont, cream,1070f, 968f, 740f, 64f, Align.center);
        feedback.setWrap(true);

        ChangeListener inputListener = new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                feedback.setText("");

                if (actor == codeField && codeError.isVisible()) {
                    setCodeErrorVisible(false);
                }

                if (actor == nameField && nameError.isVisible()) {
                    setNameErrorVisible(false);
                }

                refreshButtons();
            }
        };

        nameField.addListener(inputListener);
        codeField.addListener(inputListener);

        createButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (createButton.isDisabled()) {
                    return;
                }

                playClickSound();
                finishTyping();
                startCreatingTable(onCreate);
            }
        });

        joinButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (joinButton.isDisabled()) {
                    return;
                }

                playClickSound();
                finishTyping();

                if (codeField.getText().trim().length() != 4) {
                    setCodeErrorVisible(true);
                    return;
                }

                setCodeErrorVisible(false);
                startJoiningTable();
            }
        });

        stage.addCaptureListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode != Input.Keys.ESCAPE && keycode != Input.Keys.BACK) {
                    return false;
                }

                if (tableErrorDialog.isVisible()) {
                    tableErrorDialog.setVisible(false);
                    return true;
                }

                if (createLoading.isVisible()) {
                    return true;
                }

                finishTyping();
                onBack.run();
                return true;
            }
        });


        buildCreateLoading();
        buildTableErrorDialog();
        refreshButtons();
    }

    private void playClickSound() {
        if (gameSettings.isSoundEffectsEnabled()) {
            GameAudio.play(clickSound, 1f);
        }
    }

    private void startCreatingTable(Runnable onCreate) {
        startTableLoading("CRIANDO MESA...", "Aguarde enquanto preparamos sua mesa.", () -> {
            if (simulateNameRejection) {
                simulateNameRejection = false;
                createLoading.setVisible(false);
                setNameErrorVisible(true);
                return;
            }
            if (simulateCreateFailure) {
                simulateCreateFailure = false;
                showTableError("NÃO FOI POSSÍVEL CRIAR", "Não conseguimos criar sua mesa. Tente\nnovamente.", () -> startCreatingTable(onCreate));
                return;
            }

            onCreate.run();
        });
    }

    private void startJoiningTable() {
        startTableLoading("ENTRANDO NA MESA...", "Aguarde enquanto conectamos você à mesa.",
            () -> {
                if (simulateJoinFailure) {
                    simulateJoinFailure = false;
                    showTableError("NÃO FOI POSSÍVEL ENTRAR", "Não conseguimos entrar nesta mesa. Confira o\ncódigo e tente novamente.", this::startJoiningTable);
                    return;
                }

                createLoading.setVisible(false);
                feedback.setText("Dados preenchidos. Entrada na mesa ainda não conectada.");
            }
        );
    }

    private void showTableError(String title, String message, Runnable onRetry) {
        createLoading.setVisible(false);
        tableErrorTitle.setText(title);
        tableErrorMessage.setText(message);
        retryTableAction = onRetry;
        tableErrorDialog.setVisible(true);
    }

    private void buildTableErrorDialog() {
        tableErrorDialog.setSize(WIDTH, HEIGHT);
        tableErrorDialog.setTouchable(Touchable.enabled);

        Image dimmer = new Image(solid(new Color(0f, 0f, 0f, 0.5f)));
        place(dimmer, 0f, 0f, WIDTH, HEIGHT);
        tableErrorDialog.addActor(dimmer);

        Image panel = new Image(roundedBackground(640, 320, 24, 4, cream, 8));
        place(panel, 640f, 380.5f, 640f, 328f);
        tableErrorDialog.addActor(panel);

        tableErrorTitle = new Label("", new Label.LabelStyle(loadingTitleFont, brown));
        tableErrorTitle.setAlignment(Align.center);
        place(tableErrorTitle, 684f, 424.5f, 552f, 48f);
        tableErrorDialog.addActor(tableErrorTitle);

        tableErrorMessage = new Label("", new Label.LabelStyle(loadingMessageFont, brown));
        tableErrorMessage.setAlignment(Align.center);
        place(tableErrorMessage, 684f, 496.5f, 552f, 64f);
        tableErrorDialog.addActor(tableErrorMessage);

        addCreateErrorButton("VOLTAR", 684f, cream, green, () -> tableErrorDialog.setVisible(false));

        addCreateErrorButton("TENTAR DE NOVO", 972f, Color.valueOf("F0B639"), brown, () -> {
            tableErrorDialog.setVisible(false);
            retryTableAction.run();
        });

        tableErrorDialog.setVisible(false);
        stage.addActor(tableErrorDialog);
    }

    private void addCreateErrorButton(String text, float x, Color fill,
                                      Color textColor, Runnable onClick) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = dialogButtonFont;
        style.fontColor = textColor;
        style.up = roundedBackground(272, 72, 18, 5, fill, 8);
        style.pressedOffsetY = -2f;

        TextButton button = new TextButton(text, style);
        button.padBottom(8f);
        place(button, x, 584.5f, 272f, 80f);
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                playClickSound();
                onClick.run();
            }
        });
        tableErrorDialog.addActor(button);
    }

    private void startTableLoading(String title, String message, Runnable onComplete) {
        loadingTitle.setText(title);
        loadingMessage.setText(message);
        feedback.setText("");

        if (codeError.isVisible()) {
            setCodeErrorVisible(false);
        }

        createLoading.setVisible(true);

        for (int i = 0; i < createLoadingCaps.length; i++) {
            Image cap = createLoadingCaps[i];
            cap.clearActions();
            cap.setY(HEIGHT - 592f - cap.getHeight());
            cap.addAction(Actions.sequence(
                Actions.delay(i * 0.30f),
                Actions.moveBy(0f, 12f, 0.15f),
                Actions.moveBy(0f, -12f, 0.15f)
            ));
        }

        createLoading.addAction(Actions.sequence(
            Actions.delay(1.2f),
            Actions.run(onComplete)
        ));
    }

    private void buildCreateLoading() {
        createLoading.setSize(WIDTH, HEIGHT);
        createLoading.setTouchable(Touchable.enabled);

        Image dimmer = new Image(solid(new Color(0f, 0f, 0f, 0.5f)));
        place(dimmer, 0f, 0f, WIDTH, HEIGHT);
        createLoading.addActor(dimmer);

        Image panel = new Image(roundedBackground(640, 295, 24, 4, cream, 8));
        place(panel, 640f, 393f, 640f, 303f);
        createLoading.addActor(panel);

        loadingTitle = addLoadingLabel("", loadingTitleFont, 437f, 48f);
        loadingMessage = addLoadingLabel("", loadingMessageFont, 509f, 31f);

        Texture capTexture = new Texture(Gdx.files.internal("ui/waiting/tampinha.png"));
        capTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        textures.add(capTexture);

        float capHeight = 40f * capTexture.getHeight() / capTexture.getWidth();
        for (int i = 0; i < createLoadingCaps.length; i++) {
            Image cap = new Image(capTexture);
            cap.setScaling(Scaling.stretch);
            place(cap, 888f + i * 52f, 592f, 40f, capHeight);
            createLoadingCaps[i] = cap;
            createLoading.addActor(cap);
        }

        createLoading.setVisible(false);
        stage.addActor(createLoading);
    }

    private Label addLoadingLabel(String text, BitmapFont font, float top, float height) {
        Label label = new Label(text, new Label.LabelStyle(font, brown));
        label.setAlignment(Align.center);
        label.setTouchable(Touchable.disabled);
        place(label, 684f, top, 552f, height);
        createLoading.addActor(label);

        return label;
    }

    private void refreshButtons() {
        boolean hasName = !nameField.getText().trim().isEmpty() && !nameError.isVisible();
        boolean hasCode = !codeField.getText().trim().isEmpty();

        setEnabled(createButton, hasName);
        setEnabled(joinButton, hasName && hasCode);
    }

    private void setEnabled(TextButton button, boolean enabled) {
        button.setDisabled(!enabled);
    }

    private TextField createInput(String placeholder, float x, float top) {
        TextField.TextFieldStyle style = new TextField.TextFieldStyle();
        style.font = inputFont;
        style.fontColor = brown;
        style.messageFont = inputFont;
        style.messageFontColor = new Color(brown.r, brown.g, brown.b, 0.6f);

        style.background = createInputBackground(brown);

        TextureRegionDrawable cursor = solid(brown);
        cursor.setMinWidth(2f);
        style.cursor = cursor;

        style.selection = solid(Color.valueOf("C8B786"));

        TextField field = new TextField("", style);
        field.setMessageText(placeholder);

        place(field, x, top, 620f, 108f);
        stage.addActor(field);

        return field;
    }

    private TextureRegionDrawable createInputBackground(Color borderColor) {
        TextureRegionDrawable background = roundedBackground(620, 104, 18, 5, cream, 4, borderColor);
        background.setLeftWidth(32f);
        background.setRightWidth(32f);
        background.setTopHeight(5f);
        background.setBottomHeight(9f);

        return background;
    }

    private TextButton createButton(String text, float x, float top, float width, float height) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();

        style.font = buttonFont;
        style.fontColor = green;
        style.disabledFontColor = green;

        TextureRegionDrawable normalBackground = roundedBackground((int) width, (int) height, 18, 5, cream, 8);

        style.up = normalBackground;
        style.disabled = normalBackground.tint(new Color(1f, 1f, 1f, 0.5f));

        style.down = roundedBackground((int) width, (int) height, 18, 5, Color.valueOf("DCC79C"), 8);
        style.pressedOffsetY = -2f;

        TextButton button = new TextButton(text, style);
        button.padBottom(8f);

        place(button, x, top, width, height + 8f);
        stage.addActor(button);

        return button;
    }

    private Label addLabel(String text, BitmapFont font, Color color, float x, float top, float width, float height, int alignment) {
        Label label = new Label(text, new Label.LabelStyle(font, color));
        label.setAlignment(alignment);
        label.setTouchable(Touchable.disabled);

        place(label, x, top, width, height);
        stage.addActor(label);

        return label;
    }

    private Image addAsset(String filename, float x, float top, float width, float height) {
        Texture texture = new Texture(Gdx.files.internal("ui/pairing/" + filename));
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        textures.add(texture);

        Image image = new Image(texture);
        image.setTouchable(Touchable.disabled);

        place(image, x, top, width, height);
        stage.addActor(image);

        return image;
    }

    private void addSolid(
        Color color, float x, float top, float width, float height) {
        Image image = new Image(solid(color));
        image.setTouchable(Touchable.disabled);

        place(image, x, top, width, height);
        stage.addActor(image);
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
        return roundedBackground(width, height, radius, border, fill, shadow, brown);
    }

    private TextureRegionDrawable roundedBackground(int width, int height, int radius, int border, Color fill, int shadow, Color borderColor) {
        Pixmap pixmap = new Pixmap(width, height + shadow, Pixmap.Format.RGBA8888);
        pixmap.setColor(brown);
        fillRounded(pixmap, 0, shadow, width, height, radius);

        pixmap.setColor(borderColor);
        fillRounded(pixmap, 0, 0, width, height, radius);

        pixmap.setColor(fill);
        fillRounded(pixmap, border, border, width - border * 2, height - border * 2, radius - border);
        return drawable(pixmap);
    }

    private void setCodeErrorVisible(boolean visible) {
        codeField.setStyle(visible ? errorCodeStyle : normalCodeStyle);
        codeError.setVisible(visible);
        layoutFormErrors();
    }

    private void setNameErrorVisible(boolean visible) {
        nameField.setStyle(visible ? errorNameStyle : normalNameStyle);
        nameError.setVisible(visible);
        layoutFormErrors();
        refreshButtons();
    }

    private void layoutFormErrors() {
        float nameOffset = nameError.isVisible() ? 42f : 0f;
        float codeOffset = codeError.isVisible() ? 42f : 0f;

        place(createButton, 1190f, 442f + nameOffset, 500f, 120f);
        place(separatorImage, 1228f, 605f + nameOffset, 424f, 4f);
        place(separatorLabel, 1420f, 590f + nameOffset, 40f, 34f);
        place(codeLabel, 1130f, 656f + nameOffset, 620f, 30f);
        place(codeField, 1130f, 698f + nameOffset, 620f, 108f);
        place(codeError, 1130f, 814f + nameOffset, 620f, 30f);
        place(joinButton, 1190f, 834f + nameOffset + codeOffset, 500f, 120f);
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

    private void finishTyping() {
        stage.setKeyboardFocus(null);
        Gdx.input.setOnscreenKeyboardVisible(false);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        Gdx.input.setCatchKey(Input.Keys.BACK, true);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(cream);
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
        createLoading.clearActions();
        createLoading.setVisible(false);

        if (Gdx.input.getInputProcessor() == stage) {
            finishTyping();
            Gdx.input.setInputProcessor(null);
            Gdx.input.setCatchKey(Input.Keys.BACK, false);
        }

        tableErrorDialog.setVisible(false);
    }

    @Override
    public void dispose() {
        hide();
        GameAudio.dispose(clickSound);
        stage.dispose();

        labelFont.dispose();
        inputFont.dispose();
        buttonFont.dispose();
        separatorFont.dispose();
        codeErrorFont.dispose();
        loadingTitleFont.dispose();
        loadingMessageFont.dispose();
        dialogButtonFont.dispose();
        nameErrorFont.dispose();

        for (Texture texture : textures) {
            texture.dispose();
        }

        textures.clear();
    }
}
