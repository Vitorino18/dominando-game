package br.com.dominando.frontend.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Scaling;

import java.util.Random;

public final class MatchCelebrationView
    extends Group implements Disposable {

    public static final float DURATION = 4.2f;

    private static final Color[] COLORS = {
        Color.valueOf("F2B632"),
        Color.valueOf("F0DCB2"),
        Color.valueOf("8CC58E")
    };

    private final Texture logoTexture;
    private final Texture sparkTexture;

    private final Image logo;
    private final Image glow;
    private final Image[] sparks = new Image[96];

    private final Label title;
    private final Label subtitle;

    public MatchCelebrationView(BitmapFont displayFont, BitmapFont textFont, Drawable glowDrawable) {
        setSize(1920f, 1080f);
        setTouchable(Touchable.disabled);
        setVisible(false);

        sparkTexture = createSparkTexture();

        TextureRegionDrawable sparkDrawable =
            new TextureRegionDrawable(
                new TextureRegion(sparkTexture)
            );

        for (int i = 0; i < sparks.length; i++) {
            sparks[i] = new Image(sparkDrawable);
            sparks[i].setTouchable(Touchable.disabled);
            sparks[i].setVisible(false);
            addActor(sparks[i]);
        }

        glow = new Image(glowDrawable);
        glow.setBounds(460f, 240f, 1000f, 720f);
        glow.setTouchable(Touchable.disabled);
        addActor(glow);

        logoTexture = new Texture(Gdx.files.internal("ui/opening/logo-desce-uma.png"));
        logoTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        logo = new Image(logoTexture);
        logo.setScaling(Scaling.fit);
        logo.setBounds(650f, 360f, 620f, 450f);
        logo.setOrigin(Align.center);
        logo.setTouchable(Touchable.disabled);
        addActor(logo);

        title = new Label("", new Label.LabelStyle(displayFont, Color.valueOf("F0DCB2")));
        title.setFontScale(0.75f);
        title.setAlignment(Align.center);
        title.setBounds(360f, 245f, 1200f, 92f);
        title.setTouchable(Touchable.disabled);
        addActor(title);

        subtitle = new Label("", new Label.LabelStyle(textFont, Color.valueOf("F0DCB2")));
        subtitle.setAlignment(Align.center);
        subtitle.setBounds(360f, 176f, 1200f, 48f);
        subtitle.setTouchable(Touchable.disabled);
        addActor(subtitle);
    }

    public void start(boolean humanWon, int humanCaps, int opponentCaps) {
        stop();

        setVisible(true);
        getColor().a = 1f;

        title.setText(humanWon ? "ESSA PARTIDA É SUA!" : "VITÓRIA DO ADVERSÁRIO!");
        subtitle.setText(humanWon ? "Você conquistou " + humanCaps + " tampinhas." : "O adversário conquistou " + opponentCaps + " tampinhas.");

        logo.setScale(0.60f);
        logo.getColor().a = 0f;
        logo.addAction(Actions.parallel(
            Actions.fadeIn(0.35f),
            Actions.scaleTo(1f, 1f, 0.75f, Interpolation.swingOut)
        ));

        glow.getColor().a = 0f;
        glow.addAction(Actions.sequence(
            Actions.delay(0.20f),
            Actions.alpha(humanWon ? 0.95f : 0.55f, 0.65f)
        ));

        title.setY(225f);
        title.getColor().a = 0f;
        title.addAction(Actions.sequence(
            Actions.delay(0.35f),
            Actions.parallel(
                Actions.fadeIn(0.35f),
                Actions.moveTo(360f, 245f, 0.5f, Interpolation.pow3Out)
            )
        ));

        subtitle.getColor().a = 0f;
        subtitle.addAction(Actions.sequence(
            Actions.delay(0.8f),
            Actions.fadeIn(0.35f)
        ));

        animateFireworks(humanWon);

        addAction(Actions.sequence(
            Actions.delay(DURATION - 0.4f),
            Actions.fadeOut(0.4f)
        ));
    }

    private void animateFireworks(boolean humanWon) {
        Random random = new Random(42L);
        float intensity = humanWon ? 0.95f : 0.60f;

        for (int i = 0; i < sparks.length; i++) {
            int burst = i / 24;
            int ray = i % 24;

            float angle = ray * 15f + random.nextFloat() * 8f;

            double radians = Math.toRadians(angle);
            float radius = 220f + random.nextFloat() * 160f;
            float dx = (float) Math.cos(radians) * radius;
            float dy = (float) Math.sin(radians) * radius - 70f;

            float centerX = 960f + (burst % 2 == 0 ? -160f : 160f);
            float centerY = 620f + (burst % 3) * 55f;
            float duration = 1.1f + random.nextFloat() * 0.3f;
            float delay = 0.35f + burst * 0.65f;

            Image spark = sparks[i];
            spark.setBounds(centerX, centerY, 26f, 7f);
            spark.setOrigin(Align.center);
            spark.setRotation(angle);
            spark.setScale(1f);
            spark.setColor(COLORS[i % COLORS.length]);
            spark.getColor().a = 0f;

            spark.addAction(Actions.sequence(
                Actions.delay(delay),
                Actions.visible(true),
                Actions.parallel(
                    Actions.moveBy(dx, dy, duration, Interpolation.pow2Out),
                    Actions.scaleTo(0.25f, 0.25f, duration),
                    Actions.sequence(Actions.alpha(intensity, 0.06f), Actions.fadeOut(duration - 0.06f))),
                Actions.visible(false)
            ));
        }
    }

    public void stop() {
        clearActions();
        logo.clearActions();
        glow.clearActions();
        title.clearActions();
        subtitle.clearActions();

        for (Image spark : sparks) {
            spark.clearActions();
            spark.setVisible(false);
        }

        setVisible(false);
    }

    private Texture createSparkTexture() {
        int size = 16;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);

        try {
            pixmap.setBlending(Pixmap.Blending.None);

            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    float dx = (x - 7.5f) / 7.5f;
                    float dy = (y - 7.5f) / 7.5f;
                    float alpha = Math.max(0f, 1f - dx * dx - dy * dy);

                    pixmap.drawPixel(x, y, Color.rgba8888(1f, 1f, 1f, alpha * alpha));
                }
            }

            Texture texture = new Texture(pixmap);
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            return texture;
        } finally {
            pixmap.dispose();
        }
    }

    @Override
    public void dispose() {
        stop();
        logoTexture.dispose();
        sparkTexture.dispose();
    }
}
