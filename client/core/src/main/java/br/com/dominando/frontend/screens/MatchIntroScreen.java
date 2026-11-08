package br.com.dominando.frontend.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class MatchIntroScreen extends ScreenAdapter {

    private static final float WIDTH = 1920f;
    private static final float HEIGHT = 1080f;

    private final Stage stage;
    private final Group scene = new Group();
    private final Array<Texture> textures = new Array<>();

    private final BitmapFont titleFont;
    private final BitmapFont textFont;

    private final Label title;
    private final Label subtitle;
    private final Image dimmer;
    private final Image leftHand;
    private final Image rightHand;
    private final Image finalFade;

    private final Runnable onFinished;

    private float elapsed;
    private boolean finished;

    public MatchIntroScreen(Runnable onFinished) {
        this.onFinished = onFinished;

        stage = new Stage(new FitViewport(WIDTH, HEIGHT));

        titleFont = createFont("fonts/LilitaOne-Regular.ttf", 48);
        textFont = createFont("fonts/NunitoSans-Regular.ttf", 28);

        scene.setSize(WIDTH, HEIGHT);
        scene.setOrigin(WIDTH / 2f, HEIGHT / 2f);
        scene.setTouchable(Touchable.disabled);
        stage.addActor(scene);

        Image background = new Image(loadTexture("ui/transition/fundo-engradados.png"));
        place(background, 0f, 0f, WIDTH, HEIGHT);
        scene.addActor(background);

        Image logo = new Image(loadTexture("ui/transition/logo-desce-uma.png"));
        place(logo, 534f, 70f, 851.776f, 620f);
        scene.addActor(logo);

        title = createLabel("A MESA TÁ PRONTA", titleFont, Color.valueOf("234730"), 650f, 724f, 620f, 55f);
        scene.addActor(title);

        subtitle = createLabel("O outro jogador chegou", textFont, Color.valueOf("3F2412"), 650f, 788f, 620f, 40f);
        scene.addActor(subtitle);

        Texture blackTexture = createBlackTexture();

        dimmer = new Image(blackTexture);
        dimmer.setBounds(0f, 0f, WIDTH, HEIGHT);
        stage.addActor(dimmer);

        Texture handTexture = loadTexture("ui/transition/mao-brinde.png");

        TextureRegion leftRegion = new TextureRegion(handTexture);
        TextureRegion rightRegion = new TextureRegion(handTexture);
        rightRegion.flip(true, false);

        leftHand = createHand(leftRegion);
        rightHand = createHand(rightRegion);

        stage.addActor(leftHand);
        stage.addActor(rightHand);

        finalFade = new Image(blackTexture);
        finalFade.setBounds(0f, 0f, WIDTH, HEIGHT);
        stage.addActor(finalFade);

        Label demo = createLabel("SIMULAÇÃO LOCAL", textFont, Color.WHITE, 650f, 1015f, 620f, 40f);
        stage.addActor(demo);
    }

    private Image createHand(TextureRegion region) {
        Image hand = new Image(new TextureRegionDrawable(region)) {
            @Override
            public void draw(Batch batch, float parentAlpha) {
                int source = batch.getBlendSrcFunc();
                int destination = batch.getBlendDstFunc();
                int sourceAlpha = batch.getBlendSrcFuncAlpha();
                int destinationAlpha = batch.getBlendDstFuncAlpha();

                batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

                try {
                    super.draw(batch, parentAlpha);
                } finally {
                    batch.setBlendFunctionSeparate(source, destination, sourceAlpha, destinationAlpha);
                }
            }
        };

        hand.setTouchable(Touchable.disabled);
        return hand;
    }

    private void updateVisuals() {
        float entrance = progress(elapsed, 1.2f, 0.65f);

        scene.setScale(1f + entrance * 0.12f);
        title.getColor().a = 1f - entrance;
        subtitle.getColor().a = 1f - entrance;
        dimmer.getColor().a = entrance;

        boolean showHands = elapsed >= 2f;
        leftHand.setVisible(showHands);
        rightHand.setVisible(showHands);

        float approach = progress(elapsed, 2f, 0.7f);

        float handWidth = MathUtils.lerp(900f, 1099.5f, approach);
        float handHeight = MathUtils.lerp(600f, 733f, approach);

        place(leftHand, MathUtils.lerp(-100f, -108f, approach), MathUtils.lerp(276f, 256f, approach), handWidth, handHeight);
        place(rightHand, MathUtils.lerp(1120f, 918.5f, approach), MathUtils.lerp(300f, 280f, approach), handWidth, handHeight);

        finalFade.getColor().a = progress(elapsed, 3.15f, 0.4f);
    }

    private float progress(float time, float start, float duration) {
        float value = MathUtils.clamp(
            (time - start) / duration, 0f, 1f
        );

        return value * value * (3f - 2f * value);
    }

    private Texture loadTexture(String path) {
        Texture texture = new Texture(Gdx.files.internal(path));

        texture.setFilter(
            Texture.TextureFilter.Linear,
            Texture.TextureFilter.Linear
        );
        textures.add(texture);

        return texture;
    }

    private Texture createBlackTexture() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.BLACK);
        pixmap.fill();

        Texture texture;

        try {
            texture = new Texture(pixmap);
        } finally {
            pixmap.dispose();
        }

        textures.add(texture);
        return texture;
    }

    private Label createLabel(
        String text, BitmapFont font, Color color,
        float x, float top, float width, float height
    ) {
        Label label = new Label(
            text, new Label.LabelStyle(font, color)
        );
        label.setAlignment(Align.center);
        label.setTouchable(Touchable.disabled);

        place(label, x, top, width, height);
        return label;
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

    private void place(Actor actor, float x, float top, float width, float height) {
        actor.setBounds(x, HEIGHT - top - height, width, height);
    }

    @Override
    public void show() {
        elapsed = 0f;
        finished = false;
        updateVisuals();

        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        float animationDelta = Math.min(delta, 1f / 30f);
        elapsed += animationDelta;

        updateVisuals();

        ScreenUtils.clear(Color.BLACK);
        stage.getViewport().apply();
        stage.act(animationDelta);
        stage.draw();

        if (!finished && elapsed >= 3.55f) {
            finished = true;
            onFinished.run();
        }
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
    }

    @Override
    public void dispose() {
        hide();
        stage.dispose();

        titleFont.dispose();
        textFont.dispose();

        for (Texture texture : textures) {
            texture.dispose();
        }
        textures.clear();
    }
}
