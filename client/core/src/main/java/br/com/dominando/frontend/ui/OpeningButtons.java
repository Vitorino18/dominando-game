package br.com.dominando.frontend.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

import java.awt.*;

public class OpeningButtons implements Disposable {
    private final Color brown = Color.valueOf("3F2412");
    private final Array<Texture> textures = new Array<>();
    private final BitmapFont font;

    public OpeningButtons() {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/LilitaOne-Regular.ttf"));
        try {
            FreeTypeFontGenerator.FreeTypeFontParameter parameters = new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameters.size = 36;
            parameters.minFilter = Texture.TextureFilter.Linear;
            parameters.magFilter = Texture.TextureFilter.Linear;

            font = generator.generateFont(parameters);
        } finally {
            generator.dispose();
        }
    }

    public TextButton createEnterButton(Runnable onClick) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = font;
        style.fontColor = brown;
        style.up = createEnterBackground(false);
        style.down = createEnterBackground(true);
        style.pressedOffsetY = -6f;

        TextButton button = new TextButton("ENTRAR NO BAR", style);
        button.pad(0f,0f,8f,0f);
        button.setBounds(710f, 250f, 500f,120f);

        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onClick.run();
            }
        });
        return button;
    }

    public ImageButton createSettingsButton(Runnable onClick) {
        Texture icon = new Texture(Gdx.files.internal("ui/icons/engrenagem.png"));

        icon.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        textures.add(icon);

        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.up = createCircleBackground("F0DCB2");
        style.down = createCircleBackground("DFC795");
        style.imageUp = new TextureRegionDrawable(new TextureRegion(icon));
        style.pressedOffsetY = -2f;

        ImageButton button = new ImageButton(style);
        button.getImageCell().size(56f, 56f);
        button.setBounds(1748f,920f,112f,112f);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onClick.run();
            }
        });
        return button;
    }

    private TextureRegionDrawable createEnterBackground(boolean pressed) {
        Pixmap pixmap = new Pixmap(500,120, Pixmap.Format.RGBA8888);
        pixmap.setColor(brown);
        fillRoundedRectangle(pixmap, 0, 8, 500, 112,18);

        int top = pressed ? 6 : 0;

        fillRoundedRectangle(pixmap, 0, top, 500, 112, 18);
        pixmap.setColor(Color.valueOf("F0B639"));
        fillRoundedRectangle(pixmap, 5, top + 5, 490, 102, 13);
        return createDrawable(pixmap);
    }

    private TextureRegionDrawable createCircleBackground(String fill) {
        Pixmap pixmap = new Pixmap(112,112, Pixmap.Format.RGBA8888);
        pixmap.setColor(brown);
        pixmap.fillCircle(56,56,55);
        pixmap.setColor(Color.valueOf(fill));
        pixmap.fillCircle(56,56,51);

        return createDrawable(pixmap);
    }

    private void fillRoundedRectangle(Pixmap pixmap, int x, int y, int width, int height, int radius) {
        pixmap.fillRectangle(x + radius, y, width - radius * 2 ,height);
        pixmap.fillRectangle(x, y + radius, width, height - radius * 2);

        pixmap.fillCircle(x + radius, y + radius, radius);
        pixmap.fillCircle(x + width - radius - 1, y + radius, radius);
        pixmap.fillCircle(x + radius, y + height - radius - 1, radius);
        pixmap.fillCircle(x + width - radius - 1, y + height - radius - 1, radius);
    }

    private TextureRegionDrawable createDrawable(Pixmap pixmap) {
        Texture texture;

        try {
            texture = new Texture(pixmap);
        } finally {
            pixmap.dispose();
        }

        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        textures.add(texture);
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    @Override
    public void dispose() {
        font.dispose();
        for (Texture texture : textures) {
            texture.dispose();
        }
        textures.clear();
    }
}
