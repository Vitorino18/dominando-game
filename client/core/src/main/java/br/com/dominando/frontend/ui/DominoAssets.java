package br.com.dominando.frontend.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

public class DominoAssets implements Disposable {
    private final Array<Texture> textures = new Array<>();
    private final Drawable front;
    private final Drawable back;
    private final Drawable[] values = new Drawable[7];

    public DominoAssets() {
        front = load("pedra-base.png");
        back = load ("pedra-verso.png");

        for (int value = 1; value <= 6; value++) {
            values[value] = load("valor-" + value + ".png");
        }
    }

    private Drawable load(String filename) {
        Texture texture = new Texture(Gdx.files.internal("ui/tiles/" + filename));
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        textures.add(texture);

        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    public Drawable getFront() {
        return front;
    }
    public Drawable getBack() {
        return back;
    }

    public Drawable getValue(int value) {
        if (value < 0 || value > 6) {
            throw new IllegalArgumentException("O valor visual deve estar entre 0 e 6.");
        }

        return values[value];
    }

    @Override
    public void dispose() {
        for (Texture texture : textures) {
            texture.dispose();
        }
        textures.clear();
    }
}
