package br.com.dominando.frontend.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Scaling;

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
            caps[i].setScaling(Scaling.fit);
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
        float scale = Math.min(scaleX, scaleY);
        float capWidth = 28f;
        float capHeight = capWidth * texture.getHeight() / (float)  texture.getWidth();

        for (int i = 0; i < caps.length; i++) {
            if (caps[i] == null) {
                continue;
            }

            float x = POSITIONS[i][0];
            float top = POSITIONS[i][1];

            float centerX = (x + capWidth / 2f) * scaleX;
            float centerY = (BASE_HEIGHT - top - capHeight / 2f) * scaleY;
            float width = capWidth * scale;
            float height = capHeight * scale;
            caps[i].setBounds(centerX - width / 2f, centerY - height / 2f, width, height);
            caps[i].setOrigin(width / 2f, height / 2f);
        }
    }

    @Override
    public void setRotation(float degrees) {
        super.setRotation(degrees);
        for (Image cap : caps) {
            cap.setRotation(-degrees);
        }
    }

    @Override
    public void dispose() {
        texture.dispose();
    }
}
