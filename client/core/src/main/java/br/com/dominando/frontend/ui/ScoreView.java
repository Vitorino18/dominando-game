package br.com.dominando.frontend.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Disposable;

public class ScoreView extends Group implements Disposable {
    private static final float BASE_WIDTH = 88f;
    private static final float BASE_HEIGHT = 72f;
    private static final float[][] POSITIONS = {
        {8f, 32f},
        {30f, 36f},
        {52f, 32f},
        {19f, 14f},
        {41f, 14f}
    };

    private final Texture texture;
    private final Image[] caps = new Image[5];

    public ScoreView(boolean green, int displayedScore) {
        String filename = green ? "tampinha-verde.png" : "tampinha-amarela.png";
        texture = new Texture(Gdx.files.internal("ui/score/" + filename));
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        setTouchable(Touchable.disabled);

        for (int i = 0; i < caps.length; i++) {
            caps[i] = new Image(texture);
            caps[i].setTouchable(Touchable.disabled);
            addActor(caps[i]);
        }

        setSize(BASE_WIDTH, BASE_HEIGHT);
        setDisplayedScore(displayedScore);
    }

    public void setDisplayedScore(int score) {
        if (score < 0 || score > 5) {
            throw new IllegalArgumentException("O placar visual deve estar entre 0 e 5");

        }
        for (int i = 0; i < caps.length; i++) {
            caps[i].setVisible(i < score);
        }
    }

    @Override
    public void sizeChanged() {
        super.sizeChanged();
        if (texture == null) {
            return;
        }

        float scaleX = getWidth() / BASE_WIDTH;
        float scaleY = getHeight() / BASE_HEIGHT;
        float capWidth = 28f;
        float capHeight = capWidth * texture.getHeight() / (float)  texture.getWidth();

        for (int i = 0; i < caps.length; i++) {
            if (caps[i] == null) {
                continue;
            }

            float x = POSITIONS[i][0];
            float top = POSITIONS[i][1];

            caps[i].setBounds(x * scaleX, (BASE_HEIGHT - top - capHeight) * scaleY, capWidth * scaleX, capHeight * scaleY
            );
        }
    }

    @Override
    public void dispose() {
        texture.dispose();
    }
}
