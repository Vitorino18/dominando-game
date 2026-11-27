package br.com.dominando.frontend.ui;

import br.com.dominando.frontend.audio.GameAudio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Disposable;

public final class TableImpact implements Disposable {
    private final Sound knock;

    private float shakeTime;
    private float shakeDuration;
    private float shakeForce;

    public TableImpact() {
        knock = GameAudio.newSound(
            Gdx.files.internal("audio/batida-mesa.mp3")
        );
    }

    public void hit(int stonesRemaining) {
        float excitement = Math.max(
            0f, Math.min(1f, (6f - stonesRemaining) / 5f)
        );
        excitement *= excitement;

        shakeTime = 0f;
        shakeDuration = 0.14f + 0.08f * excitement;
        shakeForce = 1f + 5f * excitement;

        long id = GameAudio.play(
            knock, 0.30f + 0.60f * excitement
        );

        if (id >= 0L) {
            knock.setPitch(id, 0.98f - 0.10f * excitement);
        }
    }

    public void draw(
        Stage stage, Actor table, Actor stones,
        float delta, boolean active
    ) {
        if (!active) {
            stop();
        } else {
            shakeTime += Math.max(0f, delta);
        }

        float offsetX = 0f;
        float offsetY = 0f;

        if (active && shakeTime < shakeDuration) {
            float decay = 1f - shakeTime / shakeDuration;
            float force = shakeForce * decay * decay;

            offsetX = (float) Math.sin(shakeTime * 110f)
                * force * 0.35f;
            offsetY = (float) Math.sin(shakeTime * 145f)
                * force;
        }

        float tableX = table.getX();
        float tableY = table.getY();
        float stonesX = stones.getX();
        float stonesY = stones.getY();

        try {
            table.setPosition(tableX + offsetX, tableY + offsetY);
            stones.setPosition(stonesX + offsetX, stonesY + offsetY);
            stage.draw();
        } finally {
            table.setPosition(tableX, tableY);
            stones.setPosition(stonesX, stonesY);
        }
    }

    public void stop() {
        shakeTime = 0f;
        shakeDuration = 0f;
        knock.stop();
    }

    @Override
    public void dispose() {
        stop();
        GameAudio.dispose(knock);
    }
}
