package br.com.dominando.frontend.audio;

import br.com.dominando.frontend.settings.GameSettings;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.utils.Disposable;

import java.util.Random;

public final class TableAmbience implements Disposable {

    private static final float[] DURATIONS = {
        4.70f, 6.56f, 10f
    };

    private static final float[] LEVELS = {
        0.30f, 0.30f, 0.14f
    };

    private static final int[] ORDER = {
        0, 2, 1, 2
    };

    private final GameSettings settings;
    private final Random random = new Random();
    private final Music[] clips;

    private int nextSlot;
    private int currentIndex = -1;

    private float remainingSilence = 8f;
    private float elapsed;
    private float duck = 1f;

    public TableAmbience(GameSettings settings) {
        this.settings = settings;

        clips = new Music[] {
            Gdx.audio.newMusic(Gdx.files.internal("audio/cuica-01.wav")),
            Gdx.audio.newMusic(Gdx.files.internal("audio/cuica-02.wav")),
            Gdx.audio.newMusic(Gdx.files.internal("audio/ambiente-bar.ogg"))
        };

        for (Music clip : clips) {
            clip.setLooping(false);
        }
    }

    public void update(float delta, boolean active) {
        if (!active || !settings.isMusicEnabled() || settings.getMusicVolume() <= 0) {
            stop();
            return;
        }

        float dt = Math.max(0f, delta);

        if (currentIndex == -1) {
            remainingSilence -= dt;

            if (remainingSilence <= 0f && !GameAudio.wasEffectPlayedRecently(1.2f)) {
                currentIndex = ORDER[nextSlot];
                nextSlot = (nextSlot + 1) % ORDER.length;
                elapsed = 0f;
                duck = 1f;

                Music clip = clips[currentIndex];
                clip.setVolume(0f);
                clip.play();
            }

            return;
        }

        Music clip = clips[currentIndex];
        elapsed += dt;

        if (!clip.isPlaying() || elapsed >= DURATIONS[currentIndex]) {
            finishClip();
            return;
        }

        boolean recentEffect = GameAudio.wasEffectPlayedRecently(1.2f);
        float targetDuck = recentEffect ? 0.15f : 1f;
        float speed = recentEffect ? 18f : 3f;

        duck += (targetDuck - duck) * Math.min(1f, dt * speed);
        float fadeIn = Math.min(1f, elapsed / 0.5f);
        float fadeOut = Math.min(1f, (DURATIONS[currentIndex] - elapsed) / 0.8f);

        float volume = LEVELS[currentIndex]
                * settings.getMusicVolume() / 100f
                * fadeIn
                * Math.max(0f, fadeOut)
                * duck;

        clip.setVolume(volume);
    }

    private void finishClip() {
        clips[currentIndex].stop();
        currentIndex = -1;

        remainingSilence = 20f + random.nextFloat() * 25f;
        elapsed = 0f;
        duck = 1f;
    }

    public void stop() {
        if (currentIndex != -1) {
            clips[currentIndex].stop();
            currentIndex = -1;
        }

        remainingSilence = 8f;
        elapsed = 0f;
        duck = 1f;
    }

    @Override
    public void dispose() {
        stop();

        for (Music clip : clips) {
            clip.dispose();
        }
    }
}
