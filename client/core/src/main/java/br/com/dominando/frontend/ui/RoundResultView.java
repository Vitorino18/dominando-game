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
    private final Image humanTitle = optionalTitle("rodada-sua.png", 600f, 690f, 720f, 220f);
    private final Image opponentTitle = optionalTitle("rodada-adversario.png", 600f, 690f, 720f, 220f);
    private final Label fallbackTitle;
    private final Label description;
    private final Label rewardLabel;
    private final CountCard humanCard;
    private final CountCard opponentCard;
    private final Image humanCap;
    private final Image opponentCap;
    private final Image capGlow;

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

        capGlow = addImage(createCapGlow(), 735f, 315f, 450f, 450f);
        humanCap = addImage(load(Gdx.files.internal("ui/score/tampinha-amarela.png")), 840f, 420f, 240f, 240f);
        opponentCap = addImage(load(Gdx.files.internal("ui/score/tampinha-verde.png")), 840f, 420f, 240f, 240f);
        humanCap.setScaling(Scaling.fit);
        opponentCap.setScaling(Scaling.fit);

        description = label("", textFont, CREAM, 510f, 330f, 900f, 100f);
        description.setWrap(true);
        addActor(description);

        this.rewardLabel = label("+1 TAMPINHA", displayFont, CREAM, 1036f, 96f, 330f, 80f);
        rewardLabel.setFontScale(0.5f);
        rewardLabel.setAlignment(Align.left);
        rewardLabel.setVisible(false);
        addActor(rewardLabel);
    }

    private Texture createCapGlow() {
        int size = 128;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        try {
            pixmap.setBlending(Pixmap.Blending.None);
            float center = (size - 1) / 2f;

            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    float distance = (float) Math.hypot(x - center, y - center) / center;
                    float falloff = Math.max(0f, 1f - distance);
                    float rim = Math.max(0f, Math.min(1f, (distance - 0.44f) / 0.14f));
                    float alpha = 0.85f * rim * falloff * falloff;
                    pixmap.drawPixel(x, y, Color.rgba8888(1f, 0.77f, 0.30f, alpha));
                }
            }

            return own(new Texture(pixmap));
        } finally {
            pixmap.dispose();
        }
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

    public void showWinningPlay() {
        resetContent();

        if (humanTitle != null) {
            humanTitle.setBounds(708f, 920f, 504f, 154f);
        }
        fallbackTitle.setY(872f);
        showTitle(humanTitle, "RODADA SUA!");

        description.setVisible(false);
        reveal();
    }

    public void showWinningCap() {
        float centerX = getWidth() / 2f;
        float capSize = 120f;
        float gap = 16f;
        float textWidth = rewardLabel.getPrefWidth();
        float totalWidth = capSize + gap + textWidth;
        float capX = centerX - totalWidth / 2f;

        humanCap.setBounds(capX, 76f, capSize, capSize);
        capGlow.setBounds(capX + (capSize - 220f) / 2f, 26f, 220f, 220f);
        rewardLabel.setBounds(capX + capSize + gap, 96f, textWidth, 80f);

        capGlow.setVisible(true);
        humanCap.setVisible(true);
        rewardLabel.setVisible(true);
    }

    public void showRoundWinner(boolean humanWon) {
        resetContent();
        capGlow.setVisible(true);
        showTitle(humanWon ? humanTitle : opponentTitle,
            humanWon ? "RODADA SUA!" : "RODADA DO ADVERSÁRIO");
        (humanWon ? humanCap : opponentCap).setVisible(true);
        description.setPosition(510f, 295f);
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
        capGlow.setVisible(false);
        fallbackTitle.setY(730f);
        capGlow.setBounds(735f, 315f, 450f, 450f);
        description.setVisible(true);
        rewardLabel.setVisible(false);
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
