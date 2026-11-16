package br.com.dominando.frontend.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;

import java.util.ArrayList;
import java.util.List;

public final class IntervalView extends Group implements Disposable {
    public enum Snack {
        OVO(2, 645f, 732f),
        TORRESMO(4, 1355f, 732f),
        COXINHA(4, 990f, 932f);

        private final int price;
        private final float centerX;
        private final float statusTop;

        Snack(int price, float centerX, float statusTop) {
            this.price = price;
            this.centerX = centerX;
            this.statusTop = statusTop;
        }
    }

    private static final Color CREAM = Color.valueOf("F0DCB2");
    private static final Color GREEN = Color.valueOf("234730");
    private static final Color BROWN = Color.valueOf("3F2412");

    private final List<Texture> textures = new ArrayList<>();
    private final BitmapFont font;
    private final Label balanceLabel;
    private final Label timerLabel;
    private final Label warningTimerLabel;
    private final Image warning;
    private final Label[] missingBalance = new Label[Snack.values().length];
    private final Image[] boughtBadges = new Image[Snack.values().length];
    private float remaining;

    public IntervalView() {
        setSize(1920f, 1080f);
        setVisible(false);
        setTouchable(Touchable.enabled);

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
            Gdx.files.internal("fonts/NunitoSans-SemiBold.ttf")
        );
        try {
            FreeTypeFontGenerator.FreeTypeFontParameter params =
                new FreeTypeFontGenerator.FreeTypeFontParameter();
            params.size = 32;
            params.minFilter = Texture.TextureFilter.Linear;
            params.magFilter = Texture.TextureFilter.Linear;
            font = generator.generateFont(params);
        } finally {
            generator.dispose();
        }

        addImage(load("bandeja.png"), 0f, 0f, 1920f, 1080f);
        balanceLabel = addLabel(GREEN, 112f, 980f, 52f, 40f);
        timerLabel = addLabel(CREAM, 1728f, 984f, 140f, 48f);

        Texture boughtTexture = load("comprado.png");
        for (Snack snack : Snack.values()) {
            int i = snack.ordinal();
            float x = snack.centerX - 135f;

            missingBalance[i] = addLabel(CREAM, x, 1048f - snack.statusTop, 270f, 32f);
            missingBalance[i].setFontScale(22f / 32f);
            boughtBadges[i] = addImage(boughtTexture, x, 1008f - snack.statusTop, 270f, 72f);
        }

        warning = addImage(load("aviso.png"), 1408f, 960f, 464f, 80f);
        warningTimerLabel = addLabel(BROWN, 1766f, 974f, 80f, 56f);
        warningTimerLabel.setFontScale(1.25f);
    }

    public void open(int balance, Snack alreadyBought) {
        remaining = 30f;
        balanceLabel.setText(Integer.toString(balance));

        for (Snack snack : Snack.values()) {
            int i = snack.ordinal();
            boolean bought = snack == alreadyBought;
            int missing = snack.price - balance;

            boughtBadges[i].setVisible(bought);
            missingBalance[i].setVisible(!bought && missing > 0);
            missingBalance[i].setText(
                missing == 1 ? "Falta 1 moeda" : "Faltam " + missing + " moedas"
            );
        }

        refreshClock();
        setVisible(true);
    }

    @Override
    public void act(float delta) {
        if (!isVisible()) {
            return;
        }
        super.act(delta);
        remaining = Math.max(0f, remaining - delta);
        refreshClock();
    }

    public boolean isTimeUp() {
        return remaining <= 0f;
    }

    private void refreshClock() {
        int seconds = (int) Math.ceil(remaining);
        boolean lastSeconds = seconds <= 5;
        String time = seconds + "s";

        timerLabel.setText(time);
        timerLabel.setVisible(!lastSeconds);
        warning.setVisible(lastSeconds);
        warningTimerLabel.setText(time);
        warningTimerLabel.setVisible(lastSeconds);
    }


    private Texture load(String name) {
        Texture texture = new Texture(Gdx.files.internal("ui/interval/" + name));
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        textures.add(texture);
        return texture;
    }

    private Image addImage(Texture texture, float x, float y, float width, float height) {
        Image image = new Image(texture);
        image.setBounds(x, y, width, height);
        image.setTouchable(Touchable.disabled);
        addActor(image);
        return image;
    }

    private Label addLabel(Color color, float x, float y, float width, float height) {
        Label label = new Label("", new Label.LabelStyle(font, color));
        label.setBounds(x, y, width, height);
        label.setAlignment(Align.center);
        label.setTouchable(Touchable.disabled);
        addActor(label);
        return label;
    }

    @Override
    public void dispose() {
        font.dispose();
        for (Texture texture : textures) {
            texture.dispose();
        }
    }
}
