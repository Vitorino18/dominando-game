package br.com.dominando.frontend.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;

public class CavaView extends Group implements Disposable {

    private final BitmapFont counterFont;
    private final Texture counterTexture;
    private final Label counterLabel;

    public CavaView(DominoAssets assets, int displayedCount) {
        setSize(170f, 120f);
        setTouchable(Touchable.disabled);

        addBack(assets, 40.04f, 66.68f, 82f, -12f);
        addBack(assets, 69.83f, 51.38f, 82f, 7f);
        addBack(assets, 101.49f, 67.76f, 82f, -5f);
        addBack(assets, 138.34f, 55.53f, 79.2f, 13f);

        counterTexture = createCounterTexture();

        Image badge = new Image(counterTexture);
        badge.setBounds(124f, 4f, 42f, 28f);
        badge.setTouchable(Touchable.disabled);
        addActor(badge);

        counterFont = createCounterFont();

        Label.LabelStyle style = new Label.LabelStyle(counterFont, Color.valueOf("234730"));
        counterLabel = new Label("", style);
        counterLabel.setAlignment(Align.center);
        counterLabel.setBounds(124f, 4f, 42f, 28f);
        counterLabel.setTouchable(Touchable.disabled);
        addActor(counterLabel);

        setDisplayedCount(displayedCount);
    }

    private void addBack(DominoAssets assets, float centerX, float centerFromTop, float height, float rotation) {
        DominoTile tile = new DominoTile(assets, 0, 0);
        tile.setFaceDown(true);
        tile.setBounds(centerX - 22f, 120f - centerFromTop - height / 2f, 44f, height);
        tile.setRotation(rotation);
        addActor(tile);
    }

    public void setDisplayedCount(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("A quantidade exibida nao pode ser negativa.");
        }

        counterLabel.setText(Integer.toString(count));
    }

    private BitmapFont createCounterFont() {
        FreeTypeFontGenerator generator =
            new FreeTypeFontGenerator(Gdx.files.internal("fonts/NunitoSans-Regular.ttf"));

        try {
            FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.size = 16;
            parameter.characters = "0123456789";
            parameter.minFilter = Texture.TextureFilter.Linear;
            parameter.magFilter = Texture.TextureFilter.Linear;

            return generator.generateFont(parameter);
        } finally {
            generator.dispose();
        }
    }

    private Texture createCounterTexture() {
        Pixmap pixmap = new Pixmap(84, 56, Pixmap.Format.RGBA8888);
        try {
            pixmap.setColor(Color.valueOf("3F2412"));
            fillRounded(pixmap, 0, 0, 84, 56, 12);
            pixmap.setColor(Color.valueOf("F0DCB2"));
            fillRounded(pixmap, 4, 4, 76, 48, 8);
            Texture texture = new Texture(pixmap);
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

            return texture;
        } finally {
            pixmap.dispose();
        }
    }

    private void fillRounded(Pixmap pixmap, int x,int y, int width, int height, int radius) {
        pixmap.fillRectangle(x + radius, y, width - radius * 2, height);
        pixmap.fillRectangle(x, y + radius, width, height - radius * 2);
        pixmap.fillCircle(x + radius, y + radius, radius);
        pixmap.fillCircle(x + width - radius - 1, y + radius, radius);
        pixmap.fillCircle(x + radius, y + height - radius - 1, radius);
        pixmap.fillCircle(x + width - radius - 1, y + height - radius - 1, radius);
    }

    @Override
    public void dispose() {
        counterFont.dispose();
        counterTexture.dispose();
    }
}
