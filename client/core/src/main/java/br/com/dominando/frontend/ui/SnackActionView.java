package br.com.dominando.frontend.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import br.com.dominando.frontend.ui.IntervalView.Snack;

public final class SnackActionView extends Group implements Disposable {
    private final List<Texture> textures = new ArrayList<>();
    private final Map<Snack, TextureRegionDrawable> icons = new EnumMap<>(Snack.class);
    private final Map<Snack, TextureRegionDrawable> usedIcons = new EnumMap<>(Snack.class);
    private final TextureRegionDrawable usedEgg;
    private final Image button;
    private final Image revealGlow;
    private final Image revealTrack;
    private final Image revealProgress;
    private final Image base;

    public SnackActionView(Runnable onActivate) {
        setTouchable(Touchable.childrenOnly);
        setVisible(false);

        base = new Image(load("base-jogador.png"));
        base.setBounds(1544f, 48f, 344f, 240f);
        base.setTouchable(Touchable.disabled);
        addActor(base);

        icons.put(Snack.OVO, load("ovo.png"));
        icons.put(Snack.COXINHA, load("coxinha.png"));
        icons.put(Snack.TORRESMO, load("torresmo.png"));
        usedEgg = load("ovo-usado.png");
        usedIcons.put(Snack.COXINHA, load("coxinha-usada.png"));
        usedIcons.put(Snack.TORRESMO, load("torresmo-usado.png"));

        button = new Image(icons.get(Snack.OVO));
        button.setBounds(1756f, 156f, 120f, 120f);
        button.setTouchable(Touchable.disabled);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onActivate.run();
            }
        });
        addActor(button);

        revealGlow = new Image(createRevealGlow());
        revealGlow.setTouchable(Touchable.disabled);

        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixel.setColor(Color.WHITE);
        pixel.fill();
        TextureRegionDrawable bar = createDrawable(pixel);

        revealTrack = new Image(bar);
        revealTrack.setColor(0.20f, 0.09f, 0.04f, 0.75f);
        revealTrack.setTouchable(Touchable.disabled);
        addActor(revealTrack);

        revealProgress = new Image(bar);
        revealProgress.setColor(1f, 0.78f, 0.39f, 1f);
        revealProgress.setTouchable(Touchable.disabled);
        addActor(revealProgress);

        hideHighlight();
    }

    public void setPlayerAnchor(float x, float y) {
        base.setPosition(x, y + 4f);
        button.setPosition(x + 212f, y + 112f);
    }

    public void showSnack(Snack snack) {
        setVisible(snack != null);
        button.setColor(1f, 1f, 1f, 1f);
        if (snack != null) {
            button.setDrawable(icons.get(snack));
        }
    }

    public void setButtonEnabled(boolean enabled) {
        button.setTouchable(enabled ? Touchable.enabled : Touchable.disabled);
    }

    public void showUsedEgg() {
        button.setDrawable(usedEgg);
        setButtonEnabled(false);
    }

    public void showUsedSnack(Snack snack) {
        button.setDrawable(usedIcons.get(snack));
        setButtonEnabled(false);
    }

    public void highlightTile(Actor tile, float progress) {
        Group tileLayer = tile.getParent();
        if (revealGlow.getParent() != tileLayer) {
            tileLayer.addActorBefore(tile, revealGlow);
        }

        revealGlow.setBounds(
            tile.getX() - 24f, tile.getY() - 16f,
            tile.getWidth() + 48f, tile.getHeight() + 32f
        );
        revealGlow.getColor().a = Math.min(1f, progress * 5f);

        float width = tile.getWidth() * 0.8f;
        float x = tile.getX() + (tile.getWidth() - width) / 2f;
        float y = tile.getY() - 9f;

        revealTrack.setBounds(x, y, width, 4f);
        revealProgress.setBounds(x, y, width * progress, 4f);
        revealTrack.setVisible(true);
        revealProgress.setVisible(true);
    }

    public void hideHighlight() {
        revealGlow.remove();
        revealTrack.setVisible(false);
        revealProgress.setVisible(false);
    }

    private TextureRegionDrawable createRevealGlow() {
        int size = 96;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);

        for (int y = 0; y < size; y++) {
            float dy = (2f * y + 1f) / size - 1f;
            for (int x = 0; x < size; x++) {
                float dx = (2f * x + 1f) / size - 1f;
                float falloff = Math.max(0f, 1f - dx * dx - dy * dy);
                float alpha = 0.55f * falloff * falloff;
                pixmap.drawPixel(x, y, Color.rgba8888(1f, 0.73f, 0.30f, alpha));
            }
        }

        return createDrawable(pixmap);
    }

    private TextureRegionDrawable createDrawable(Pixmap pixmap) {
        try {
            Texture texture = new Texture(pixmap);
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            textures.add(texture);
            return new TextureRegionDrawable(new TextureRegion(texture));
        } finally {
            pixmap.dispose();
        }
    }

    private TextureRegionDrawable load(String file) {
        Texture texture = new Texture(Gdx.files.internal("ui/snack/" + file));
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        textures.add(texture);
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    @Override
    public void dispose() {
        for (Texture texture : textures) {
            texture.dispose();
        }
    }
}
