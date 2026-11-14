package br.com.dominando.frontend.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Scaling;

import java.util.ArrayList;
import java.util.List;

public final class RoundResultView extends Group implements Disposable {
    private static final Color CREAM = Color.valueOf("F0DCB2");
    private static final Color GREEN = Color.valueOf("234730");
    private static final Color BROWN = Color.valueOf("3F2412");
    private static final Color GOLD = Color.valueOf("F2B632");

    private final List<Texture> textures = new ArrayList<>();
    private final BitmapFont displayFont = createFont("LilitaOne-Regular.ttf", 80, false);
    private final BitmapFont titleFont = createFont("LilitaOne-Regular.ttf", 88, true);
    private final BitmapFont textFont = createFont("NunitoSans-Regular.ttf", 28, false);
    private final BitmapFont captionFont = createFont("NunitoSans-Regular.ttf", 21, false);
    private final Texture card = cardTexture(BROWN);
    private final Texture highlightedCard = cardTexture(GOLD);

    private final Image closedTitle = optionalTitle("mesa-fechada.png", 600f, 730f, 720f, 240f);
    private final Image tieTitle = optionalTitle("empate.png", 660f, 750f, 600f, 200f);
    private final Image humanTitle = optionalTitle("rodada-sua.png", 600f, 730f, 720f, 240f);
    private final Image opponentTitle = optionalTitle("rodada-adversario.png", 600f, 730f, 720f, 240f);
    private final Label fallbackTitle;
    private final Label description;
    private final CountCard humanCard;
    private final CountCard opponentCard;
    private final Image humanCap;
    private final Image opponentCap;

    public RoundResultView() {
        setSize(1920f, 1080f);
        setTouchable(Touchable.disabled);
        setVisible(false);

        fallbackTitle = label("", titleFont, CREAM, 480f, 730f, 960f, 240f);
        addActor(fallbackTitle);
        humanCard = new CountCard("VOCÊ", 600f);
        opponentCard = new CountCard("ADVERSÁRIO", 1000f);
        addActor(humanCard);
        addActor(opponentCard);

        humanCap = addImage(load(Gdx.files.internal("ui/score/tampinha-amarela.png")), 880f, 220f, 160f, 160f);
        opponentCap = addImage(load(Gdx.files.internal("ui/score/tampinha-verde.png")), 880f, 220f, 160f, 160f);
        humanCap.setScaling(Scaling.fit);
        opponentCap.setScaling(Scaling.fit);

        description = label("", textFont, CREAM, 510f, 330f, 900f, 100f);
        description.setWrap(true);
        addActor(description);
    }

    public void showCounts(boolean tie, boolean humanWon, int human, int opponent) {
        resetContent();
        showTitle(tie ? tieTitle : closedTitle, tie ? "EMPATE!" : "MESA FECHADA");
        humanCard.show(human, "PONTOS", !tie && humanWon);
        opponentCard.show(opponent, "PONTOS", !tie && !humanWon);
        description.setPosition(510f, 327f);
        description.setText(tie
            ? "Ninguém recebe tampinha. Vamos embaralhar e distribuir novamente."
            : humanWon ? "Você venceu com a menor soma de pontos."
              : "O adversário venceu com a menor soma de pontos.");
        reveal();
    }

    public void showRoundWinner(boolean humanWon) {
        resetContent();
        showTitle(humanWon ? humanTitle : opponentTitle,
            humanWon ? "RODADA SUA!" : "RODADA DO ADVERSÁRIO");
        (humanWon ? humanCap : opponentCap).setVisible(true);
        description.setPosition(510f, 139f);
        description.setText(humanWon ? "+1 TAMPINHA PARA VOCÊ" : "+1 TAMPINHA PARA O ADVERSÁRIO");
        reveal();
    }

    public void showMatchWinner(boolean humanWon, int human, int opponent) {
        resetContent();
        showTitle(null, humanWon ? "A PARTIDA É SUA!" : "O ADVERSÁRIO VENCEU!");
        humanCard.show(human, "TAMPINHAS", humanWon);
        opponentCard.show(opponent, "TAMPINHAS", !humanWon);
        description.setPosition(510f, 327f);
        description.setText(humanWon ? "Você conquistou 5 tampinhas." : "O adversário conquistou 5 tampinhas.");
        reveal();
    }

    private void resetContent() {
        if (closedTitle != null) closedTitle.setVisible(false);
        if (tieTitle != null) tieTitle.setVisible(false);
        if (humanTitle != null) humanTitle.setVisible(false);
        if (opponentTitle != null) opponentTitle.setVisible(false);
        fallbackTitle.setVisible(false);
        humanCard.setVisible(false);
        opponentCard.setVisible(false);
        humanCap.setVisible(false);
        opponentCap.setVisible(false);
    }

    private void showTitle(Image artwork, String fallback) {
        if (artwork != null) {
            artwork.setVisible(true);
        } else {
            fallbackTitle.setText(fallback);
            fallbackTitle.setVisible(true);
        }
    }

    private void reveal() {
        if (!isVisible()) {
            clearActions();
            getColor().a = 0f;
            setVisible(true);
            addAction(Actions.fadeIn(0.28f, Interpolation.smooth));
        }
    }

    private final class CountCard extends Group {
        private final Image frame;
        private final Label value;
        private final Label unit;

        CountCard(String title, float x) {
            setBounds(x, 480f, 320f, 220f);
            frame = new Image(card);
            frame.setBounds(0f, -6f, 320f, 226f);
            addActor(frame);
            addActor(label(title, captionFont, GREEN, 0f, 166f, 320f, 40f));
            value = label("", displayFont, GREEN, 0f, 62f, 320f, 96f);
            unit = label("", captionFont, GREEN, 0f, 23f, 320f, 34f);
            addActor(value);
            addActor(unit);
        }

        void show(int points, String units, boolean winner) {
            frame.setDrawable(new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                new com.badlogic.gdx.graphics.g2d.TextureRegion(winner ? highlightedCard : card)
            ));
            value.setText(Integer.toString(points));
            unit.setText(units);
            setVisible(true);
        }
    }

    private Label label(
        String text, BitmapFont font, Color color, float x, float y, float w, float h
    ) {
        Label label = new Label(text, new Label.LabelStyle(font, color));
        label.setAlignment(Align.center);
        label.setBounds(x, y, w, h);
        return label;
    }

    private Image optionalTitle(String name, float x, float y, float w, float h) {
        FileHandle file = Gdx.files.internal("ui/result/" + name);
        return file.exists() ? addImage(load(file), x, y, w, h) : null;
    }

    private Image addImage(Texture texture, float x, float y, float w, float h) {
        Image image = new Image(texture);
        image.setBounds(x, y, w, h);
        addActor(image);
        return image;
    }

    private Texture load(FileHandle file) {
        return own(new Texture(file));
    }

    private Texture own(Texture texture) {
        textures.add(texture);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return texture;
    }

    private Texture cardTexture(Color border) {
        Pixmap pixmap = new Pixmap(320, 226, Pixmap.Format.RGBA8888);
        try {
            rounded(pixmap, BROWN, 0, 6, 320, 220, 24);
            rounded(pixmap, border, 0, 0, 320, 220, 24);
            rounded(pixmap, CREAM, 4, 4, 312, 212, 20);
            return own(new Texture(pixmap));
        } finally {
            pixmap.dispose();
        }
    }

    private void rounded(
        Pixmap pixmap, Color color, int x, int y, int w, int h, int radius
    ) {
        pixmap.setColor(color);
        pixmap.fillRectangle(x + radius, y, w - 2 * radius, h);
        pixmap.fillRectangle(x, y + radius, w, h - 2 * radius);
        pixmap.fillCircle(x + radius, y + radius, radius);
        pixmap.fillCircle(x + w - radius - 1, y + radius, radius);
        pixmap.fillCircle(x + radius, y + h - radius - 1, radius);
        pixmap.fillCircle(x + w - radius - 1, y + h - radius - 1, radius);
    }

    private BitmapFont createFont(String filename, int size, boolean outline) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/" + filename));
        try {
            FreeTypeFontGenerator.FreeTypeFontParameter config =
                new FreeTypeFontGenerator.FreeTypeFontParameter();
            config.size = size;
            config.characters = FreeTypeFontGenerator.DEFAULT_CHARS
                + "áàâãéêíóôõúçÁÀÂÃÉÊÍÓÔÕÚÇ";
            config.minFilter = Texture.TextureFilter.Linear;
            config.magFilter = Texture.TextureFilter.Linear;
            if (outline) {
                config.borderWidth = 4f;
                config.borderColor = BROWN;
            }
            return generator.generateFont(config);
        } finally {
            generator.dispose();
        }
    }

    @Override
    public void dispose() {
        for (Texture texture : textures) {
            texture.dispose();
        }
        displayFont.dispose();
        titleFont.dispose();
        textFont.dispose();
        captionFont.dispose();
    }
}
