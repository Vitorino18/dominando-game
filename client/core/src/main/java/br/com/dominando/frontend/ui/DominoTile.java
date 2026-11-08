package br.com.dominando.frontend.ui;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Scaling;

public class DominoTile extends Group {
    private final DominoAssets assets;
    private final Image body = new Image();
    private final Image upperValue = new Image();
    private final Image lowerValue = new Image();

    private int upper;
    private int lower;
    private  boolean faceDown;

    public DominoTile(DominoAssets assets, int upper, int lower) {
        this.assets = assets;

        body.setScaling(Scaling.stretch);
        upperValue.setScaling(Scaling.stretch);
        lowerValue.setScaling(Scaling.stretch);

        body.setTouchable(Touchable.disabled);
        upperValue.setTouchable(Touchable.disabled);
        lowerValue.setTouchable(Touchable.disabled);

        addActor(body);
        addActor(upperValue);
        addActor(lowerValue);

        setSize(88f, 164f);
        setValues(upper, lower);
    }

    public void setValues(int upper, int lower){
        if (upper < 0 || upper > 6 || lower < 0 || lower > 6) {
            throw new IllegalArgumentException("o valor visual deve estar entre 0 e 6");
        }
        this.upper = upper;
        this.lower = lower;

        upperValue.setDrawable(assets.getValue(upper));
        lowerValue.setDrawable(assets.getValue(lower));

        updateAppearance();
    }

    public void setFaceDown(boolean faceDown) {
        this.faceDown = faceDown;
        updateAppearance();
    }

    private void updateAppearance() {
        body.setDrawable(faceDown ? assets.getBack() : assets.getFront());
        upperValue.setVisible(!faceDown && upper != 0);
        lowerValue.setVisible(!faceDown && lower != 0);
    }

    @Override
    protected void sizeChanged() {
        super.sizeChanged();

        float sx = getWidth() / 88f;
        float sy = getHeight() / 164f;

        body.setBounds(0f, 0f, getWidth(), getHeight());

        upperValue.setBounds(16f * sx, 84f * sy, 56f * sx, 56f * sy);
        lowerValue.setBounds(16f * sx, 24f * sy, 56f * sx, 56f * sy);

        setOrigin(getWidth() / 2f, getHeight() / 2f);
    }
}
