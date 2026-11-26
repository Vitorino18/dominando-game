package br.com.dominando.frontend.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Disposable;

public final class ShopAudio implements Disposable {

    private final Sound whistle;
    private final Sound tick;
    private final Sound tock;
    private final Sound murmur;

    private boolean nextIsTock;
    private boolean murmurPlayed;
    private float nextTickAtRemaining;
    private long murmurUntilNanos;

    public ShopAudio() {
        whistle = load("assovio-loja.wav");
        tick = load("relogio-tic.wav");
        tock = load("relogio-tac.wav");
        murmur = load("garcom-murmurio.wav");
    }

    private Sound load(String name) {
        return GameAudio.newSound(
            Gdx.files.internal("audio/" + name)
        );
    }

    public void open(float secondsRemaining) {
        stop();

        nextTickAtRemaining =
            Math.min(5f, secondsRemaining);

        // Evita repetir a chamada ao entrar perto do fim.
        murmurPlayed = secondsRemaining <= 2f;

        if (secondsRemaining > 5f) {
            GameAudio.play(whistle, 0.65f);
        }
    }

    public void update(float remaining, boolean reconnecting) {
        if (reconnecting || remaining <= 0f) {
            tick.stop();
            tock.stop();
            whistle.stop();
            murmur.stop();
            murmurUntilNanos = 0L;
            nextTickAtRemaining = remaining;
            return;
        }

        if (!murmurPlayed && remaining <= 2f) {
            murmurPlayed = true;

            if (remaining >= 1.7f) {
                long id = GameAudio.play(murmur, 0.85f);

                if (id >= 0L) {
                    murmurUntilNanos =
                        System.nanoTime() + 1_800_000_000L;
                }
            }
        }

        if (remaining > 5f
            || remaining > nextTickAtRemaining) {
            return;
        }

        float progress = Math.max(
            0f,
            Math.min(1f, (5f - remaining) / 5f)
        );

        float interval = 0.58f
            - 0.42f * (float) Math.pow(progress, 1.2);

        float volume = isMurmuring() ? 0.40f : 0.70f;

        GameAudio.play(nextIsTock ? tock : tick, volume);
        nextIsTock = !nextIsTock;

        // Agenda a próxima batida sem acumular sons atrasados.
        nextTickAtRemaining = remaining - interval;
    }

    private boolean isMurmuring() {
        return System.nanoTime() < murmurUntilNanos;
    }

    public float getAmbienceMultiplier(float remaining) {
        if (isMurmuring()) {
            return 0.35f;
        }

        return remaining <= 5f ? 0.65f : 1f;
    }

    public void stop() {
        whistle.stop();
        tick.stop();
        tock.stop();
        murmur.stop();

        nextIsTock = false;
        murmurPlayed = false;
        nextTickAtRemaining = 5f;
        murmurUntilNanos = 0L;
    }

    @Override
    public void dispose() {
        stop();
        GameAudio.dispose(whistle);
        GameAudio.dispose(tick);
        GameAudio.dispose(tock);
        GameAudio.dispose(murmur);
    }
}
