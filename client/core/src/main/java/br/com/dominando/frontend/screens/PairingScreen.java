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

    public PairingScreen(Runnable onBack, Runnable onCreate) {
        stage = new Stage(new FitViewport(WIDTH, HEIGHT));

        labelFont = createFont("fonts/NunitoSans-Regular.ttf", 22);
        inputFont = createFont("fonts/NunitoSans-Regular.ttf", 28);
        buttonFont = createFont("fonts/LilitaOne-Regular.ttf", 36);
        separatorFont = createFont("fonts/LilitaOne-Regular.ttf", 30);

        addAsset("fundo-engradados.png", 0f, 0f, 1920f, 1080f);
        addAsset("logo-desce-uma.png", 145f, 64f, 649.822f, 473f);

        addSolid(green, 960f, 0f, 960f, 1080f);
        addAsset("textura-verde.png", 960f, 0f, 960f, 1080f);
        addSolid(brown, 960f, 0f, 6f, 1080f);

        addAsset("titulo-escolha-mesa.png", 1160f, 48f, 561f, 187f);

        addLabel("SEU NOME", labelFont, cream, 1130f, 264f, 620f, 30f, Align.left);
        nameField = createInput("Digite seu nome", 1130f, 306f);

        createButton = createButton("CRIAR UMA MESA", 1190f, 442f, 500f, 112f);

        addAsset("separador.png", 1228f, 605f, 424f, 4f);

        addLabel("OU", separatorFont, ochre,1420f, 590f, 40f, 34f, Align.center);

        addLabel("CÓDIGO DA MESA", labelFont, cream,1130f, 656f, 620f, 30f, Align.left);
        codeField = createInput("Digite o código", 1130f, 698f);
        joinButton = createButton("ENTRAR NA MESA", 1190f, 834f, 500f, 112f);

        feedback = addLabel("", labelFont, cream,1070f, 968f, 740f, 64f, Align.center);
        feedback.setWrap(true);

        TextButton backButton = createButton("VOLTAR", 48f, 48f, 200f, 72f);

        ChangeListener inputListener = new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                feedback.setText("");
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

                finishTyping();
                onCreate.run();
            }
        });

        joinButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (joinButton.isDisabled()) {
                    return;
                }

                finishTyping();
                feedback.setText("Dados preenchidos. Entrada na mesa ainda não conectada.");
            }
        });

        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                onBack.run();
            }
        });

        refreshButtons();
    }

    private void refreshButtons() {
        boolean hasName = !nameField.getText().trim().isEmpty();
        boolean hasCode = !codeField.getText().trim().isEmpty();

        setEnabled(createButton, hasName);
        setEnabled(joinButton, hasName && hasCode);
    }

    private void setEnabled(TextButton button, boolean enabled) {
        button.setDisabled(!enabled);
        button.setColor(1f, 1f, 1f, enabled ? 1f : 0.5f);
    }

    private TextField createInput(String placeholder, float x, float top) {
        TextField.TextFieldStyle style = new TextField.TextFieldStyle();
        style.font = inputFont;
        style.fontColor = brown;
        style.messageFont = inputFont;
        style.messageFontColor = new Color(brown.r, brown.g, brown.b, 0.6f);

        TextureRegionDrawable background = roundedBackground(620, 104, 18, 5, cream, 4);

        background.setLeftWidth(32f);
        background.setRightWidth(32f);
        background.setTopHeight(5f);
        background.setBottomHeight(9f);

        style.background = background;

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

    private TextButton createButton(
        String text, float x, float top, float width, float height
    ) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();

        style.font = buttonFont;
        style.fontColor = green;
        style.disabledFontColor = green;

        style.up = roundedBackground((int) width, (int) height, 18, 5, cream, 8);
        style.down = roundedBackground((int) width, (int) height, 18, 5, Color.valueOf("DCC79C"), 8);
        style.disabled = style.up;
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

    private void addAsset(String filename, float x, float top, float width, float height) {
        Texture texture = new Texture(Gdx.files.internal("ui/pairing/" + filename));
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        textures.add(texture);

        Image image = new Image(texture);
        image.setTouchable(Touchable.disabled);

        place(image, x, top, width, height);
        stage.addActor(image);
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

    private void finishTyping() {
        stage.setKeyboardFocus(null);
        Gdx.input.setOnscreenKeyboardVisible(false);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
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
        if (Gdx.input.getInputProcessor() == stage) {
            finishTyping();
            Gdx.input.setInputProcessor(null);
        }
    }

    @Override
    public void dispose() {
        hide();
        stage.dispose();

        labelFont.dispose();
        inputFont.dispose();
        buttonFont.dispose();
        separatorFont.dispose();

        for (Texture texture : textures) {
            texture.dispose();
        }
        textures.clear();
    }
}
