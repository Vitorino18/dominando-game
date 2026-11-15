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
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.math.MathUtils;

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
    private final Image humanCap;
    private final Image opponentCap;
    private final RoundCountReceipt roundCountReceipt;
    private final Image capGlow;
    private final Image backCoin;
    private final Image frontCoin;
    private final Label coinRewardLabel;
    private final Image closedRoundDimmer;
    private final FinalReceipt finalReceipt;

    public RoundResultView(Runnable onReturnToStart, Runnable onPlayAgain) {
        setSize(1920f, 1080f);
        setTouchable(Touchable.disabled);
        setVisible(false);

        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        try {
            pixel.setColor(0f, 0f, 0f, 0.22f);
            pixel.fill();
            closedRoundDimmer = addImage(own(new Texture(pixel)), 0f, 0f, getWidth(), getHeight());
            closedRoundDimmer.toBack();
        } finally {
            pixel.dispose();
        }

        fallbackTitle = label("", titleFont, CREAM, 480f, 730f, 960f, 240f);
        addActor(fallbackTitle);

        capGlow = addImage(createCapGlow(), 735f, 315f, 450f, 450f);
        humanCap = addImage(load(Gdx.files.internal("ui/score/tampinha-amarela.png")), 840f, 420f, 240f, 240f);
        opponentCap = addImage(load(Gdx.files.internal("ui/score/tampinha-verde.png")), 840f, 420f, 240f, 240f);
        humanCap.setScaling(Scaling.fit);
        opponentCap.setScaling(Scaling.fit);
        roundCountReceipt = new RoundCountReceipt();
        addActor(roundCountReceipt);

        description = label("", textFont, CREAM, 510f, 330f, 900f, 100f);
        description.setWrap(true);
        addActor(description);

        this.rewardLabel = label("+1 TAMPINHA", displayFont, CREAM, 1036f, 96f, 330f, 80f);
        rewardLabel.setFontScale(0.5f);
        rewardLabel.setAlignment(Align.left);
        rewardLabel.setVisible(false);
        addActor(rewardLabel);

        Texture coinTexture = load(Gdx.files.internal("ui/result/moeda.png"));

        backCoin = addImage(coinTexture, 0f, 0f, 96f, 96f);
        frontCoin = addImage(coinTexture, 0f, 0f, 96f, 96f);

        coinRewardLabel = label("+2 MOEDAS", displayFont, CREAM, 0f, 0f, 330f, 80f);
        coinRewardLabel.setFontScale(0.5f);
        coinRewardLabel.setAlignment(Align.left);
        addActor(coinRewardLabel);

        finalReceipt = new FinalReceipt(onReturnToStart, onPlayAgain);
        addActor(finalReceipt);
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

        closedRoundDimmer.setVisible(true);
        roundCountReceipt.show(tie, humanWon, human, opponent);

        description.setVisible(tie);
        if (tie) {
            description.setPosition(510f, 327f);
            description.setText(
                "Ninguém recebe tampinha ou moedas.\n"
                    + "Vamos embaralhar e distribuir novamente."
            );
        }

        reveal();
    }

    public Group createRedistributionMessage() {
        Group message = new Group();
        message.setTouchable(Touchable.disabled);
        message.setVisible(false);

        Label title = label("EMBARALHANDO...", displayFont, CREAM, 560f, 848f, 800f, 72f);
        title.setFontScale(0.6f);
        message.addActor(title);

        message.addActor(label(
            "Uma nova rodada vai começar", textFont, CREAM, 560f, 176f, 800f, 44f
        ));

        return message;
    }

    public void showWinningPlay(boolean humanWon) {
        resetContent();

        Image artwork = humanWon ? humanTitle : opponentTitle;
        if (artwork != null) {
            artwork.setBounds(708f, 920f, 504f, 154f);
        }

        fallbackTitle.setY(872f);
        showTitle(artwork, humanWon ? "RODADA SUA!" : "RODADA DO ADVERSÁRIO");

        description.setVisible(false);
        reveal();
    }

    public void showWinningCap(boolean humanWon) {
        Image cap = humanWon ? humanCap : opponentCap;
        float centerX = getWidth() / 2f;
        float capSize = 120f;
        float gap = 16f;
        float textWidth = rewardLabel.getPrefWidth();
        float totalWidth = capSize + gap + textWidth;
        float capX = centerX - totalWidth / 2f;

        cap.setBounds(capX, 76f, capSize, capSize);
        capGlow.setBounds(capX + (capSize - 220f) / 2f, 26f, 220f, 220f);
        capGlow.setColor(humanWon ? 1f : 0.45f, 1f, 1f, 1f);
        rewardLabel.setBounds(capX + capSize + gap, 96f, textWidth, 80f);

        capGlow.setVisible(true);
        cap.setVisible(true);
        rewardLabel.setVisible(true);
    }

    public void showWinningCoins() {
        humanCap.setVisible(false);
        capGlow.setVisible(false);
        rewardLabel.setVisible(false);

        float coinSize = 96f;
        float overlapOffset = 24f;
        float gap = 16f;
        float pileWidth = coinSize + overlapOffset;
        float textWidth = coinRewardLabel.getPrefWidth();
        float pileX = (getWidth() - pileWidth - gap - textWidth) / 2f;

        float backY = 76f;
        float frontX = pileX + overlapOffset;
        float frontY = backY + overlapOffset;

        backCoin.setPosition(pileX, backY);
        backCoin.setVisible(true);

        frontCoin.setPosition(frontX, frontY + 16f);
        frontCoin.setVisible(false);

        coinRewardLabel.setBounds(pileX + pileWidth + gap, 96f, textWidth, 80f);
        coinRewardLabel.setVisible(false);

        frontCoin.addAction(Actions.sequence(
            Actions.delay(0.10f),
            Actions.run(() -> frontCoin.setVisible(true)),
            Actions.moveTo(frontX, frontY, 0.15f, Interpolation.pow2In),
            Actions.moveTo(frontX, frontY + 7f, 0.08f, Interpolation.pow2Out),
            Actions.moveTo(frontX, frontY, 0.10f, Interpolation.pow2In),
            Actions.run(() -> coinRewardLabel.setVisible(true))
        ));
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
        closedRoundDimmer.setVisible(true);
        description.setVisible(false);
        finalReceipt.show(humanWon, human, opponent);
        setTouchable(Touchable.enabled);
        reveal();
    }

    public boolean isFinalReceiptVisible() {
        return isVisible() && finalReceipt.isVisible();
    }

    public void showRematchWaiting() {
        finalReceipt.showWaiting();
    }

    private void resetContent() {
        if (closedTitle != null) closedTitle.setVisible(false);
        if (tieTitle != null) tieTitle.setVisible(false);
        if (humanTitle != null) humanTitle.setVisible(false);
        if (opponentTitle != null) opponentTitle.setVisible(false);
        fallbackTitle.setVisible(false);
        humanCap.setVisible(false);
        opponentCap.setVisible(false);
        capGlow.setVisible(false);
        capGlow.setColor(Color.WHITE);
        fallbackTitle.setY(730f);
        capGlow.setBounds(735f, 315f, 450f, 450f);
        description.setVisible(true);
        rewardLabel.setVisible(false);
        backCoin.setVisible(false);
        frontCoin.clearActions();
        frontCoin.setVisible(false);
        coinRewardLabel.setVisible(false);
        roundCountReceipt.setVisible(false);
        closedRoundDimmer.setVisible(false);
        finalReceipt.setVisible(false);
        setTouchable(Touchable.disabled);
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

    private final class FinalReceipt extends Group {
        private final Label headline;
        private final Label detail;
        private final Label humanScore;
        private final Label opponentScore;
        private final TextButton playAgainButton;
        private final Label receiptFooter;
        private final Group loadingCaps = new Group();
        private final Image[] waitingCaps = new Image[3];
        private float waitingTime;

        FinalReceipt(Runnable onReturnToStart, Runnable onPlayAgain) {
            setBounds(610f, 120f, 700f, 840f);
            setTouchable(Touchable.childrenOnly);
            setVisible(false);

            Image paper = new Image(load(Gdx.files.internal("ui/result/comanda-final.png")));
            paper.setSize(700f, 840f);
            addActor(paper);

            Image logo = new Image(load(Gdx.files.internal("ui/result/marca-comanda-final.png")));
            logo.setBounds(270f, 680f, 160f, 112f);
            logo.setScaling(Scaling.fit);
            addActor(logo);

            addActor(label("COMANDA FINAL", textFont, GREEN, 150f, 632f, 400f, 28f));

            headline = label("", displayFont, BROWN, 110f, 548f, 480f, 56f);
            headline.setFontScale(0.5f);
            addActor(headline);

            detail = label("", textFont, GREEN, 110f, 508f, 480f, 32f);
            addActor(detail);

            addActor(label("VOCÊ", textFont, GREEN, 130f, 456f, 180f, 28f));
            addActor(label("ADVERSÁRIO", textFont, GREEN, 390f, 456f, 180f, 28f));

            humanScore = label("", displayFont, GREEN, 130f, 376f, 180f, 72f);
            opponentScore = label("", displayFont, GREEN, 390f, 376f, 180f, 72f);
            humanScore.setFontScale(0.8f);
            opponentScore.setFontScale(0.8f);
            addActor(humanScore);
            addActor(opponentScore);

            addActor(createFinalButton("VOLTAR AO INÍCIO", 244f, CREAM, GREEN, onReturnToStart));
            playAgainButton = createFinalButton("JOGAR DE NOVO", 136f, Color.valueOf("F0B639"), BROWN, onPlayAgain);
            addActor(playAgainButton);

            loadingCaps.setTouchable(Touchable.disabled);
            loadingCaps.setVisible(false);

            for (int i = 0; i < waitingCaps.length; i++) {
                Image cap = new Image(humanCap.getDrawable());
                cap.setScaling(Scaling.fit);
                cap.setBounds(242f + 78f * i, 130f, 60f, 60f);
                waitingCaps[i] = cap;
                loadingCaps.addActor(cap);
            }

            addActor(loadingCaps);

            receiptFooter = label("A saideira pode esperar", captionFont, BROWN, 150f, 64f, 400f, 32f);
            addActor(receiptFooter);
        }

        void show(boolean humanWon, int human, int opponent) {
            playAgainButton.setVisible(true);
            loadingCaps.setVisible(false);
            receiptFooter.setText("A saideira pode esperar");
            receiptFooter.setY(64f);
            headline.setText(humanWon ? "A PARTIDA É SUA!" : "O ADVERSÁRIO VENCEU!");
            detail.setText(humanWon ? "Você conquistou " + human + " tampinhas." : "O adversário conquistou " + opponent + " tampinhas.");
            humanScore.setText(Integer.toString(human));
            opponentScore.setText(Integer.toString(opponent));
            setVisible(true);
        }

        void showWaiting() {
            headline.setText("MAIS UMA?");
            detail.setText("Pedido enviado ao adversário");
            playAgainButton.setVisible(false);
            receiptFooter.setText("Aguardando o adversário...");
            receiptFooter.setY(82f);
            waitingTime = 0f;
            loadingCaps.setVisible(true);
        }

        @Override
        public void act(float delta) {
            super.act(delta);

            if (!loadingCaps.isVisible() || !RoundResultView.this.isVisible()) {
                return;
            }

            float stepDuration = 0.45f;
            float cycleDuration = stepDuration * waitingCaps.length;
            waitingTime = (waitingTime + delta) % cycleDuration;

            int active = Math.min(waitingCaps.length - 1, (int) (waitingTime / stepDuration));
            float progress = (waitingTime - active * stepDuration) / stepDuration;
            float pulse = MathUtils.sin(MathUtils.PI * progress);
            float baseAlpha = 0.28f;

            for (int i = 0; i < waitingCaps.length; i++) {
                waitingCaps[i].getColor().a = i == active
                    ? MathUtils.lerp(baseAlpha, 1f, pulse)
                    : baseAlpha;
            }
        }
    }

    private TextButton createFinalButton(
        String text, float y, Color fill, Color ink, Runnable onClick
    ) {
        Pixmap pixmap = new Pixmap(440, 96, Pixmap.Format.RGBA8888);
        Texture texture;
        try {
            rounded(pixmap, BROWN, 0, 8, 440, 88, 18);
            rounded(pixmap, BROWN, 0, 0, 440, 88, 18);
            rounded(pixmap, fill, 5, 5, 430, 78, 13);
            texture = own(new Texture(pixmap));
        } finally {
            pixmap.dispose();
        }

        TextureRegionDrawable up = new TextureRegionDrawable(new TextureRegion(texture));
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle(
            up, up.tint(Color.valueOf("EAD8BB")), null, displayFont
        );
        style.fontColor = ink;
        style.pressedOffsetY = -2f;

        TextButton button = new TextButton(text, style);
        button.getLabel().setFontScale(0.45f);
        button.padBottom(8f);
        button.setBounds(130f, y, 440f, 96f);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onClick.run();
            }
        });
        return button;
    }

    private final class RoundCountReceipt extends Group {
        private final Label humanPoints;
        private final Label opponentPoints;
        private final Image winnerCap;
        private final Label winnerStamp;

        RoundCountReceipt() {
            setBounds(610f, 455f, 700f, 260f);

            Image paper = new Image(load(Gdx.files.internal("ui/result/comanda-rodada.png")));
            paper.setSize(700f, 260f);
            addActor(paper);

            addActor(label("VOCÊ", textFont, GREEN, 50f, 177f, 250f, 44f));
            addActor(label("ADVERSÁRIO", textFont, GREEN, 400f, 177f, 250f, 44f));

            humanPoints = label("", displayFont, GREEN, 50f, 76f, 250f, 96f);
            opponentPoints = label("", displayFont, GREEN, 400f, 76f, 250f, 96f);
            addActor(humanPoints);
            addActor(opponentPoints);
            addActor(label("PONTOS", captionFont, BROWN, 250f, 36f, 200f, 36f));

            winnerCap = new Image();
            winnerCap.setSize(50f, 50f);
            addActor(winnerCap);

            winnerStamp = label("VENCEU A RODADA", displayFont, GREEN, 50f, 48f, 250f, 30f);
            winnerStamp.setFontScale(0.30f);
            addActor(winnerStamp);
        }

        void show(boolean tie, boolean humanWon, int human, int opponent) {
            humanPoints.setText(Integer.toString(human));
            opponentPoints.setText(Integer.toString(opponent));

            winnerCap.setVisible(!tie);
            winnerStamp.setVisible(!tie);

            if (!tie) {
                Label winningPoints = humanWon ? humanPoints : opponentPoints;
                Image cap = humanWon ? humanCap : opponentCap;
                float scoreCenterX = winningPoints.getX() + winningPoints.getWidth() / 2f;
                float scoreRight = scoreCenterX + winningPoints.getPrefWidth() / 2f;

                winnerCap.setDrawable(cap.getDrawable());
                winnerCap.setPosition(scoreRight + 12f, 99f);
                winnerStamp.setX(winningPoints.getX());
                winnerStamp.setColor(humanWon ? BROWN : GREEN);
            }

            clearActions();
            setY(473f);
            getColor().a = 0f;
            setVisible(true);
            addAction(Actions.sequence(
                Actions.delay(0.20f),
                Actions.parallel(
                    Actions.moveTo(getX(), 455f, 0.22f, Interpolation.smooth),
                    Actions.fadeIn(0.22f, Interpolation.smooth)
                )
            ));
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
