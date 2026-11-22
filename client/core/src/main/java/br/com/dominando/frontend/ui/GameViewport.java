package br.com.dominando.frontend.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

public class GameViewport extends ExtendViewport {

    private final float designWidth;
    private final float designHeight;

    public GameViewport(float width, float height) {
        super(width, height);
        designWidth = width;
        designHeight = height;
    }

    @Override
    public void update(int width, int height, boolean centerCamera) {
        if (width <= 0 || height <= 0) {
            return;
        }

        super.update(width, height, false);

        getCamera().position.set(designWidth / 2f, designHeight / 2f, 0f);
        getCamera().update();
    }

    public float getLeft() {
        return (designWidth - getWorldWidth()) / 2f;
    }

    public float getBottom() {
        return (designHeight - getWorldHeight()) / 2f;
    }

    public void cover(Actor actor) {
        actor.setBounds(getLeft(), getBottom(), getWorldWidth(), getWorldHeight()
        );
    }
}
