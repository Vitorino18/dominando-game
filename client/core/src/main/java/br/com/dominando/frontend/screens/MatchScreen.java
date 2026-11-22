package br.com.dominando.frontend.screens;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

import br.com.dominando.frontend.settings.GameSettings;
import br.com.dominando.frontend.ui.OpeningDialogs;
import br.com.dominando.frontend.match.LocalDominoRound;
import br.com.dominando.frontend.match.LocalDominoRound.Placement;
import br.com.dominando.frontend.match.LocalDominoRound.Player;
import br.com.dominando.frontend.match.LocalDominoRound.Side;
import br.com.dominando.frontend.match.LocalDominoRound.Stone;
import br.com.dominando.frontend.ui.DominoAssets;
import br.com.dominando.frontend.ui.DominoTile;
import br.com.dominando.frontend.ui.CavaView;
import br.com.dominando.frontend.ui.ScoreView;
import br.com.dominando.frontend.ui.PlayerInfoView;
import br.com.dominando.frontend.ui.RoundResultView;
import br.com.dominando.frontend.ui.RoundResultView.MatchEndReason;
import br.com.dominando.frontend.ui.IntervalView;
import br.com.dominando.frontend.ui.SnackActionView;
import br.com.dominando.frontend.ui.IntervalView.Snack;


import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.EnumSet;
import java.util.Set;

public class MatchScreen extends ScreenAdapter {
    private static final float WIDTH = 1920f;
    private static final float HEIGHT = 1080f;
    private static final float ENTRANCE_DURATION = 4.4f;
    private static final float BAR_AMBIENCE_DUCK_START = ENTRANCE_DURATION - 0.50f;
    private static final float BAR_AMBIENCE_TAIL_DURATION = 1.20f;
    private static final float BAR_AMBIENCE_END = ENTRANCE_DURATION + BAR_AMBIENCE_TAIL_DURATION;
    private static final float BAR_AMBIENCE_SHUFFLE_LEVEL = 0.25f;
    private static final float SHUFFLE_DURATION = 2.8f;
    private static final float SHUFFLE_END = ENTRANCE_DURATION + SHUFFLE_DURATION;
    private static final float DEAL_SLIDE = 0.45f;
    private static final float DEAL_INTERVAL = 0.06f;
    private static final float DEAL_DURATION = 6 * DEAL_INTERVAL + DEAL_SLIDE;
    private static final float PREPARATION_END = SHUFFLE_END + DEAL_DURATION + 0.35f;
    private static final float STONE_WIDTH = 60f;
    private static final float STONE_HEIGHT = 111.82f;
    private static final int CAPS_TO_WIN = 5;
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
    private final Group hudLayer;
    private final Group gameHud = new Group();
    private final Group shuffleLayer = new Group();

    private final CavaView cavaView;

    private final ScoreView playerScoreView;
    private final ScoreView opponentScoreView;

    private final PlayerInfoView playerInfoView;
    private final PlayerInfoView opponentInfoView;

    private final DominoTile leftEndPreview;
    private final DominoTile rightEndPreview;
    private final RoundResultView roundResultView;
    private final IntervalView intervalView;
    private final Texture intervalFadeTexture;
    private final Image intervalFade;
    private Runnable afterInterval;
    private int humanCoins;
    private final Set<IntervalView.Snack> boughtSnacks = EnumSet.noneOf(IntervalView.Snack.class);
    private final BitmapFont controlsFont;
    private final Random random = new Random();
    private final Map<Stone, DominoTile> handActors = new IdentityHashMap<>();
    private final Map<Placement, DominoTile> chainActors = new IdentityHashMap<>();
    private LocalDominoRound round;
    private Player roundOpeningPreference;
    private Stone selectedStone;
    private final List<ChainMove> chainMoves = new ArrayList<>();
    private int chainSizePercent = 100;
    private boolean leavingResult;
    private boolean entranceFinished;
    private boolean movingStone;
    private boolean roundScored;
    private int humanCaps;
    private int opponentCaps;

    private final OpeningDialogs matchSettings;
    private final Texture matchSettingsIcon;
    private final CavaView dealCavaView;
    private final PassButton passButton;

    private boolean closedRoundPreview;

    private final Group redistributionMessage;
    private boolean redistributingTie;

    private final SnackActionView snackActionView;
    private Snack nextRoundSnack;
    private Snack roundSnack;
    private boolean snackUsed;
    private boolean choosingCoxinha;
    private boolean torresmoArmed;
    private Player snackTurn;
    private boolean humanDrewThisTurn;
    private Stone revealedStone;
    private float revealRemaining;
    private static final float BAR_AMBIENCE_VOLUME = 0.45f;

    private final GameSettings gameSettings = new GameSettings();

    private final Music barAmbience;
    private final Sound shuffleSound;
    private final Sound dealSound;
    private final Sound drawSound;
    private final Sound passTapSound;
    private final Sound roundVictorySound;
    private final Sound coinRewardSound;
    private final Sound snackPurchaseSound;
    private final Runnable onUiClick;


    public MatchScreen(Runnable onLeaveMatch, Runnable onUiClick) {
        this.onUiClick = onUiClick;

        barAmbience = Gdx.audio.newMusic(Gdx.files.internal("audio/ambiente-bar.ogg"));
        shuffleSound = Gdx.audio.newSound(Gdx.files.internal("audio/embaralhar-domino.wav"));
        dealSound = Gdx.audio.newSound(Gdx.files.internal("audio/distribuir-domino.mp3"));
        drawSound = Gdx.audio.newSound(Gdx.files.internal("audio/cavar-domino.mp3"));
        passTapSound = Gdx.audio.newSound(Gdx.files.internal("audio/batida-mesa.mp3"));
        roundVictorySound = Gdx.audio.newSound(Gdx.files.internal("audio/vitoria-rodada-mesa.wav"));
        coinRewardSound = Gdx.audio.newSound(Gdx.files.internal("audio/moedas-recompensa.wav"));
        snackPurchaseSound = Gdx.audio.newSound(Gdx.files.internal("audio/compra-petisco.wav"));

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

        floorTexture = new Texture(Gdx.files.internal("ui/match/chao-vista-superior.png"));
        floorTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        tableTexture = new Texture(Gdx.files.internal("ui/match/mesa-vista-superior.png"));
        tableTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        floorBackground = new Image(floorTexture);
        floorBackground.setBounds(0f, 0f, WIDTH, HEIGHT);
        floorBackground.setTouchable(Touchable.disabled);
        floorBackground.setVisible(false);

        stage.addActor(floorBackground);

        tableImage = new Image(tableTexture);
        tableImage.setBounds(404f, HEIGHT - 25f - 1053f, 1140f, 1053f);
        tableImage.setTouchable(Touchable.disabled);
        tableImage.setVisible(false);

        stage.addActor(tableImage);
        stage.addActor(seatedBackground);


        dominoAssets = new DominoAssets();

        shuffleLayer.setTouchable(Touchable.disabled);
        shuffleLayer.setVisible(false);
        createShuffleTiles();
        stage.addActor(shuffleLayer);

        tilesLayer = new Group();
        tilesLayer.setSize(WIDTH,HEIGHT);
        tilesLayer.setTouchable(Touchable.childrenOnly);
        tilesLayer.setVisible(false);

        stage.addActor(tilesLayer);

        leftEndPreview = createEndPreview(Side.LEFT);
        rightEndPreview = createEndPreview(Side.RIGHT);

        tilesLayer.addActor(leftEndPreview);
        tilesLayer.addActor(rightEndPreview);

        cavaView = new CavaView(dominoAssets, 14);
        cavaView.setScale(CAVA_SCALE);
        cavaView.setPosition(CAVA_X, CAVA_Y);

        tilesLayer.addActor(cavaView);

        dealCavaView = new CavaView(dominoAssets, 14);
        dealCavaView.setScale(CAVA_SCALE);
        dealCavaView.setPosition(CAVA_X, CAVA_Y);
        dealCavaView.setTouchable(Touchable.disabled);
        dealCavaView.setVisible(false);
        stage.addActor(dealCavaView);

        playerScoreView = new ScoreView(false, 0);
        opponentScoreView = new ScoreView(true, 0);

        playerScoreView.setBounds(1320f, 65f, 176f, 144f);
        playerScoreView.setOrigin(88f, 72f);
        playerScoreView.setRotation(42.76f);

        opponentScoreView.setBounds(470f, 880f, 176f, 144f);
        opponentScoreView.setOrigin(88f, 72f);
        opponentScoreView.setRotation(-133.16f);

        tilesLayer.addActor(playerScoreView);
        tilesLayer.addActor(opponentScoreView);

        hudLayer = new Group();
        hudLayer.setSize(WIDTH, HEIGHT);
        hudLayer.setTouchable(Touchable.childrenOnly);
        hudLayer.setVisible(false);

        stage.addActor(hudLayer);

        gameHud.setSize(WIDTH, HEIGHT);
        gameHud.setTouchable(Touchable.childrenOnly);

        hudLayer.addActor(gameHud);

        opponentInfoView = new PlayerInfoView(false, "Adversário", 7);
        opponentInfoView.setPosition(48f, HEIGHT - 48f - PlayerInfoView.TOTAL_HEIGHT);

        gameHud.addActor(opponentInfoView);

        playerInfoView = new PlayerInfoView(true, "Você", 7);
        playerInfoView.setPosition(1544f, HEIGHT - 920f - PlayerInfoView.TOTAL_HEIGHT);

        gameHud.addActor(playerInfoView);

        snackActionView = new SnackActionView(this::useSnack);
        gameHud.addActorBefore(playerInfoView, snackActionView);

        matchSettings = new OpeningDialogs(gameSettings, () -> {}, onLeaveMatch);
        matchSettingsIcon = new Texture(Gdx.files.internal("ui/icons/engrenagem-partida.png"));
        matchSettingsIcon.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        ImageButton settingsButton = new ImageButton(
            new TextureRegionDrawable(new TextureRegion(matchSettingsIcon))
        );
        settingsButton.getImageCell().size(48f, 48f);
        settingsButton.setBounds(1776f, HEIGHT - 48f - 96f, 96f, 96f);
        settingsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onUiClick.run();
                matchSettings.open(stage);
            }
        });

        gameHud.addActor(settingsButton);

        controlsFont = createControlsFont();

        passButton = new PassButton(controlsFont);
        passButton.setPosition(134f, 48f);
        passButton.setAvailable(false);
        passButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                registerPassTap();
            }
        });

        gameHud.addActor(passButton);

        roundResultView = new RoundResultView(
            () -> closeFinalResult(() -> Gdx.app.postRunnable(onLeaveMatch)),
            this::startRematchWaiting,
            onUiClick
        );
        hudLayer.addActor(roundResultView);

        intervalView = new IntervalView(onUiClick);
        hudLayer.addActor(intervalView);

        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        try {
            pixel.setColor(Color.BLACK);
            pixel.fill();
            intervalFadeTexture = new Texture(pixel);
        } finally {
            pixel.dispose();
        }

        intervalFade = new Image(intervalFadeTexture);
        intervalFade.setSize(WIDTH, HEIGHT);
        intervalFade.setTouchable(Touchable.enabled);

        redistributionMessage = roundResultView.createRedistributionMessage();
        stage.getRoot().addActorBefore(hudLayer, redistributionMessage);

        cavaView.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (canAct() && !choosingCoxinha) {
                    drawForCurrentPlayer();
                }
            }
        });

        startRound(null);
    }

    private void playSnackPurchaseSound() {
        if (gameSettings.isSoundEffectsEnabled()) {
            snackPurchaseSound.play(0.70f);
        }
    }

    private void playRoundVictorySound() {
        if (gameSettings.isSoundEffectsEnabled()) {
            roundVictorySound.play(0.75f);
        }
    }

    private void showCoinReward() {
        roundResultView.showWinningCoins();

        if (gameSettings.isSoundEffectsEnabled()) {
            coinRewardSound.play(0.70f);
        }
    }

    private void showRoundCounts(
        boolean tie, boolean humanWon, int human, int opponent
    ) {
        roundResultView.showCounts(tie, humanWon, human, opponent);

        if (!tie && humanWon) {
            roundResultView.addAction(Actions.sequence(
                Actions.delay(0.20f),
                Actions.run(this::playRoundVictorySound)
            ));
        }
    }

    private void playDealSounds(float previousTime) {
        float firstArrival = SHUFFLE_END + DEAL_SLIDE - 0.05f;
        float lastArrival = firstArrival + 6 * DEAL_INTERVAL;

        if (entranceTime < firstArrival || previousTime >= lastArrival
            || !gameSettings.isSoundEffectsEnabled()) {
            return;
        }

        for (int pair = 0; pair < 7; pair++) {
            float arrival = firstArrival + pair * DEAL_INTERVAL;
            if (previousTime < arrival && entranceTime >= arrival) {
                dealSound.play(0.45f);
            }
        }
    }

    private void playPassTap() {
        if (gameSettings.isSoundEffectsEnabled()) {
            passTapSound.play(0.70f);
        }
    }

    private void playShuffleSound() {
        if (gameSettings.isSoundEffectsEnabled()) {
            shuffleSound.play(0.85f);
        }
    }

    private void fadeInterval(Runnable atBlack) {
        stage.cancelTouchFocus();
        intervalFade.clearActions();
        intervalFade.getColor().a = 0f;
        stage.addActor(intervalFade);

        intervalFade.addAction(Actions.sequence(
            Actions.fadeIn(0.45f, Interpolation.smooth),
            Actions.run(atBlack),
            Actions.fadeOut(0.45f, Interpolation.smooth),
            Actions.removeActor()
        ));
    }

    private void openInterval(int balance, Set<IntervalView.Snack> alreadyBought,boolean boughtThisInterval, boolean refuseNextOrder, Runnable onFinished) {
        afterInterval = onFinished;
        fadeInterval(() -> {
            roundResultView.setVisible(false);
            intervalView.open(balance, alreadyBought, boughtThisInterval, refuseNextOrder, snack -> {
                if (onFinished != null) {
                    humanCoins -= snack.getPrice();
                    boughtSnacks.add(snack);
                    nextRoundSnack = snack;
                }

                playSnackPurchaseSound();
            });
        });
    }

    private void createShuffleTiles() {
        float[][] poses = {
            {697.6f, 325.8f, -18f}, {782f, 346.7f, 12f}, {879.6f, 318.5f, -30f},
            {963.8f, 341.3f, 22f}, {1044.8f, 320.2f, -10f}, {1144.4f, 350.4f, 32f},
            {1227.8f, 324.3f, -22f}, {720.7f, 456.5f, 28f}, {802.4f, 431.5f, -16f},
            {896.6f, 455.9f, 38f}, {975.7f, 428.5f, -26f}, {1058.4f, 447.5f, 16f},
            {1159.5f, 432.2f, -35f}, {1226.8f, 460.2f, 10f}, {696.7f, 606.5f, -28f},
            {782.7f, 630.1f, 20f}, {866f, 596.7f, -12f}, {969.2f, 622.3f, 34f},
            {1058.8f, 600.4f, -24f}, {1139.3f, 627.1f, 14f}, {1234.6f, 601.9f, -38f},
            {712.4f, 731.5f, 16f}, {808.4f, 708.4f, -32f}, {895.7f, 736.5f, 26f},
            {977.3f, 713.1f, -14f}, {1075.9f, 740.1f, 36f}, {1154.7f, 710.1f, -20f},
            {1231.5f, 727.7f, 8f}
        };

        for (int i = 0; i < poses.length; i++) {
            float[] pose = poses[i];
            Group position = new Group();
            position.setPosition(pose[0], HEIGHT - pose[1]);
            position.setUserObject(pose[2]);

            DominoTile tile = new DominoTile(dominoAssets, 0, 0);
            tile.setFaceDown(true);
            tile.setBounds(-22f, -41f, 44f, 82f);
            tile.setRotation(pose[2]);

            position.addActor(tile);
            shuffleLayer.addActor(position);
        }
    }

    private void updateShuffle() {
        boolean shuffling = entranceTime >= ENTRANCE_DURATION && entranceTime < PREPARATION_END;
        shuffleLayer.setVisible(shuffling);

        float dealTime = entranceTime - SHUFFLE_END;
        dealCavaView.setVisible(shuffling && dealTime >= DEAL_DURATION);

        if (!shuffling) {
            return;
        }

        float progress = Math.min(1f, (entranceTime - ENTRANCE_DURATION) / SHUFFLE_DURATION);
        float easedProgress = smooth(progress);
        float centerX = 974f;
        float centerY = 540f;
        float verticalScale = 0.82f;
        double fullTurn = 2 * Math.PI;
        double pull = Math.sin(Math.PI * progress);

        for (int i = 0; i < shuffleLayer.getChildren().size; i++) {
            Group position = (Group) shuffleLayer.getChildren().get(i);
            float startX = position.getX();
            float startY = position.getY();

            float offsetX = startX - centerX;
            float offsetY = (startY - centerY) / verticalScale;
            double turns = 0.68 + 0.07 * (i % 4);
            double angle = fullTurn * turns * easedProgress;

            double distanceFromCenter = Math.hypot(offsetX, offsetY);
            double centerWeight = 1.0 - Math.min(distanceFromCenter / 320.0, 1.0);
            double pullStrength = 0.20 + 0.12 * centerWeight;

            double radiusScale = 1.0 - pullStrength * pull * pull
                * (1.0 + Math.sin(fullTurn * progress + i * 1.7));

            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            float x = centerX + (float) (radiusScale * (offsetX * cos - offsetY * sin));
            float y = centerY + (float) (verticalScale * radiusScale * (offsetX * sin + offsetY * cos));

            DominoTile tile = (DominoTile) position.getChildren().first();
            placeDealtTile(tile, i, x, y, startX, startY, (Float) position.getUserObject(), dealTime);
        }
    }

    private void placeDealtTile(DominoTile tile, int index, float x, float y,
                                float startX, float startY, float rotation, float dealTime) {
        float width = 44f;
        float height = 82f;
        boolean toCava = index >= 14;
        boolean visible = true;

        if (dealTime >= 0f) {
            int slot = index % 7;
            float slide = Math.max(0f, Math.min(1f,
                (dealTime - slot * DEAL_INTERVAL) / DEAL_SLIDE));
            float progress = smooth(slide);
            float targetX;
            float targetY;

            if (toCava) {
                targetX = CAVA_X + dealCavaView.getWidth() * CAVA_SCALE / 2f;
                targetY = CAVA_Y + dealCavaView.getHeight() * CAVA_SCALE / 2f;
                visible = slide < 1f;
            } else {
                Player player = index < 7 ? Player.OPPONENT : Player.HUMAN;
                Stone stone = round.hand(player).get(slot);
                DominoTile target = handActors.get(stone);
                targetX = target.getX() + target.getWidth() / 2f;
                targetY = target.getY() + target.getHeight() / 2f;
                width += (target.getWidth() - width) * progress;
                height += (target.getHeight() - height) * progress;
            }

            x += (targetX - x) * progress;
            y += (targetY - y) * progress;
            rotation *= 1f - progress;
        }

        tile.setVisible(visible);
        tile.setBounds(x - startX - width / 2f, y - startY - height / 2f, width, height);
        tile.setRotation(rotation);
    }

    private void updateEntrance() {
        float fadeProgress = Math.min(entranceTime / 0.8f, 1f);
        float fade = fadeProgress * (3f - 2f * fadeProgress);

        background.getColor().a = fade;

        if (entranceTime < BAR_AMBIENCE_END) {
            float duckProgress = Math.max(0f, Math.min(1f, (entranceTime - BAR_AMBIENCE_DUCK_START) / (ENTRANCE_DURATION - BAR_AMBIENCE_DUCK_START)));
            float tailProgress = Math.max(0f, Math.min(1f, (entranceTime - ENTRANCE_DURATION) / BAR_AMBIENCE_TAIL_DURATION));
            float duck = 1f - (1f - BAR_AMBIENCE_SHUFFLE_LEVEL) * smooth(duckProgress);
            float tail = 1f - smooth(tailProgress);
            barAmbience.setVolume(BAR_AMBIENCE_VOLUME * fade * duck * tail);
        }

        float moveProgress = Math.min(entranceTime / 2f, 1f);
        float movement = moveProgress * moveProgress * (3f - 2f * moveProgress);
        float scale = 1f + 0.12f * movement;
        float width = WIDTH * scale;
        float height = HEIGHT * scale;

        background.setBounds((WIDTH - width) * 0.5f, (HEIGHT - height) * 0.45f, width, height);

        float seatedProgress = Math.max(0f, Math.min((entranceTime - 2f) / 0.9f, 1f));
        background.setVisible(seatedProgress < 1f);

        float seatedExit = Math.max(0f, Math.min((entranceTime - 3.5f) / 0.9f, 1f));
        seatedBackground.getColor().a = smooth(seatedProgress) * (1f - smooth(seatedExit));

        boolean showTop = entranceTime >= 3.5f;
        floorBackground.setVisible(showTop);
        tableImage.setVisible(showTop);

        updateShuffle();
    }

    private float smooth(float progress) {
        return progress * progress * (3f - 2f * progress);
    }

    private DominoTile createEndPreview(Side side) {
        DominoTile preview = new DominoTile(dominoAssets, 0, 0);
        preview.getColor().a = 0.42f;
        preview.setVisible(false);
        preview.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (canAct() && preview.isVisible()) {
                    playStone(selectedStone, side);
                }
            }
        });
        return preview;
    }

    private void startRound(Player preferredStarter) {
        startRound(preferredStarter, random);
    }

    private void startRound(Player preferredStarter, Random dealRandom) {
        clearEggReveal();
        roundSnack = nextRoundSnack;
        nextRoundSnack = null;
        snackUsed = false;
        choosingCoxinha = false;
        torresmoArmed = false;
        snackTurn = null;
        humanDrewThisTurn = false;
        showRoundSnack();
        passButton.setAvailable(false);
        tilesLayer.clearActions();
        selectStone(null);

        for (DominoTile tile : handActors.values()) {
            tile.remove();
        }
        for (DominoTile tile : chainActors.values()) {
            tile.remove();
        }
        handActors.clear();
        chainActors.clear();

        chainMoves.clear();
        chainSizePercent = 100;
        leavingResult = false;
        cavaView.clearActions();
        roundScored = false;
        roundResultView.clearActions();
        roundResultView.setVisible(false);
        playerScoreView.setVisible(true);
        opponentScoreView.setVisible(true);
        playerInfoView.setVisible(true);
        opponentInfoView.setVisible(true);
        roundOpeningPreference = preferredStarter;
        round = new LocalDominoRound(dealRandom, preferredStarter);

        for (Player player : Player.values()) {
            for (Stone stone : round.hand(player)) {
                createHandActor(stone, player);
            }
        }

        layoutHand(Player.HUMAN);
        layoutHand(Player.OPPONENT);
        refreshTurn();
    }

    private void startRematchWaiting() {
        roundResultView.showRematchWaiting();

        Runnable response = closedRoundPreview && refuseRematchInPreview ? roundResultView::showRematchDeclined : this::startAcceptedRematch;

        roundResultView.addAction(Actions.delay(5f, Actions.run(response)));
    }

    private void startAcceptedRematch() {
        roundResultView.showRematchAccepted();
        roundResultView.addAction(Actions.delay(
            1.5f, Actions.run(() -> closeFinalResult(this::startNewMatch))
        ));
    }

    private void closeFinalResult(Runnable nextAction) {
        if (closedRoundPreview) {
            closeClosedRoundPreview();
            nextAction.run();
            return;
        }

        if (!matchIsFinished() || leavingResult) {
            return;
        }

        leavingResult = true;
        roundResultView.setTouchable(Touchable.disabled);
        roundResultView.clearActions();
        roundResultView.addAction(Actions.sequence(
            Actions.fadeOut(0.18f, Interpolation.smooth),
            Actions.run(nextAction)
        ));
    }

    private void startNewMatch() {
        humanCaps = 0;
        opponentCaps = 0;
        humanCoins = 0;
        boughtSnacks.clear();
        playerScoreView.setDisplayedScore(0);
        opponentScoreView.setDisplayedScore(0);
        nextRoundSnack = null;
        startRound(null);
    }

    private boolean intervalIsDue() {
        int decidedRounds = humanCaps + opponentCaps;
        return round.winner() != null
            && !matchIsFinished()
            && decidedRounds > 0
            && decidedRounds % 2 == 0;
    }

    private void finishRoundAfter(SequenceAction result) {
        if (!intervalIsDue()) {
            result.addAction(Actions.fadeOut(0.25f, Interpolation.smooth));
        }

        result.addAction(Actions.run(this::beginNextRound));
        roundResultView.addAction(result);
    }

    private void beginNextRound() {
        Player nextStarter = round.winner() != null ? round.winner() : roundOpeningPreference;

        if (round.isBlocked() && round.winner() == null) {
            startTieRedistribution(nextStarter);
            return;
        }

        if (matchIsFinished()) {
            startNewMatch();
            return;
        }
        if (intervalIsDue()) {
            openInterval(humanCoins, boughtSnacks, false, false,() -> startTieRedistribution(nextStarter));
            return;
        }



        startRound(nextStarter);
    }

    private void startTieRedistribution(Player preferredStarter) {
        redistributingTie = true;
        entranceFinished = false;
        entranceTime = ENTRANCE_DURATION;

        startRound(preferredStarter);

        for (DominoTile tile : handActors.values()) {
            tile.setVisible(false);
        }
        cavaView.setVisible(false);

        updateEntrance();
        playShuffleSound();
    }

    private void createHandActor(Stone stone, Player player) {
        boolean local = player == Player.HUMAN;
        DominoTile tile = local ? new DominoTile(dominoAssets, stone.first, stone.second) : new DominoTile(dominoAssets, 0, 0);

        if (local) {
            tile.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (!canAct()) {
                        return;
                    }

                    if (choosingCoxinha) {
                        swapWithCoxinha(stone);
                    } else {
                        selectStone(selectedStone == stone ? null : stone);
                    }
                }
            });
        } else {
            tile.setFaceDown(true);
            tile.setTouchable(Touchable.disabled);
        }

        handActors.put(stone, tile);
        tilesLayer.addActor(tile);
    }

    private void layoutHand(Player player) {
        List<Stone> hand = round.hand(player);
        if (hand.isEmpty()) {
            return;
        }

        boolean local = player == Player.HUMAN;
        float tileWidth = local ? 88f : 44f;
        float tileHeight = local ? 164f : 82f;
        float gap = local ? 12f : 10f;
        float availableWidth = local ? 720f : 480f;
        float centerY = local ? HUMAN_HAND_Y : OPPONENT_HAND_Y;

        if (local && hand.size() > 1) {
            float gapThatFits = (availableWidth - hand.size() * tileWidth) / (hand.size() - 1);
            gap = Math.max(-10f, Math.min(gap, gapThatFits));
        }

        float totalWidth = hand.size() * tileWidth + (hand.size() - 1) * gap;
        float scale = Math.min(1f, availableWidth / totalWidth);
        float startX = (WIDTH - totalWidth * scale) / 2f;

        for (int i = 0; i < hand.size(); i++) {
            DominoTile tile = handActors.get(hand.get(i));
            float x = startX + i * (tileWidth + gap) * scale;
            float y = centerY - tileHeight * scale / 2f;
            float width = tileWidth * scale;
            float height = tileHeight * scale;
            if (tile.getX() == 0f && tile.getY() == 0f) {
                tile.setBounds(x, y, width, height);
            } else {
                tile.clearActions();
                tile.addAction(Actions.parallel(
                    Actions.moveTo(x, y, 0.24f, Interpolation.smooth),
                    Actions.sizeTo(width, height, 0.24f, Interpolation.smooth)
                ));
            }
        }
    }

    private boolean canAct() {
        return entranceFinished && !movingStone && !round.isFinished()
            && round.turn() == Player.HUMAN;
    }

    private boolean canUseEgg() {
        return roundSnack == Snack.OVO && !snackUsed && !humanDrewThisTurn
            && !closedRoundPreview && !intervalView.isVisible()
            && stage.getActors().peek() == hudLayer
            && canAct() && !round.hand(Player.OPPONENT).isEmpty();
    }

    private void useEgg() {
        if (!canUseEgg()) {
            return;
        }

        List<Stone> opponentHand = round.hand(Player.OPPONENT);
        revealedStone = opponentHand.get(random.nextInt(opponentHand.size()));
        DominoTile tile = handActors.get(revealedStone);

        tile.setValues(revealedStone.first, revealedStone.second);
        tile.setFaceDown(false);
        revealRemaining = 5f;
        snackUsed = true;
        snackActionView.setButtonEnabled(false);
        snackActionView.highlightTile(tile, 1f);
    }

    private boolean canUseNewSnack() {
        return !snackUsed && !humanDrewThisTurn && !closedRoundPreview
            && !intervalView.isVisible() && stage.getActors().peek() == hudLayer
            && canAct();
    }

    private boolean canUseCoxinha() {
        return roundSnack == Snack.COXINHA && canUseNewSnack()
            && round.stockCount() > 0;
    }

    private boolean canUseTorresmo() {
        return roundSnack == Snack.TORRESMO && canUseNewSnack()
            && round.canDraw();
    }

    private void useSnack() {
        if (canUseEgg()) {
            onUiClick.run();
            useEgg();
            return;
        }

        if (canUseCoxinha()) {
            choosingCoxinha = !choosingCoxinha;
        } else if (canUseTorresmo()) {
            torresmoArmed = !torresmoArmed;
        } else {
            return;
        }

        onUiClick.run();
        selectStone(null);
        refreshTurn();
    }

    private void swapWithCoxinha(Stone offered) {
        if (!choosingCoxinha || !canUseCoxinha()) {
            return;
        }

        Stone replacement = round.swapStone(offered, random);
        if (replacement == null) {
            return;
        }

        choosingCoxinha = false;
        snackUsed = true;
        selectStone(null);

        DominoTile oldTile = handActors.remove(offered);
        oldTile.remove();
        createHandActor(replacement, Player.HUMAN);
        layoutHand(Player.HUMAN);

        snackActionView.showUsedSnack(Snack.COXINHA);
        refreshTurn();
        playerInfoView.setTurnPrompt("Peça trocada");
    }

    private void updateEggReveal(float delta) {
        if (revealedStone == null) {
            return;
        }

        revealRemaining -= delta;
        DominoTile tile = handActors.get(revealedStone);
        if (revealRemaining <= 0f || tile == null) {
            clearEggReveal();
            return;
        }

        snackActionView.highlightTile(tile, revealRemaining / 5f);
    }

    private void clearEggReveal() {
        DominoTile tile = handActors.get(revealedStone);
        if (tile != null) {
            tile.setFaceDown(true);
            tile.setValues(0, 0);
        }

        revealedStone = null;
        snackActionView.hideHighlight();
        if (roundSnack == Snack.OVO && snackUsed) {
            snackActionView.showUsedEgg();
        }
    }

    private void previewOtherSnacks() {
        boolean alt = Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT)
            || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT);

        if (!alt || !entranceFinished || movingStone || round.isFinished()
            || closedRoundPreview || roundResultView.isVisible()
            || stage.getActors().peek() != hudLayer) {
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            nextRoundSnack = Snack.COXINHA;
            startRound(Player.HUMAN, new Random(1L));
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            boolean shift = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
            nextRoundSnack = Snack.TORRESMO;
            startRound(Player.OPPONENT, new Random(shift ? 2L : 30L));
        }
    }

    private void previewEgg() {
        boolean alt = Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT)
            || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT);

        if (!alt || !Gdx.input.isKeyJustPressed(Input.Keys.O)
            || !canAct() || humanDrewThisTurn || closedRoundPreview
            || stage.getActors().peek() != hudLayer) {
            return;
        }

        clearEggReveal();
        roundSnack = Snack.OVO;
        snackUsed = false;
        choosingCoxinha = false;
        torresmoArmed = false;
        showRoundSnack();
        refreshTurn();
    }



    private void selectStone(Stone stone) {
        DominoTile previous = handActors.get(selectedStone);
        if (previous != null) {
            previous.setSelected(false);
        }
        selectedStone = stone;
        DominoTile selected = handActors.get(stone);
        if (selected != null) {
            selected.setSelected(true);
        }

        leftEndPreview.setVisible(false);
        rightEndPreview.setVisible(false);
        if (stone != null && canAct()) {
            refreshPreviews(stone);
        }
    }

    private void showRoundSnack() {
        playerInfoView.setBackgroundVisible(roundSnack == null);
        snackActionView.showSnack(roundSnack);
    }

    private void refreshPreviews(Stone stone) {
        Placement left = round.preview(stone, Side.LEFT);
        Placement right = chainMoves.isEmpty() && left != null ? null : round.preview(stone, Side.RIGHT);
        showPreview(leftEndPreview, left, previewPose(left, Side.LEFT, chainSizePercent));
        showPreview(rightEndPreview, right, previewPose(right, Side.RIGHT, chainSizePercent));
    }

    private boolean prepareHumanTurn() {
        int percent = chainSizePercent;
        while (!allPreviewsFit(percent)) {
            if (percent == MIN_CHAIN_PERCENT) {
                throw new IllegalStateException("As prévias não couberam na área de jogo.");
            }
            percent -= CHAIN_SCALE_STEP;
        }

        if (percent == chainSizePercent) {
            return false;
        }
        Map<Placement, Pose> poses = layoutChain(chainMoves, percent);
        chainSizePercent = percent;
        movingStone = true;
        cavaView.setTouchable(Touchable.disabled);
        passButton.setAvailable(false);
        applyChainLayout(poses, null);
        tilesLayer.addAction(Actions.sequence(
            Actions.delay(CHAIN_MOVE_DURATION),
            Actions.run(() -> {
                movingStone = false;
                refreshTurn();
            })
        ));
        return true;
    }

    private boolean allPreviewsFit(int percent) {
        for (Stone stone : round.hand(Player.HUMAN)) {
            Placement left = round.preview(stone, Side.LEFT);
            Placement right = chainMoves.isEmpty() && left != null ? null : round.preview(stone, Side.RIGHT);
            Pose leftPose = previewPose(left, Side.LEFT, percent);
            Pose rightPose = previewPose(right, Side.RIGHT, percent);
            if ((left != null && leftPose == null) || (right != null && rightPose == null)) {
                return false;
            }
            if (leftPose != null && rightPose != null && chainBody(leftPose).overlaps(chainBody(rightPose))) {
                return false;
            }
        }
        return true;
    }

    private Pose previewPose(Placement preview, Side side, int percent) {
        if (preview == null) {
            return null;
        }
        List<ChainMove> proposed = new ArrayList<>(chainMoves);
        proposed.add(new ChainMove(preview, side));
        Map<Placement, Pose> poses = layoutChain(proposed, percent);
        return poses == null ? null : poses.get(preview);
    }

    private void showPreview(DominoTile tile, Placement placement, Pose pose) {
        if (pose == null) {
            return;
        }
        tile.setValues(placement.stone.first, placement.stone.second);
        pose.applyTo(tile);
        tile.toFront();
        tile.setVisible(true);
    }

    private void playStone(Stone stone, Side side) {
        Player mover = round.turn();
        Placement placement = round.play(stone, side);
        if (placement == null) {
            return;
        }

        movingStone = true;
        selectStone(null);
        chainMoves.add(new ChainMove(placement, side));

        DominoTile tile = handActors.remove(stone);
        chainActors.put(placement, tile);
        tile.setValues(stone.first, stone.second);
        tile.setFaceDown(false);
        tile.setTouchable(Touchable.disabled);
        tile.toFront();

        cavaView.setTouchable(Touchable.disabled);
        passButton.setAvailable(false);
        layoutHand(mover);

        Map<Placement, Pose> poses = layoutChain(chainMoves, chainSizePercent);
        while (poses == null && chainSizePercent > MIN_CHAIN_PERCENT) {
            chainSizePercent -= CHAIN_SCALE_STEP;
            poses = layoutChain(chainMoves, chainSizePercent);
        }
        if (poses == null) {
            throw new IllegalStateException("A corrente não coube na área de jogo.");
        }
        applyChainLayout(poses, placement);
        Pose target = poses.get(placement);
        float duration = CHAIN_MOVE_DURATION;
        tile.addAction(Actions.sequence(
            Actions.parallel(
                Actions.moveTo(target.x, target.y, duration, Interpolation.smooth),
                Actions.sizeTo(target.width, target.height, duration, Interpolation.smooth),
                Actions.rotateTo(target.rotation, duration, Interpolation.smooth),
                Actions.sequence(
                    Actions.delay(Math.max(0f, duration - 0.05f)),
                    Actions.run(() -> {
                        if (gameSettings.isSoundEffectsEnabled()) {
                            dealSound.play(0.65f);
                        }
                    })
                )
            ),
            Actions.run(() -> {
                movingStone = false;
                refreshTurn();
            })
        ));
    }

    private void registerPassTap() {
        if (!passButton.isVisible() || !canAct() || !round.canPass() || passButton.taps == 2) {
            return;
        }

        passButton.showTaps(passButton.taps + 1);
        playPassTap();

        if (passButton.taps == 2) {
            passButton.setTouchable(Touchable.disabled);
            passButton.addAction(Actions.sequence(
                Actions.delay(0.22f),
                Actions.run(this::finishPass)
            ));
        }
    }

    private void finishPass() {
        boolean passed = canAct() && round.pass();
        passButton.setAvailable(false);

        if (passed) {
            selectStone(null);
        }

        refreshTurn();
    }

    private void drawForCurrentPlayer() {
        Player player = round.turn();
        boolean useTorresmo = player == Player.HUMAN
            && torresmoArmed && canUseTorresmo();

        Stone stone = useTorresmo ? round.drawWithTorresmo() : round.draw();
        if (stone == null) {
            return;
        }

        if (gameSettings.isSoundEffectsEnabled()) {
            drawSound.play(0.65f);
        }

        if (player == Player.HUMAN) {
            humanDrewThisTurn = true;
        }

        if (useTorresmo) {
            torresmoArmed = false;
            snackUsed = true;
            snackActionView.showUsedSnack(Snack.TORRESMO);
        }

        selectStone(null);
        createHandActor(stone, player);
        handActors.get(stone).setBounds(CAVA_X + 70f * CAVA_SCALE, CAVA_Y + 30f * CAVA_SCALE, 44f, 82f);
        layoutHand(player);
        refreshTurn();

        if (useTorresmo && !round.isFinished()) {
            playerInfoView.setTurnPrompt(
                round.turn() == player ? "Escolha sua jogada" : "Sem encaixe"
            );
        }
    }

    private void playOpponentTurn() {
        if (movingStone || round.isFinished() || round.turn() != Player.OPPONENT) {
            return;
        }

        for (Stone stone : round.hand(Player.OPPONENT)) {
            if (round.preview(stone, Side.LEFT) != null) {
                playStone(stone, Side.LEFT);
                return;
            }
            if (round.preview(stone, Side.RIGHT) != null) {
                playStone(stone, Side.RIGHT);
                return;
            }
        }

        if (round.canDraw()) {
            drawForCurrentPlayer();
        } else if (round.pass()) {
            movingStone = true;
            playPassTap();

            tilesLayer.addAction(Actions.sequence(
                Actions.delay(0.18f),
                Actions.run(this::playPassTap),
                Actions.delay(0.22f),
                Actions.run(() -> {
                    movingStone = false;
                    refreshTurn();
                })
            ));
        }
    }

    private void refreshTurn() {
        if (round.isFinished() && roundScored) {
            return;
        }

        Player currentTurn = round.turn();
        if (currentTurn != snackTurn) {
            choosingCoxinha = false;
            torresmoArmed = false;
            snackTurn = currentTurn;
            humanDrewThisTurn = false;
        }

        playerInfoView.setDisplayedCount(round.hand(Player.HUMAN).size());
        opponentInfoView.setDisplayedCount(round.hand(Player.OPPONENT).size());
        cavaView.setDisplayedCount(round.stockCount());
        cavaView.setVisible(round.stockCount() > 0);

        boolean human = canAct();
        if (human && prepareHumanTurn()) {
            return;
        }

        boolean opponent = entranceFinished && !movingStone && !round.isFinished()
            && round.turn() == Player.OPPONENT;
        playerInfoView.setActiveTurn(human);
        opponentInfoView.setActiveTurn(opponent);
        cavaView.setTouchable(human && !choosingCoxinha && round.canDraw() ? Touchable.enabled : Touchable.disabled);
        passButton.setAvailable(human && round.canPass());

        if (round.isFinished()) {
            showResult();
        } else if (opponent) {
            opponentInfoView.setTurnPrompt("Jogando...");
            tilesLayer.addAction(Actions.sequence(
                Actions.delay(0.45f),
                Actions.run(this::playOpponentTurn)
            ));
        } else if (human && choosingCoxinha) {
            playerInfoView.setTurnPrompt("Escolha a peça");
        } else if (human && torresmoArmed) {
            playerInfoView.setTurnPrompt("Cave uma pedra");
        } else if (human && round.canDraw()) {
            playerInfoView.setTurnPrompt("Toque na cava");
        } else if (human && round.canPass()) {
            playerInfoView.setTurnPrompt("Cava vazia");
        }
    }

    private void showResult() {
        clearEggReveal();
        snackActionView.setVisible(false);
        Player winner = round.winner();
        if (winner == Player.HUMAN) {
            humanCaps++;
        } else if (winner == Player.OPPONENT) {
            opponentCaps++;
        }

        boolean matchFinished = matchIsFinished();
        boolean showWinningBoard = winner != null && !round.isBlocked() && !matchFinished;
        boolean showClosedBoard = round.isBlocked() && !matchFinished;

        if (!showWinningBoard) {
            playerScoreView.setDisplayedScore(humanCaps);
            opponentScoreView.setDisplayedScore(opponentCaps);
        }

        roundScored = true;

        for (DominoTile tile : handActors.values()) {
            tile.setVisible(false);
        }
        if (!showWinningBoard && !showClosedBoard) {
            for (DominoTile tile : chainActors.values()) {
                tile.setVisible(false);
            }
        }

        cavaView.setVisible(false);
        playerInfoView.setVisible(false);
        opponentInfoView.setVisible(false);

        if (matchFinished) {
            roundResultView.showMatchWinner(winner == Player.HUMAN, humanCaps, opponentCaps);
            return;
        }

        if (round.isBlocked()) {
            showRoundCounts(winner == null, winner == Player.HUMAN, round.handPoints(Player.HUMAN), round.handPoints(Player.OPPONENT));
        } else if (showWinningBoard) {
            boolean humanWon = winner == Player.HUMAN;
            roundResultView.showWinningPlay(humanWon);
            SequenceAction result = Actions.sequence(
                Actions.delay(1.5f),
                Actions.run(() -> {
                    playerScoreView.setDisplayedScore(humanCaps);
                    opponentScoreView.setDisplayedScore(opponentCaps);
                    roundResultView.showWinningCap(humanWon);

                    if (humanWon) {
                        playRoundVictorySound();
                    }
                }),
                Actions.delay(2f)
            );

            if (humanWon) {
                result.addAction(Actions.run(() -> {
                    humanCoins += 2;
                    showCoinReward();
                }));
                result.addAction(Actions.delay(2f));
            }

            finishRoundAfter(result);
            return;
        } else {
            roundResultView.showRoundWinner(winner == Player.HUMAN);

            if (winner == Player.HUMAN) {
                playRoundVictorySound();
            }
        }

        float delay = round.isBlocked() ? 4.5f : 3.5f;
        finishRoundAfter(Actions.sequence(Actions.delay(delay)));
    }

    private boolean matchIsFinished() {
        return humanCaps >= CAPS_TO_WIN || opponentCaps >= CAPS_TO_WIN;
    }

    private static final float PLAY_LEFT = 526f;
    private static final float PLAY_RIGHT = 1420f;
    private static final float PLAY_BOTTOM = 214f;
    private static final float PLAY_TOP = 916f;
    // Centro do furo desenhado na textura da mesa.
    private static final float OPENING_X = 974f;
    private static final float OPENING_Y = 542f;

    private static final float HUMAN_HAND_Y = 112f;
    private static final float OPPONENT_HAND_Y = 1000f;
    private static final float CAVA_X = 1314f;
    private static final float CAVA_Y = 864f;
    private static final float CAVA_SCALE = 0.72f;
    private static final Rectangle CAVA_AREA = new Rectangle(CAVA_X - 5f, CAVA_Y - 5f, 133f, 97f);
    private static final Rectangle OPPONENT_CAPS_AREA = new Rectangle(470f, 880f, 160f, 164f);

    private static final float BODY_WIDTH = STONE_WIDTH * 152f / 176f;
    private static final float BODY_LENGTH = STONE_HEIGHT * 297f / 328f;
    private static final float BODY_SHIFT = STONE_HEIGHT * 7.5f / 328f;
    private static final float PIP_OFFSET = STONE_HEIGHT * 30f / 164f;

    private static final int MIN_CHAIN_PERCENT = 10;
    private static final int CHAIN_SCALE_STEP = 5;
    private static final float CHAIN_MOVE_DURATION = 0.42f;

    private static Map<Placement, Pose> layoutChain(List<ChainMove> moves, int percent) {
        Map<Placement, Pose> poses = new IdentityHashMap<>();
        if (moves.isEmpty()) {
            return poses;
        }
        float scale = percent / 100f;
        Placement opening = moves.get(0).placement;
        float angle = opening.stone.isDouble() ? 0f : opening.stone.first == opening.left ? 90f : -90f;
        Pose center = chainPose(OPENING_X, OPENING_Y, angle, scale);
        poses.put(opening, center);

        ChainEnd left = new ChainEnd(opening, center, Direction.LEFT);
        ChainEnd right = new ChainEnd(opening, center, Direction.RIGHT);
        List<Rectangle> occupied = new ArrayList<>();
        occupied.add(CAVA_AREA);
        occupied.add(OPPONENT_CAPS_AREA);
        occupied.add(chainBody(center));

        for (int i = 1; i < moves.size(); i++) {
            ChainMove move = moves.get(i);
            ChainEnd end = move.side == Side.LEFT ? left : right;
            if (!extendChain(end, move, scale, occupied)) {
                return null;
            }
            poses.put(move.placement, end.pose);
            occupied.add(chainBody(end.pose));
        }
        return poses;
    }

    private static boolean extendChain(ChainEnd end, ChainMove move, float scale, List<Rectangle> occupied) {
        boolean bucha = move.placement.stone.isDouble();
        int incoming = move.side == Side.LEFT ? move.placement.right : move.placement.left;
        boolean firstIsIncoming = move.placement.stone.first == incoming;
        Rectangle previousBody = chainBody(end.pose);
        Direction direction = end.direction;
        float x = previousBody.x + previousBody.width / 2f;
        float y = previousBody.y + previousBody.height / 2f;
        float gap = 1.5f * scale;
        float halfAlong = (bucha ? BODY_WIDTH : BODY_LENGTH) * scale / 2f;
        float halfAcross = (bucha ? BODY_LENGTH : BODY_WIDTH) * scale / 2f;

        float straight = halfExtent(previousBody, direction) + gap + halfAlong;
        float angle = stoneAngle(direction, bucha, firstIsIncoming);
        float straightX = x + direction.x * straight;
        float straightY = y + direction.y * straight;
        Pose candidate = chainPose(straightX, straightY, angle, scale);
        float nextTurnOffset = bucha ? 0f
            : (PIP_OFFSET + (firstIsIncoming ? BODY_SHIFT : -BODY_SHIFT)) * scale;
        Direction preferred = preferredTurn(direction, move.side, x, y);
        float doubleHalfWidth = BODY_LENGTH * scale / 2f;
        boolean reserveCorner = direction.x != 0
            && Math.abs(straightY - OPENING_Y) <= BODY_WIDTH * scale;
        if (fitsOnTable(candidate, occupied)
            && (!reserveCorner || fitsDoubleAcross(straightX + direction.x * nextTurnOffset,
            straightY, preferred, doubleHalfWidth))) {
            end.place(move.placement, candidate, direction);
            return true;
        }

        boolean firstIsOutgoing = end.placement.stone.first
            == (move.side == Side.LEFT ? end.placement.left : end.placement.right);
        float outgoingOffset = end.placement.stone.isDouble() ? 0f
            : (PIP_OFFSET - (firstIsOutgoing ? BODY_SHIFT : -BODY_SHIFT)) * scale;
        for (Direction turn : new Direction[]{preferred, preferred.opposite()}) {
            angle = stoneAngle(turn, bucha, firstIsIncoming);

            // Na curva externa, a metade de entrada da peça nova toca a ponta antiga.
            float outward = halfExtent(previousBody, direction) + gap + halfAcross;
            float incomingOffset = bucha ? 0f : (PIP_OFFSET - (firstIsIncoming ? BODY_SHIFT : -BODY_SHIFT)) * scale;
            float turnX = x + direction.x * outward + turn.x * incomingOffset;
            float turnY = y + direction.y * outward + turn.y * incomingOffset;
            candidate = chainPose(turnX, turnY, angle, scale);
            if (!end.placement.stone.isDouble()
                && fitsDoubleAcross(turnX, turnY, turn, doubleHalfWidth)
                && fitsOnTable(candidate, occupied)) {
                end.place(move.placement, candidate, turn);
                return true;
            }

            // Junto ao friso, a próxima peça toca a lateral da metade que está livre.
            float inward = halfExtent(previousBody, turn) + gap + halfAlong;
            turnX = x + direction.x * outgoingOffset + turn.x * inward;
            turnY = y + direction.y * outgoingOffset + turn.y * inward;
            candidate = chainPose(turnX, turnY, angle, scale);
            if (fitsDoubleAcross(turnX, turnY, turn, doubleHalfWidth) && fitsOnTable(candidate, occupied)) {
                end.place(move.placement, candidate, turn);
                return true;
            }
        }
        return false;
    }

    private static boolean fitsDoubleAcross(float x, float y, Direction direction, float halfWidth) {
        // Reserva a largura da bucha transversal ao próximo trecho.
        return direction.x == 0
            ? x - halfWidth >= PLAY_LEFT && x + halfWidth <= PLAY_RIGHT
            : y - halfWidth >= PLAY_BOTTOM && y + halfWidth <= PLAY_TOP;
    }

    private static Direction preferredTurn(Direction direction, Side side, float x, float y) {
        if (direction.x == 0) {
            return x < OPENING_X ? Direction.RIGHT : Direction.LEFT;
        }
        if (Math.abs(y - OPENING_Y) <= BODY_WIDTH) {
            return side == Side.LEFT ? Direction.UP : Direction.DOWN;
        }
        return y < OPENING_Y ? Direction.UP : Direction.DOWN;
    }

    private static float halfExtent(Rectangle body, Direction direction) {
        return (direction.x == 0 ? body.height : body.width) / 2f;
    }

    private static float stoneAngle(Direction direction, boolean bucha, boolean firstIsIncoming) {
        if (direction.x != 0) {
            return bucha ? 0f : firstIsIncoming == (direction == Direction.RIGHT) ? 90f : -90f;
        }
        return bucha ? 90f : firstIsIncoming == (direction == Direction.DOWN) ? 0f : 180f;
    }

    private static boolean fitsOnTable(Pose pose, List<Rectangle> occupied) {
        Rectangle body = chainBody(pose);
        if (body.x < PLAY_LEFT || body.x + body.width > PLAY_RIGHT
            || body.y < PLAY_BOTTOM || body.y + body.height > PLAY_TOP) {
            return false;
        }
        for (Rectangle placed : occupied) {
            if (body.overlaps(placed)) {
                return false;
            }
        }
        return true;
    }

    private enum Direction {
        LEFT(-1, 0), RIGHT(1, 0), UP(0, 1), DOWN(0, -1);

        final int x;
        final int y;

        Direction(int x, int y) {
            this.x = x;
            this.y = y;
        }

        Direction opposite() {
            switch (this) {
                case LEFT: return RIGHT;
                case RIGHT: return LEFT;
                case UP: return DOWN;
                default: return UP;
            }
        }
    }

    private static final class ChainMove {
        final Placement placement;
        final Side side;

        ChainMove(Placement placement, Side side) {
            this.placement = placement;
            this.side = side;
        }
    }

    private static final class ChainEnd {
        Placement placement;
        Pose pose;
        Direction direction;

        ChainEnd(Placement placement, Pose pose, Direction direction) {
            place(placement, pose, direction);
        }

        void place(Placement placement, Pose pose, Direction direction) {
            this.placement = placement;
            this.pose = pose;
            this.direction = direction;
        }
    }

    private static Pose chainPose(float x, float y, float angle, float scale) {
        float shift = BODY_SHIFT * scale;
        float artX = angle == 90f ? -shift : angle == -90f ? shift : 0f;
        float artY = angle == 0f ? shift : angle == 180f ? -shift : 0f;
        float width = STONE_WIDTH * scale;
        float height = STONE_HEIGHT * scale;
        return new Pose(x - width / 2f - artX, y - height / 2f - artY, width, height, angle);
    }

    private static Rectangle chainBody(Pose pose) {
        float scale = pose.width / STONE_WIDTH;
        float shift = BODY_SHIFT * scale;
        boolean horizontal = Math.abs(pose.rotation) == 90f;
        float width = (horizontal ? BODY_LENGTH : BODY_WIDTH) * scale;
        float height = (horizontal ? BODY_WIDTH : BODY_LENGTH) * scale;
        float x = pose.x + pose.width / 2f;
        float y = pose.y + pose.height / 2f;
        x -= horizontal ? Math.copySign(shift, pose.rotation) : 0f;
        y += horizontal ? 0f : pose.rotation == 0f ? shift : -shift;
        return new Rectangle(x - width / 2f, y - height / 2f, width, height);
    }

    private void applyChainLayout(Map<Placement, Pose> poses, Placement moving) {
        for (Map.Entry<Placement, DominoTile> entry : chainActors.entrySet()) {
            if (entry.getKey() != moving) {
                Pose pose = poses.get(entry.getKey());
                DominoTile tile = entry.getValue();
                float rotation = tile.getRotation();
                float delta = (pose.rotation - rotation + 540f) % 360f - 180f;
                if (Math.abs(tile.getX() - pose.x) < 0.1f
                    && Math.abs(tile.getY() - pose.y) < 0.1f
                    && Math.abs(tile.getWidth() - pose.width) < 0.1f && Math.abs(delta) < 0.1f) {
                    continue;
                }
                tile.clearActions();
                tile.addAction(Actions.parallel(
                    Actions.moveTo(pose.x, pose.y, CHAIN_MOVE_DURATION, Interpolation.smooth),
                    Actions.sizeTo(pose.width, pose.height, CHAIN_MOVE_DURATION, Interpolation.smooth),
                    Actions.rotateTo(rotation + delta, CHAIN_MOVE_DURATION, Interpolation.smooth)
                ));
            }
        }
    }

    private static final class Pose {
        final float x;
        final float y;
        final float width;
        final float height;
        final float rotation;

        Pose(float x, float y, float width, float height, float rotation) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.rotation = rotation;
        }

        void applyTo(DominoTile tile) {
            tile.setBounds(x, y, width, height);
            tile.setRotation(rotation);
        }
    }

    private static final class PassButton extends Group implements Disposable {
        private static final float SIZE = 152f;
        private static final int FRAME_SIZE = 256;

        private final Texture texture;
        private final TextureRegionDrawable[] faces = new TextureRegionDrawable[3];
        private final Image face;
        private final Label count;

        private int taps;

        PassButton(BitmapFont font) {
            setSize(SIZE, SIZE);
            texture = createTexture();

            for (int i = 0; i < faces.length; i++) {
                TextureRegion region = new TextureRegion(texture, i * FRAME_SIZE, 0, FRAME_SIZE, FRAME_SIZE);
                faces[i] = new TextureRegionDrawable(region);
            }

            face = new Image();
            face.setBounds(0f, 0f, SIZE, SIZE);
            face.setTouchable(Touchable.disabled);
            addActor(face);

            Label.LabelStyle style = new Label.LabelStyle(font, Color.valueOf("234730"));
            Label title = new Label("BATER", style);
            title.setAlignment(Align.center);
            title.setBounds(0f, SIZE / 2f - 2f, SIZE, 28f);
            title.setTouchable(Touchable.disabled);
            addActor(title);

            count = new Label("", style);
            count.setAlignment(Align.center);
            count.setFontScale(0.8f);
            count.setBounds(0f, SIZE / 2f - 30f, SIZE, 24f);
            count.setTouchable(Touchable.disabled);
            addActor(count);

            showTaps(0);
        }

        void setAvailable(boolean available) {
            setVisible(available);

            if (!available) {
                clearActions();
                showTaps(0);
            }

            setTouchable(available && taps < 2 ? Touchable.enabled : Touchable.disabled);
        }

        void showTaps(int taps) {
            this.taps = taps;
            face.setDrawable(faces[taps]);
            count.setText(taps + "/2");
        }

        @Override
        public Actor hit(float x, float y, boolean touchable) {
            float dx = x - SIZE / 2f;
            float dy = y - SIZE / 2f;
            float radius = 124f * SIZE / FRAME_SIZE;

            if (dx * dx + dy * dy > radius * radius) {
                return null;
            }

            return super.hit(x, y, touchable);
        }

        private static Texture createTexture() {
            Pixmap pixmap = new Pixmap(FRAME_SIZE * 3, FRAME_SIZE, Pixmap.Format.RGBA8888);
            try {
                pixmap.setBlending(Pixmap.Blending.None);

                for (int taps = 0; taps <= 2; taps++) {
                    drawFace(pixmap, taps);
                }

                Texture texture = new Texture(pixmap);
                texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                return texture;
            } finally {
                pixmap.dispose();
            }
        }

        private static void drawFace(Pixmap pixmap, int taps) {
            int centerX = taps * FRAME_SIZE + FRAME_SIZE / 2;
            int centerY = FRAME_SIZE / 2;
            Color cream = Color.valueOf("F0DCB2");
            Color gold = Color.valueOf("F5C54C");

            pixmap.setColor(Color.valueOf("3F2412"));
            pixmap.fillCircle(centerX, centerY, 124);
            pixmap.setColor(cream);
            pixmap.fillCircle(centerX, centerY, 115);
            pixmap.setColor(taps == 2 ? gold : Color.valueOf("80684B"));
            pixmap.fillCircle(centerX, centerY, 106);

            if (taps == 1) {
                pixmap.setColor(gold);

                for (int x = 0; x <= 106; x++) {
                    int extent = (int) Math.sqrt(106 * 106 - x * x);
                    pixmap.drawLine(centerX + x, centerY - extent,
                        centerX + x, centerY + extent);
                }
            }

            pixmap.setColor(cream);
            pixmap.fillCircle(centerX, centerY, 94);
        }

        @Override
        public void dispose() {
            texture.dispose();
        }
    }

    private BitmapFont createControlsFont() {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
            Gdx.files.internal("fonts/NunitoSans-Regular.ttf")
        );
        try {
            FreeTypeFontGenerator.FreeTypeFontParameter config =
                new FreeTypeFontGenerator.FreeTypeFontParameter();
            config.size = 28;
            config.characters = FreeTypeFontGenerator.DEFAULT_CHARS
                + "áàâãéêíóôõúçÁÀÂÃÉÊÍÓÔÕÚÇ";
            config.minFilter = Texture.TextureFilter.Linear;
            config.magFilter = Texture.TextureFilter.Linear;
            return generator.generateFont(config);
        } finally {
            generator.dispose();
        }
    }

    @Override
    public void show() {
        if (entranceTime < ENTRANCE_DURATION && gameSettings.isSoundEffectsEnabled()) {
            barAmbience.setVolume(0f);
            barAmbience.play();
        }
        updateEntrance();
        passButton.setAvailable(canAct() && round.canPass());

        Gdx.input.setInputProcessor(stage);
    }

    private void simulateReconnection() {
        if (!entranceFinished || round.isFinished() || matchSettings.isExitConfirmationOpen()) {
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F6)) {
            matchSettings.openReconnection(stage, false);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.F7)) {
            matchSettings.openReconnection(stage, true);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.F8)) {
            matchSettings.closeReconnection();
        }
    }

    private MatchEndReason readMatchEndPreview() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.F4)) {
            return MatchEndReason.OPPONENT_LEFT;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
            return MatchEndReason.OPPONENT_RECONNECT_EXPIRED;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
            return MatchEndReason.OWN_RECONNECT_EXPIRED;
        }
        return null;
    }

    private void toggleClosedRoundPreview() {
        MatchEndReason endReason = readMatchEndPreview();
        boolean finalPreview = Gdx.input.isKeyJustPressed(Input.Keys.F11);
        boolean invitationPreview = Gdx.input.isKeyJustPressed(Input.Keys.F12);

        if (endReason == null && !finalPreview && !invitationPreview
            && !Gdx.input.isKeyJustPressed(Input.Keys.F9)) {
            return;
        }

        if (closedRoundPreview) {
            closeClosedRoundPreview();
            Gdx.input.setInputProcessor(stage);
            return;
        }

        if (!entranceFinished || movingStone || round.isFinished()
            || roundResultView.isVisible() || stage.getActors().peek() != hudLayer) {
            return;
        }

        boolean opponentWins = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
            || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);

        closedRoundPreview = true;
        stage.cancelTouchFocus();
        if (endReason != null) {
            roundResultView.showInterruptedMatch(endReason, 3, 2);
            Gdx.input.setInputProcessor(stage);
            return;
        }
        if (finalPreview || invitationPreview) {
            refuseRematchInPreview = finalPreview && !invitationPreview && (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT));
            roundResultView.showMatchWinner(!opponentWins, opponentWins ? 3 : 5, opponentWins ? 5 : 3);

            if (invitationPreview) {
                roundResultView.showRematchInvitation(this::startAcceptedRematch);
            }

            Gdx.input.setInputProcessor(stage);
            return;
        }

        boolean coinPreview = Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT)
            || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT);

        if (coinPreview) {
            roundResultView.showWinningPlay(true);

            roundResultView.addAction(Actions.sequence(
                Actions.delay(0.30f),
                Actions.run(this::showCoinReward)
            ));

            Gdx.input.setInputProcessor(stage);
            return;
        }

        Gdx.input.setInputProcessor(null);
        boolean tiePreview = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)
            || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT);

        if (tiePreview) {
            showRoundCounts(true, false, 15, 15);
        } else {
            showRoundCounts(false, !opponentWins, opponentWins ? 34 : 12, opponentWins ? 12 : 34);
        }
    }

    private boolean refuseRematchInPreview;

    private void closeClosedRoundPreview() {
        closedRoundPreview = false;
        refuseRematchInPreview = false;
        roundResultView.clearActions();
        roundResultView.setVisible(false);
        coinRewardSound.stop();
    }

    private boolean updateInterval(float delta) {
        if (intervalFade.getParent() != null) {
            intervalFade.act(delta);
            return true;
        }

        if (intervalView.isVisible()) {
            boolean closePreview = afterInterval == null && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE);

            if (Gdx.input.isKeyJustPressed(Input.Keys.F6)) {
                stage.cancelTouchFocus();
                intervalView.setReconnecting(true);
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.F8)) {
                intervalView.setReconnecting(false);
            }

            intervalView.act(delta);

            if (closePreview || (intervalView.isTimeUp() && !intervalView.isReconnecting())) {
                fadeInterval(() -> {
                    intervalView.setVisible(false);

                    Runnable resume = afterInterval;
                    afterInterval = null;
                    if (resume != null) {
                        resume.run();
                    }
                });
            }
            return true;
        }

        if (!Gdx.input.isKeyJustPressed(Input.Keys.F5)
            || !entranceFinished || movingStone || round.isFinished()
            || closedRoundPreview || roundResultView.isVisible()
            || stage.getActors().peek() != hudLayer) {
            return false;
        }

        boolean fullBalance = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
            || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
        boolean alreadyBought = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)
            || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT);

        boolean refuseOrder = fullBalance && alreadyBought;
        Set<IntervalView.Snack> previewHistory = EnumSet.noneOf(IntervalView.Snack.class);

        if (alreadyBought && !refuseOrder) {
            previewHistory.add(IntervalView.Snack.OVO);
        }

        int previewBalance = !refuseOrder && (fullBalance || alreadyBought) ? 4 : 2;
        openInterval(previewBalance, previewHistory, false, refuseOrder, null);
        return true;
    }

    @Override
    public void render(float delta) {
        float dt = Math.min(delta, 1f / 30f);

        float previousEntranceTime = entranceTime;
        entranceTime = Math.min(entranceTime + dt, PREPARATION_END);
        if (previousEntranceTime < ENTRANCE_DURATION && entranceTime >= ENTRANCE_DURATION) {
            playShuffleSound();
        }

        if (previousEntranceTime < BAR_AMBIENCE_END && entranceTime >= BAR_AMBIENCE_END) {
            barAmbience.stop();
        }

        updateEntrance();
        playDealSounds(previousEntranceTime);

        if (!entranceFinished && entranceTime >= PREPARATION_END) {
            entranceFinished = true;

            if (redistributingTie) {
                redistributingTie = false;
                for (DominoTile tile : handActors.values()) {
                    tile.setVisible(true);
                }
            }

            refreshTurn();
        }

        ScreenUtils.clear(0f, 0f, 0f, 1f);

        stage.getViewport().apply();
        if (!updateInterval(delta)) {
            toggleClosedRoundPreview();
            previewEgg();
            previewOtherSnacks();

            if (closedRoundPreview) {
                roundResultView.act(dt);
            } else {
                if (Gdx.input.isKeyJustPressed(Input.Keys.F10)
                    && entranceFinished && !movingStone && !round.isFinished()
                    && !roundResultView.isVisible() && stage.getActors().peek() == hudLayer) {
                    startTieRedistribution(roundOpeningPreference);
                }

                simulateReconnection();

                if (!matchSettings.isExitConfirmationOpen() && !matchSettings.isReconnectionOpen()) {
                    stage.act(dt);
                }
            }
        }

        boolean readyToPlay = entranceTime >= PREPARATION_END;
        boolean showRedistribution = redistributingTie && !readyToPlay;
        boolean showFinalReceipt = roundResultView.isFinalReceiptVisible();

        tilesLayer.setVisible(!showFinalReceipt && (readyToPlay || showRedistribution));
        hudLayer.setVisible(readyToPlay);
        gameHud.setVisible(!showFinalReceipt);
        redistributionMessage.setVisible(showRedistribution);

        updateEggReveal(delta);
        snackActionView.setButtonEnabled(canUseEgg() || canUseCoxinha() || canUseTorresmo());
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void hide() {
        barAmbience.stop();
        shuffleSound.stop();
        dealSound.stop();
        drawSound.stop();
        passTapSound.stop();
        roundVictorySound.stop();
        coinRewardSound.stop();
        snackPurchaseSound.stop();

        clearEggReveal();
        if (closedRoundPreview) {
            closeClosedRoundPreview();
        }

        if (Gdx.input.getInputProcessor() == stage) {
            Gdx.input.setInputProcessor(null);
        }
        passButton.setAvailable(false);
        intervalView.setVisible(false);
        afterInterval = null;
        intervalFade.clearActions();
        intervalFade.remove();
        matchSettings.close();
    }

    @Override
    public void dispose() {
        hide();

        snackPurchaseSound.dispose();
        coinRewardSound.dispose();
        roundVictorySound.dispose();
        passTapSound.dispose();
        drawSound.dispose();
        dealSound.dispose();
        shuffleSound.dispose();
        barAmbience.dispose();
        stage.dispose();
        roundResultView.dispose();
        intervalView.dispose();
        snackActionView.dispose();
        controlsFont.dispose();
        passButton.dispose();
        playerInfoView.dispose();
        opponentInfoView.dispose();
        playerScoreView.dispose();
        opponentScoreView.dispose();
        backgroundTexture.dispose();
        seatedTexture.dispose();
        floorTexture.dispose();
        tableTexture.dispose();
        dominoAssets.dispose();
        cavaView.dispose();
        matchSettings.dispose();
        matchSettingsIcon.dispose();
        dealCavaView.dispose();
    }
}
