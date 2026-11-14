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
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
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

import br.com.dominando.frontend.settings.GameSettings;
import br.com.dominando.frontend.ui.OpeningDialogs;


import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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

public class MatchScreen extends ScreenAdapter {
    private static final float WIDTH = 1920f;
    private static final float HEIGHT = 1080f;
    private static final float ENTRANCE_DURATION = 4.4f;
    private static final float SHUFFLE_DURATION = 2.8f;
    private static final float SHUFFLE_END = ENTRANCE_DURATION + SHUFFLE_DURATION;
    private static final float DEAL_SLIDE = 0.45f;
    private static final float DEAL_INTERVAL = 0.06f;
    private static final float DEAL_DURATION = 6 * DEAL_INTERVAL + DEAL_SLIDE;
    private static final float PREPARATION_END = SHUFFLE_END + DEAL_DURATION + 0.35f;
    private static final float STONE_WIDTH = 60f;
    private static final float STONE_HEIGHT = 111.82f;
    private static final float CHAIN_LEFT = 490f;
    private static final float CHAIN_RIGHT = 1495f;
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
    private final Group shuffleLayer = new Group();

    private final CavaView cavaView;

    private final ScoreView playerScoreView;
    private final ScoreView opponentScoreView;

    private final PlayerInfoView playerInfoView;
    private final PlayerInfoView opponentInfoView;

    private final DominoTile leftEndPreview;
    private final DominoTile rightEndPreview;
    private final Label hint;
    private final RoundResultView roundResultView;
    private final TextButton passButton;
    private final TextButton nextRoundButton;
    private final BitmapFont controlsFont;
    private final Texture buttonTexture;

    private final Random random = new Random();
    private final Map<Stone, DominoTile> handActors = new IdentityHashMap<>();
    private final Map<Placement, DominoTile> chainActors = new IdentityHashMap<>();
    private LocalDominoRound round;
    private Player roundOpeningPreference;
    private Stone selectedStone;
    private Placement firstPlacement;
    private Boolean chainBendsLeft;
    private Placement firstCorner;
    private boolean cavaAside;
    private boolean leavingResult;
    private boolean entranceFinished;
    private boolean movingStone;
    private boolean roundScored;
    private int humanCaps;
    private int opponentCaps;

    private final OpeningDialogs matchSettings;
    private final Texture matchSettingsIcon;
    private final CavaView dealCavaView;

    public MatchScreen() {
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
        float cavaScale = 1f;

        cavaView.setOrigin(0f, 0f);
        cavaView.setScale(cavaScale);
        cavaView.setPosition(490f, HEIGHT - 400f - 120f * cavaScale);

        tilesLayer.addActor(cavaView);

        dealCavaView = new CavaView(dominoAssets, 14);
        dealCavaView.setPosition(cavaView.getX(), cavaView.getY());
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

        opponentInfoView = new PlayerInfoView(false, "Adversário", 7);
        opponentInfoView.setPosition(48f, HEIGHT - 48f - PlayerInfoView.TOTAL_HEIGHT);

        hudLayer.addActor(opponentInfoView);

        playerInfoView = new PlayerInfoView(true, "Você", 7);
        playerInfoView.setPosition(1544f, HEIGHT - 920f - PlayerInfoView.TOTAL_HEIGHT);

        hudLayer.addActor(playerInfoView);

        matchSettings = new OpeningDialogs(new GameSettings(), () -> {});
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
                matchSettings.open(stage);
            }
        });
        hudLayer.addActor(settingsButton);

        controlsFont = createControlsFont();
        buttonTexture = createButtonTexture();

        hint = new Label("", new Label.LabelStyle(controlsFont, Color.valueOf("F0DCB2")));
        hint.setAlignment(Align.center);
        hint.setBounds(510f, 337f, 900f, 42f);
        hint.setTouchable(Touchable.disabled);
        hudLayer.addActor(hint);

        passButton = createButton("Bater duas vezes");
        ClickListener passListener = new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (getTapCount() >= 2 && canAct() && round.pass()) {
                    selectStone(null);
                    refreshTurn();
                }
            }
        };
        passListener.setTapCountInterval(0.45f);
        passButton.addListener(passListener);

        roundResultView = new RoundResultView();
        hudLayer.addActor(roundResultView);

        nextRoundButton = createButton("Próxima rodada");
        nextRoundButton.setY(105f);
        nextRoundButton.setOrigin(nextRoundButton.getWidth() / 2f, nextRoundButton.getHeight() / 2f);
        nextRoundButton.setTransform(true);
        nextRoundButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (round.isFinished() && !movingStone && !leavingResult) {
                    leavingResult = true;
                    nextRoundButton.setTouchable(Touchable.disabled);
                    nextRoundButton.clearActions();
                    nextRoundButton.addAction(Actions.parallel(
                        Actions.fadeOut(0.18f, Interpolation.smooth),
                        Actions.scaleTo(0.97f, 0.97f, 0.18f, Interpolation.smooth)
                    ));
                    roundResultView.clearActions();
                    roundResultView.addAction(Actions.sequence(
                        Actions.fadeOut(0.18f, Interpolation.smooth),
                        Actions.run(MatchScreen.this::beginNextRound)
                    ));
                }
            }
        });

        cavaView.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (canAct()) {
                    drawForCurrentPlayer();
                }
            }
        });

        startRound(null);
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
                targetX = dealCavaView.getX() + dealCavaView.getWidth() / 2f;
                targetY = dealCavaView.getY() + dealCavaView.getHeight() / 2f;
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
        tilesLayer.setVisible(entranceTime >= PREPARATION_END);
        hudLayer.setVisible(entranceTime >= PREPARATION_END);
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

        firstPlacement = null;
        chainBendsLeft = null;
        firstCorner = null;
        cavaAside = false;
        leavingResult = false;
        cavaView.clearActions();
        cavaView.setX(490f);
        roundScored = false;
        roundResultView.clearActions();
        roundResultView.setVisible(false);
        playerScoreView.setVisible(true);
        opponentScoreView.setVisible(true);
        playerInfoView.setVisible(true);
        opponentInfoView.setVisible(true);
        roundOpeningPreference = preferredStarter;
        round = new LocalDominoRound(random, preferredStarter);

        for (Player player : Player.values()) {
            for (Stone stone : round.hand(player)) {
                createHandActor(stone, player);
            }
        }

        layoutHand(Player.HUMAN);
        layoutHand(Player.OPPONENT);
        refreshTurn();
    }

    private void beginNextRound() {
        Player nextStarter = round.winner() != null ? round.winner() : roundOpeningPreference;
        if (matchIsFinished()) {
            humanCaps = 0;
            opponentCaps = 0;
            nextStarter = null;
            playerScoreView.setDisplayedScore(0);
            opponentScoreView.setDisplayedScore(0);
        }
        startRound(nextStarter);
    }

    private void createHandActor(Stone stone, Player player) {
        boolean local = player == Player.HUMAN;
        DominoTile tile = local
            ? new DominoTile(dominoAssets, stone.first, stone.second)
            : new DominoTile(dominoAssets, 0, 0);

        if (local) {
            tile.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (canAct()) {
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
        float centerY = local ? 170f : HEIGHT - 132f - tileHeight / 2f;
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
        } else if (round != null && !movingStone) {
            applyChainLayout(planChain(round.chain(), false), null);
        }
    }

    private void refreshPreviews(Stone stone) {
        Placement left = round.preview(stone, Side.LEFT);
        Placement right = round.preview(stone, Side.RIGHT);
        List<Placement> displayed = new ArrayList<>(round.chain());

        if (left != null) {
            displayed.add(0, left);
        }
        if (right != null) {
            displayed.add(right);
        }

        Map<Placement, Pose> poses = planChain(displayed, false);
        applyChainLayout(poses, null);
        showPreview(leftEndPreview, left, poses);
        showPreview(rightEndPreview, right, poses);
    }

    private void showPreview(
        DominoTile actor, Placement placement, Map<Placement, Pose> poses
    ) {
        if (placement == null) {
            return;
        }
        actor.setValues(placement.stone.first, placement.stone.second);
        poses.get(placement).applyTo(actor);
        actor.toFront();
        actor.setVisible(true);
    }

    private void playStone(Stone stone, Side side) {
        Player mover = round.turn();
        Placement placement = round.play(stone, side);
        if (placement == null) {
            return;
        }

        movingStone = true;
        selectStone(null);
        if (firstPlacement == null) {
            firstPlacement = placement;
        }

        DominoTile tile = handActors.remove(stone);
        chainActors.put(placement, tile);
        tile.setValues(stone.first, stone.second);
        tile.setFaceDown(false);
        tile.setTouchable(Touchable.disabled);
        tile.toFront();

        cavaView.setTouchable(Touchable.disabled);
        passButton.setVisible(false);
        hint.setText("");
        layoutHand(mover);

        Map<Placement, Pose> poses = planChain(round.chain(), true);
        applyChainLayout(poses, placement);
        Pose target = poses.get(placement);
        float duration = 0.42f;
        tile.addAction(Actions.sequence(
            Actions.parallel(
                Actions.moveTo(target.x, target.y, duration, Interpolation.smooth),
                Actions.sizeTo(target.width, target.height, duration, Interpolation.smooth),
                Actions.rotateTo(target.rotation, duration, Interpolation.smooth)
            ),
            Actions.run(() -> {
                movingStone = false;
                refreshTurn();
            })
        ));
    }

    private void drawForCurrentPlayer() {
        Player player = round.turn();
        Stone stone = round.draw();
        if (stone != null) {
            selectStone(null);
            createHandActor(stone, player);
            handActors.get(stone).setBounds(cavaView.getX() + 70f, cavaView.getY() + 30f, 44f, 82f);
            layoutHand(player);
            refreshTurn();
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
            refreshTurn();
        }
    }

    private void refreshTurn() {
        playerInfoView.setDisplayedCount(round.hand(Player.HUMAN).size());
        opponentInfoView.setDisplayedCount(round.hand(Player.OPPONENT).size());
        cavaView.setDisplayedCount(round.stockCount());
        cavaView.setVisible(round.stockCount() > 0);

        boolean human = canAct();
        boolean opponent = entranceFinished && !movingStone && !round.isFinished()
            && round.turn() == Player.OPPONENT;
        playerInfoView.setActiveTurn(human);
        opponentInfoView.setActiveTurn(opponent);
        cavaView.setTouchable(human && round.canDraw() ? Touchable.enabled : Touchable.disabled);
        passButton.setVisible(human && round.canPass());
        nextRoundButton.setVisible(round.isFinished());

        hint.setY(337f);
        if (round.isFinished()) {
            showResult();
        } else if (opponent) {
            hint.setText("Adversário jogando...");
            tilesLayer.addAction(Actions.sequence(
                Actions.delay(0.45f),
                Actions.run(this::playOpponentTurn)
            ));
        } else if (human && round.canDraw()) {
            hint.setY(26f);
            hint.setText("Sem encaixe. Toque na cava.");
        } else if (human && round.canPass()) {
            hint.setText("Cava vazia.");
        } else if (human && round.chain().isEmpty()) {
            Stone opening = round.openingStone();
            if (opening == null) {
                hint.setText("Selecione uma pedra para abrir.");
            } else {
                hint.setText("Abra com a bucha de " + opening.first + ".");
            }
        } else {
            hint.setText("");
        }
    }

    private void showResult() {
        Player winner = round.winner();
        if (!roundScored) {
            nextRoundButton.clearActions();
            nextRoundButton.setTouchable(Touchable.disabled);
            nextRoundButton.getColor().a = 0f;
            nextRoundButton.setScale(0.97f);
            nextRoundButton.addAction(Actions.sequence(
                Actions.delay(0.10f),
                Actions.parallel(
                    Actions.fadeIn(0.26f, Interpolation.smooth),
                    Actions.scaleTo(1f, 1f, 0.26f, Interpolation.smooth)
                ),
                Actions.touchable(Touchable.enabled)
            ));
            if (winner == Player.HUMAN) {
                humanCaps++;
            } else if (winner == Player.OPPONENT) {
                opponentCaps++;
            }
            playerScoreView.setDisplayedScore(humanCaps);
            opponentScoreView.setDisplayedScore(opponentCaps);
            roundScored = true;
        }

        hint.setText("");
        for (DominoTile tile : handActors.values()) {
            tile.setVisible(false);
        }
        for (DominoTile tile : chainActors.values()) {
            tile.setVisible(false);
        }
        cavaView.setVisible(false);
        playerInfoView.setVisible(false);
        opponentInfoView.setVisible(false);

        if (matchIsFinished()) {
            roundResultView.showMatchWinner(winner == Player.HUMAN, humanCaps, opponentCaps);
            nextRoundButton.setText("Nova partida");
        } else if (round.isBlocked()) {
            roundResultView.showCounts(
                winner == null, winner == Player.HUMAN,
                round.handPoints(Player.HUMAN), round.handPoints(Player.OPPONENT)
            );
            nextRoundButton.setText(winner == null ? "Refazer rodada" : "Próxima rodada");
        } else {
            roundResultView.showRoundWinner(winner == Player.HUMAN);
            nextRoundButton.setText("Próxima rodada");
        }
    }

    private boolean matchIsFinished() {
        return humanCaps >= CAPS_TO_WIN || opponentCaps >= CAPS_TO_WIN;
    }

    // Dimensões do corpo opaco no PNG pedra-base.png (176 por 328).
    private static final float BODY_WIDTH = STONE_WIDTH * 152f / 176f;
    private static final float BODY_LENGTH = STONE_HEIGHT * 297f / 328f;
    private static final float BODY_SHIFT = STONE_HEIGHT * 7.5f / 328f;
    private static final float UPPER_ROW_Y = 724f;
    private static final float MIDDLE_ROW_Y = UPPER_ROW_Y - 3f * BODY_LENGTH;
    private static final float LOWER_ROW_Y = MIDDLE_ROW_Y - BODY_LENGTH;
    private static final float LEFT_RAIL = 735f;
    private static final float RIGHT_RAIL_LIMIT = CHAIN_RIGHT - BODY_WIDTH / 2f;
    private static final float RAIL_INSET = BODY_WIDTH / 2f;
    private static final float FREE_SHIFT = 72f;

    private Map<Placement, Pose> planChain(List<Placement> pieces, boolean commit) {
        boolean preferLeft = chainBendsLeft != null
            ? chainBendsLeft : leftOverflowsMore(pieces, firstPlacement);

        Map<Placement, Pose> poses = layoutChain(pieces, firstPlacement, preferLeft, firstCorner);

        if (commit) {
            for (int i = 0; i + 1 < pieces.size(); i++) {
                int current = preferLeft ? pieces.size() - 1 - i : i;
                int next = preferLeft ? current - 1 : current + 1;
                Pose from = poses.get(pieces.get(current));
                Pose to = poses.get(pieces.get(next));
                if (from.y - to.y > 50f) {
                    chainBendsLeft = preferLeft;
                    firstCorner = pieces.get(current);
                    break;
                }
            }
        }

        return poses;
    }

    private static boolean leftOverflowsMore(List<Placement> pieces, Placement firstPlacement) {
        if (pieces.isEmpty()) {
            return false;
        }

        float length = rowLength(pieces, 0, pieces.size());
        float start = preferredStart(pieces, firstPlacement);
        return CHAIN_LEFT - start > start + length - CHAIN_RIGHT;
    }

    private static float preferredStart(List<Placement> pieces, Placement firstPlacement) {
        Placement anchor = firstPlacement != null ? firstPlacement : pieces.get(0);
        float before = 0f;
        for (Placement piece : pieces) {
            if (piece == anchor) {
                break;
            }
            before += visualWidth(piece);
        }
        return WIDTH / 2f - before - visualWidth(anchor) / 2f;
    }

    private static Map<Placement, Pose> layoutChain(
        List<Placement> logical, Placement firstPlacement, boolean bendLeft, Placement firstCorner
    ) {
        Map<Placement, Pose> poses = new IdentityHashMap<>();
        int count = logical.size();
        if (count == 0) {
            return poses;
        }

        float length = rowLength(logical, 0, count);
        float preferred = preferredStart(logical, firstPlacement);
        float straight = Math.max(CHAIN_LEFT, Math.min(preferred, CHAIN_RIGHT - length));
        boolean fitsStraight = length <= CHAIN_RIGHT - CHAIN_LEFT;
        if (firstCorner == null && fitsStraight
            && (count < 4 || Math.abs(straight - preferred) <= FREE_SHIFT)) {
            placeRow(poses, logical, 0, count, straight, UPPER_ROW_Y, true, false);
            return poses;
        }

        List<Placement> pieces = logical;
        if (bendLeft) {
            pieces = new ArrayList<>(logical);
            java.util.Collections.reverse(pieces);
        }

        int cornerIndex = firstCorner == null ? -1 : pieces.indexOf(firstCorner);
        if (cornerIndex > 0 && placeBentChain(poses, pieces, cornerIndex, bendLeft)) {
            return poses;
        }
        if (placeBentChain(poses, pieces, count - 3, bendLeft)) {
            return poses;
        }

        if (fitsStraight) {
            placeRow(poses, logical, 0, count, straight, UPPER_ROW_Y, true, false);
            return poses;
        }

        throw new IllegalStateException("A corrente excedeu a capacidade da mesa.");
    }

    private static boolean placeBentChain(
        Map<Placement, Pose> poses, List<Placement> pieces, int maxUpper, boolean bendLeft
    ) {
        int count = pieces.size();
        // A primeira descida leva a pedra nova e pelo menos as duas anteriores.
        for (int upper = Math.min(maxUpper, count - 3); upper >= 1; upper--) {
            int rightCount = Math.min(4, count - upper);
            int middleStart = upper + rightCount;
            float upperLength = rowLength(pieces, 0, upper);
            float middleLength = rowLength(pieces, middleStart, count);
            float right = Math.max(1250f, Math.max(
                CHAIN_LEFT + upperLength + RAIL_INSET,
                LEFT_RAIL + middleLength + RAIL_INSET
            ));
            if (right > RIGHT_RAIL_LIMIT) {
                continue;
            }

            placeRow(poses, pieces, 0, upper,
                right - RAIL_INSET - upperLength, UPPER_ROW_Y, true, bendLeft);
            placeRail(poses, pieces, upper, rightCount, right, UPPER_ROW_Y, bendLeft);
            placeRow(poses, pieces, middleStart, count,
                right - RAIL_INSET, MIDDLE_ROW_Y, false, bendLeft);
            return true;
        }

        for (int upper = Math.min(maxUpper, count - 5); upper >= 1; upper--) {
            float upperLength = rowLength(pieces, 0, upper);
            int middleStart = upper + 4;

            for (int middleEnd = count - 2; middleEnd > middleStart; middleEnd--) {
                int lowerStart = middleEnd + 2;
                float middleLength = rowLength(pieces, middleStart, middleEnd);
                float lowerLength = rowLength(pieces, lowerStart, count);
                float railDistance = middleLength + 2f * RAIL_INSET;
                float left = Math.max(LEFT_RAIL, CHAIN_LEFT + upperLength + RAIL_INSET - railDistance);
                float right = left + railDistance;

                if (right > RIGHT_RAIL_LIMIT
                    || left + RAIL_INSET + lowerLength > CHAIN_RIGHT) {
                    continue;
                }

                placeRow(poses, pieces, 0, upper,
                    right - RAIL_INSET - upperLength, UPPER_ROW_Y, true, bendLeft);
                placeRail(poses, pieces, upper, 4, right, UPPER_ROW_Y, bendLeft);
                placeRow(poses, pieces, middleStart, middleEnd,
                    right - RAIL_INSET, MIDDLE_ROW_Y, false, bendLeft);
                placeRail(poses, pieces, middleEnd, 2, left, MIDDLE_ROW_Y, bendLeft);
                placeRow(poses, pieces, lowerStart, count,
                    left + RAIL_INSET, LOWER_ROW_Y, true, bendLeft);
                return true;
            }
        }

        return false;
    }

    private static float rowLength(List<Placement> pieces, int from, int to) {
        float length = 0f;
        for (int i = from; i < to; i++) {
            length += visualWidth(pieces.get(i));
        }
        return length;
    }

    private static void placeRow(
        Map<Placement, Pose> poses, List<Placement> pieces,
        int from, int to, float edge, float y, boolean goesRight, boolean mirrored
    ) {
        float direction = goesRight ? 1f : -1f;
        boolean actualGoesRight = goesRight != mirrored;
        for (int i = from; i < to; i++) {
            Placement piece = pieces.get(i);
            float width = visualWidth(piece);
            float centerX = edge + direction * width / 2f;
            int incoming = mirrored ? piece.right : piece.left;
            boolean firstIsIncoming = piece.stone.first == incoming;
            float angle = piece.stone.isDouble() ? 0f
                : firstIsIncoming == actualGoesRight ? 90f : -90f;

            putPose(poses, piece, mirrorX(centerX, mirrored), y, angle);
            edge += direction * width;
        }
    }

    private static void placeRail(
        Map<Placement, Pose> poses, List<Placement> pieces,
        int from, int count, float x, float y, boolean mirrored
    ) {
        for (int i = 0; i < count; i++) {
            Placement piece = pieces.get(from + i);
            int incoming = mirrored ? piece.right : piece.left;
            float angle = piece.stone.first == incoming ? 0f : 180f;
            putPose(poses, piece, mirrorX(x, mirrored), y - i * BODY_LENGTH, angle);
        }
    }

    private static float mirrorX(float x, boolean mirrored) {
        return mirrored ? CHAIN_LEFT + CHAIN_RIGHT - x : x;
    }

    private static void putPose(
        Map<Placement, Pose> poses, Placement piece, float centerX, float centerY, float angle
    ) {
        float artX = angle == 90f ? -BODY_SHIFT : angle == -90f ? BODY_SHIFT : 0f;
        float artY = angle == 0f ? BODY_SHIFT : angle == 180f ? -BODY_SHIFT : 0f;
        poses.put(piece, new Pose(
            centerX - STONE_WIDTH / 2f - artX,
            centerY - STONE_HEIGHT / 2f - artY,
            STONE_WIDTH, STONE_HEIGHT, angle
        ));
    }

    private static float visualWidth(Placement placement) {
        return placement.stone.isDouble() ? BODY_WIDTH : BODY_LENGTH;
    }

    private void makeRoomForChain(Map<Placement, Pose> poses) {
        boolean needsSpace = false;
        for (Pose pose : poses.values()) {
            float centerX = pose.x + STONE_WIDTH / 2f;
            float centerY = pose.y + STONE_HEIGHT / 2f;
            float halfWidth = Math.abs(pose.rotation) == 90f ? STONE_HEIGHT / 2f : STONE_WIDTH / 2f;
            float halfHeight = Math.abs(pose.rotation) == 90f ? STONE_WIDTH / 2f : STONE_HEIGHT / 2f;
            if (centerX + halfWidth > 490f && centerX - halfWidth < 710f
                && centerY + halfHeight > 530f && centerY - halfHeight < 680f) {
                needsSpace = true;
                break;
            }
        }

        if (needsSpace != cavaAside) {
            cavaAside = needsSpace;
            cavaView.clearActions();
            cavaView.addAction(Actions.moveTo(
                cavaAside ? 260f : 490f, cavaView.getY(), 0.28f, Interpolation.smooth
            ));
        }
    }

    private void applyChainLayout(Map<Placement, Pose> poses, Placement moving) {
        makeRoomForChain(poses);
        for (Map.Entry<Placement, DominoTile> entry : chainActors.entrySet()) {
            if (entry.getKey() != moving) {
                Pose pose = poses.get(entry.getKey());
                DominoTile tile = entry.getValue();
                float rotation = tile.getRotation();
                float delta = (pose.rotation - rotation + 540f) % 360f - 180f;
                if (Math.abs(tile.getX() - pose.x) < 0.1f
                    && Math.abs(tile.getY() - pose.y) < 0.1f && Math.abs(delta) < 0.1f) {
                    continue;
                }
                tile.clearActions();
                tile.addAction(Actions.parallel(
                    Actions.moveTo(pose.x, pose.y, 0.24f, Interpolation.smooth),
                    Actions.rotateTo(rotation + delta, 0.24f, Interpolation.smooth)
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

    private Texture createButtonTexture() {
        Pixmap pixmap = new Pixmap(600, 116, Pixmap.Format.RGBA8888);
        try {
            pixmap.setBlending(Pixmap.Blending.None);
            drawButtonBody(pixmap, 0, Color.valueOf("3F241266"));
            drawButtonBody(pixmap, 3, Color.valueOf("F0DCB2"));
            Texture texture = new Texture(pixmap);
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            return texture;
        } finally {
            pixmap.dispose();
        }
    }

    private void drawButtonBody(Pixmap pixmap, int inset, Color color) {
        int radius = 57 - inset;
        pixmap.setColor(color);
        pixmap.fillCircle(57, 57, radius);
        pixmap.fillCircle(542, 57, radius);
        pixmap.fillRectangle(57, inset, 486, 116 - 2 * inset);
    }

    private TextButton createButton(String text) {
        TextureRegionDrawable up = new TextureRegionDrawable(new TextureRegion(buttonTexture));
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle(
            up, up.tint(Color.valueOf("F3D8A2")), null, controlsFont
        );
        style.fontColor = Color.valueOf("3F2412");
        TextButton button = new TextButton(text, style);
        button.setBounds((WIDTH - 300f) / 2f, 272f, 300f, 58f);
        button.setVisible(false);
        hudLayer.addActor(button);
        return button;
    }

    @Override
    public void show() {
        updateEntrance();

        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        float dt = Math.min(delta, 1f / 30f);

        entranceTime = Math.min(entranceTime + dt, PREPARATION_END);
        updateEntrance();

        if (!entranceFinished && entranceTime >= PREPARATION_END) {
            entranceFinished = true;
            refreshTurn();
        }

        ScreenUtils.clear(0f, 0f, 0f, 1f);

        stage.getViewport().apply();
        stage.act(Math.min(delta, 1f / 30f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void hide() {
        if (Gdx.input.getInputProcessor() == stage) {
            Gdx.input.setInputProcessor(null);
        }
        matchSettings.close();
    }

    @Override
    public void dispose() {
        hide();

        stage.dispose();
        roundResultView.dispose();
        controlsFont.dispose();
        buttonTexture.dispose();
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
        cavaView.dispose();
    }
}
