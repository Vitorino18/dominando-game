package br.com.dominando.frontend.screens;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import org.w3c.dom.Text;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import br.com.dominando.frontend.settings.GameSettings;
import br.com.dominando.frontend.ui.OpeningDialogs;
import br.com.dominando.frontend.ui.DominoAssets;
import br.com.dominando.frontend.ui.DominoTile;
import br.com.dominando.frontend.ui.CavaView;
import br.com.dominando.frontend.ui.ScoreView;
import br.com.dominando.frontend.ui.PlayerInfoView;

public class MatchScreen extends ScreenAdapter {
    private static final float WIDTH = 1920f;
    private static final float HEIGHT = 1080f;
    private static final float ENTRANCE_DURATION = 4.4f;
    private static final float SHUFFLE_DURATION = 2.8f;
    private static final float PREPARATION_END = ENTRANCE_DURATION + SHUFFLE_DURATION;
    private static final float FIRST_STONE_X = 930f;
    private static final float FIRST_STONE_TOP = 300f;
    private static final float FIRST_STONE_WIDTH = 60f;
    private static final float FIRST_STONE_HEIGHT = 111.82f;
    private static final float FIRST_STONE_Y = HEIGHT - FIRST_STONE_TOP - FIRST_STONE_HEIGHT;
    private static final float TILE_CONTACT_OFFSET = 10f;
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
    private final Group shuffleLayer = new Group();
    private final Group tilesLayer;
    private final Group hudLayer;
    private final OpeningDialogs matchSettings;
    private final Texture matchSettingsIcon;

    private final CavaView cavaView;

    private final ScoreView playerScoreView;
    private final ScoreView opponentScoreView;

    private final PlayerInfoView playerInfoView;
    private final PlayerInfoView opponentInfoView;

    private DominoTile selectedTile;
    private DominoTile openingTile;
    private final Group firstStoneTarget;
    private boolean firstStonePlayed;
    private DominoTile opponentReplyTile;
    private boolean playerCanSelect = true;

    private final DominoTile leftEndPreview;
    private final DominoTile rightEndPreview;

    private int playerHandCount;



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

            DominoTile tile = new DominoTile(dominoAssets, 0, 0);
            tile.setFaceDown(true);
            tile.setBounds(-22f, -41f, 44f, 82f);
            tile.setRotation(pose[2]);

            position.addActor(tile);
            shuffleLayer.addActor(position);
        }
    }

    private void updateShuffle() {
        boolean shuffling = entranceTime >= ENTRANCE_DURATION
            && entranceTime < PREPARATION_END;
        shuffleLayer.setVisible(shuffling);

        if (!shuffling) {
            return;
        }

        float progress = (entranceTime - ENTRANCE_DURATION) / SHUFFLE_DURATION;
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
            float y = centerY + (float) (verticalScale * radiusScale
                * (offsetX * sin + offsetY * cos));

            position.getChildren().first().setPosition(
                x - startX - 22f,
                y - startY - 41f
            );
        }
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
        createDemoTiles();

        firstStoneTarget = createFirstStoneTarget();
        tilesLayer.addActor(firstStoneTarget);

        leftEndPreview = createEndPreview();
        rightEndPreview = createEndPreview();

        tilesLayer.addActor(leftEndPreview);
        tilesLayer.addActor(rightEndPreview);

        cavaView = new CavaView(dominoAssets, 14);
        float cavaScale = 1f;

        cavaView.setOrigin(0f, 0f);
        cavaView.setScale(cavaScale);
        cavaView.setPosition(490f, HEIGHT - 400f - 120f * cavaScale);

        tilesLayer.addActor(cavaView);

        playerScoreView = new ScoreView(false, 3);
        opponentScoreView = new ScoreView(true, 2);

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

        playerInfoView.setActiveTurn(true);
        opponentInfoView.setActiveTurn(false);
    }

    private DominoTile createEndPreview() {
        DominoTile preview = new DominoTile(dominoAssets, 0, 0);
        preview.setSize(FIRST_STONE_WIDTH, FIRST_STONE_HEIGHT);
        preview.getColor().a = 0.42f;
        preview.setTouchable(Touchable.enabled);
        preview.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                playSelectedTileAt(preview);
            }
        });
        preview.setVisible(false);
        return preview;
    }

    private Group createFirstStoneTarget() {
        float paddingX = 32f;
        float paddingY = 24f;

        Group target = new Group();
        target.setBounds(FIRST_STONE_X - paddingX, FIRST_STONE_Y - paddingY, FIRST_STONE_WIDTH + 2f * paddingX, FIRST_STONE_HEIGHT + 2f * paddingY);
        target.setVisible(false);

        DominoTile preview = new DominoTile(dominoAssets, 6, 6);
        preview.setBounds(paddingX, paddingY, FIRST_STONE_WIDTH, FIRST_STONE_HEIGHT);
        preview.getColor().a = 0.42f;
        preview.setTouchable(Touchable.disabled);
        target.addActor(preview);

        target.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                playFirstStone();
            }
        });

        return target;
    }

    private void createDemoTiles() {
        tilesLayer.clearChildren();

        int[][] demoHand = {
            {6, 6},
            {0, 6},
            {2, 6},
            {1, 3},
            {2, 4},
            {3, 5},
            {0, 0}
        };

        playerHandCount = demoHand.length;

        for (int i = 0; i < demoHand.length; i++) {
            DominoTile tile = new DominoTile(dominoAssets, demoHand[i][0], demoHand[i][1]);

            float x = 616f + i * 100f;
            float y = HEIGHT - 820f - 164f;

            tile.setBounds(x, y, 88f, 164f);

            if (demoHand[i][0] == 6 && demoHand[i][1] == 6) {
                openingTile = tile;
            }

            tile.addListener(new ClickListener(){
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (!playerCanSelect) {
                        return;
                    }

                    setSelectedTile(selectedTile == tile ? null : tile);
                }
            });

            tilesLayer.addActor(tile);
        }

        float opponentWidth = 44f;
        float opponentHeight = 82f;
        float gap = 10f;

        float totalWidth = 7f * opponentWidth + 6f * gap;
        float startX = (WIDTH - totalWidth) / 2f;

        for (int i = 0; i < 7; i++) {
            DominoTile tile = new DominoTile(dominoAssets, 0, 0);
            tile.setFaceDown(true);

            tile.setTouchable(Touchable.disabled);

            tile.setBounds(startX + i * (opponentWidth + gap), HEIGHT - 132f - opponentHeight, opponentWidth, opponentHeight);
            tilesLayer.addActor(tile);

            if (i == 0) {
                opponentReplyTile = tile;
            }
        }
    }

    private void setSelectedTile(DominoTile tile) {
        if (selectedTile == tile) {
            return;
        }

        if (selectedTile != null) {
            selectedTile.setSelected(false);
        }

        selectedTile = tile;

        if (selectedTile != null) {
            selectedTile.setSelected(true);
        }

        firstStoneTarget.setVisible(!firstStonePlayed && selectedTile != null && selectedTile == openingTile);
        updateEndPreviews();
    }

    private void updateEndPreviews() {
        leftEndPreview.setVisible(false);
        rightEndPreview.setVisible(false);

        if (!firstStonePlayed || !playerCanSelect || selectedTile == null) {
            return;
        }

        float leftEdgeX = openingTile.getX();
        float rightEdgeX = opponentReplyTile.getX() + opponentReplyTile.getWidth() / 2f + opponentReplyTile.getHeight() / 2f;

        showEndPreview(leftEndPreview, openingTile.getUpperValue(), leftEdgeX, true);
        showEndPreview(rightEndPreview, opponentReplyTile.getUpperValue(), rightEdgeX, false);
    }

    private void showEndPreview(DominoTile preview, int endValue, float edgeX, boolean leftSide) {
        int upper = selectedTile.getUpperValue();
        int lower = selectedTile.getLowerValue();

        if (upper != endValue && lower != endValue) {
            return;
        }

        boolean doubleTile = upper == lower;
        float rotation = 0f;
        float horizontalWidth = FIRST_STONE_WIDTH;

        if (!doubleTile) {
            boolean upperFacesRight = leftSide ? upper == endValue : lower == endValue;
            rotation = upperFacesRight ? -90f : 90f;
            horizontalWidth = FIRST_STONE_HEIGHT;
        }

        float distance = horizontalWidth / 2f - TILE_CONTACT_OFFSET;
        float centerX = leftSide ? edgeX - distance : edgeX + distance;

        preview.setValues(upper, lower);
        preview.setRotation(rotation);
        preview.setPosition(centerX - FIRST_STONE_WIDTH / 2f, FIRST_STONE_Y);
        preview.setVisible(true);
    }

    private void playFirstStone() {
        if (firstStonePlayed || openingTile == null || selectedTile != openingTile) {
            return;
        }

        firstStonePlayed = true;
        playerCanSelect = false;
        DominoTile tile = openingTile;
        setSelectedTile(null);

        tile.setTouchable(Touchable.disabled);
        tile.toFront();

        float duration = 0.32f;

        tile.addAction(Actions.sequence(
            Actions.parallel(
                Actions.moveTo(FIRST_STONE_X, FIRST_STONE_Y, duration, Interpolation.sineOut),
                Actions.sizeTo(FIRST_STONE_WIDTH, FIRST_STONE_HEIGHT, duration, Interpolation.sineOut)
            ),
            Actions.run(() -> {
                playerHandCount--;
                playerInfoView.setDisplayedCount(playerHandCount);
                playerInfoView.setActiveTurn(false);
                opponentInfoView.setActiveTurn(true);
                playOpponentReply();
            })
        ));
    }

    private void playOpponentReply() {
        DominoTile tile = opponentReplyTile;

        float centerX = FIRST_STONE_X + FIRST_STONE_WIDTH + FIRST_STONE_HEIGHT / 2f;
        float targetX = centerX - FIRST_STONE_WIDTH / 2f - TILE_CONTACT_OFFSET;
        float targetY = FIRST_STONE_Y;
        float duration = 0.32f;

        tile.addAction(Actions.sequence(
            Actions.delay(0.65f),
            Actions.run(() -> {
                tile.setValues(3,6);
                tile.setFaceDown(false);
                tile.toFront();
            }),
            Actions.parallel(
                Actions.moveTo(targetX, targetY, duration, Interpolation.sineOut),
                Actions.sizeTo(FIRST_STONE_WIDTH, FIRST_STONE_HEIGHT, duration, Interpolation.sineOut),
                Actions.rotateTo(-90f, duration, Interpolation.sineOut)
            ),
            Actions.run(() -> {
                opponentInfoView.setDisplayedCount(6);
                opponentInfoView.setActiveTurn(false);
                playerInfoView.setActiveTurn(true);
                playerCanSelect = true;
            })
        ));
    }

    private void playSelectedTileAt(DominoTile preview) {
        if (!playerCanSelect || selectedTile == null || !preview.isVisible()) {
            return;
        }

        DominoTile tile = selectedTile;
        float targetX = preview.getX();
        float targetY = preview.getY();
        float targetRotation = preview.getRotation();
        float duration = 0.32f;

        playerCanSelect = false;
        setSelectedTile(null);

        tile.setTouchable(Touchable.disabled);
        tile.toFront();

        tile.addAction(Actions.sequence(
            Actions.parallel(
                Actions.moveTo(targetX, targetY, duration, Interpolation.sineOut),
                Actions.sizeTo(FIRST_STONE_WIDTH, FIRST_STONE_HEIGHT, duration, Interpolation.sineOut),
                Actions.rotateTo(targetRotation, duration, Interpolation.sineOut)
            ),
            Actions.run(() -> {
                playerHandCount--;
                playerInfoView.setDisplayedCount(playerHandCount);
                playerInfoView.setActiveTurn(false);
                opponentInfoView.setActiveTurn(true);
            })
        ));
    }

    @Override
    public void show() {
        entranceTime = 0f;
        setSelectedTile(null);
        updateEntrance();

        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        float dt = Math.min(delta, 1f / 30f);

        entranceTime = Math.min(entranceTime + dt, PREPARATION_END);
        updateEntrance();

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
        matchSettings.close();
        if (Gdx.input.getInputProcessor() == stage) {
            Gdx.input.setInputProcessor(null);
        }
    }

    @Override
    public void dispose() {
        hide();

        stage.dispose();
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
    }
}
