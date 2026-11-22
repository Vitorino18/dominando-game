package br.com.dominando.frontend.audio;

import br.com.dominando.frontend.settings.GameSettings;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Controle dos efeitos e da ambiencia; a trilha do menu continua independente. */
public final class GameAudio {
    private static final List<Sound> sounds = new ArrayList<>();
    private static final List<Playback> playbacks = new ArrayList<>();
    private static final Map<Music, Float> musicVolumes = new IdentityHashMap<>();
    private static final List<Music> pausedMusic = new ArrayList<>();
    private static GameSettings settings;
    private static boolean foreground = true;
    private static float previousVolume = -1f;

    private GameAudio() {}

    public static void init(GameSettings gameSettings) {
        settings = gameSettings;
        foreground = true;
        previousVolume = -1f;
    }

    public static Sound newSound(FileHandle file) {
        Sound sound = Gdx.audio.newSound(file);
        sounds.add(sound);
        return sound;
    }

    public static Music newMusic(FileHandle file) {
        Music music = Gdx.audio.newMusic(file);
        musicVolumes.put(music, 1f);
        return music;
    }

    private static float volume() {
        return settings != null && settings.isSoundEffectsEnabled()
            ? settings.getSoundEffectsVolume() / 100f : 0f;
    }

    private static boolean canPlay() {
        return foreground && volume() > 0f;
    }

    public static long play(Sound sound, float level) {
        if (!canPlay()) return -1L;
        float gain = Math.max(0f, Math.min(1f, level));
        long id = sound.play(gain * volume());
        if (id >= 0L) {
            if (playbacks.size() >= 128) playbacks.remove(0);
            playbacks.add(new Playback(sound, id, gain));
        }
        return id;
    }

    public static void play(Music music) {
        if (canPlay()) music.play();
    }

    public static void setVolume(Music music, float level) {
        float gain = Math.max(0f, Math.min(1f, level));
        musicVolumes.put(music, gain);
        music.setVolume(gain * volume());
    }

    public static void stop(Music music) {
        pausedMusic.remove(music);
        music.stop();
    }

    private static void stopSounds() {
        for (Sound sound : sounds) sound.stop();
        playbacks.clear();
    }

    public static void update() {
        if (settings == null) return;
        float currentVolume = volume();
        if (currentVolume == previousVolume) return;

        if (currentVolume <= 0f) {
            stopSounds();
            for (Music music : musicVolumes.keySet()) music.stop();
            pausedMusic.clear();
        } else {
            for (Playback playback : playbacks) {
                playback.sound.setVolume(playback.id, playback.gain * currentVolume);
            }
            for (Map.Entry<Music, Float> entry : musicVolumes.entrySet()) {
                entry.getKey().setVolume(entry.getValue() * currentVolume);
            }
        }
        previousVolume = currentVolume;
    }

    public static void setForeground(boolean active) {
        if (foreground == active) return;
        foreground = active;
        if (!active) {
            stopSounds();
            for (Music music : musicVolumes.keySet()) {
                if (music.isPlaying()) {
                    pausedMusic.add(music);
                    music.pause();
                }
            }
        } else {
            update();
            if (canPlay()) {
                for (Music music : pausedMusic) music.play();
            }
            pausedMusic.clear();
        }
    }

    public static void dispose(Sound sound) {
        sounds.remove(sound);
        for (int i = playbacks.size() - 1; i >= 0; i--) {
            if (playbacks.get(i).sound == sound) playbacks.remove(i);
        }
        sound.dispose();
    }

    public static void dispose(Music music) {
        musicVolumes.remove(music);
        pausedMusic.remove(music);
        music.dispose();
    }

    public static void dispose() {
        for (Sound sound : sounds) sound.dispose();
        for (Music music : musicVolumes.keySet()) music.dispose();
        sounds.clear();
        playbacks.clear();
        musicVolumes.clear();
        pausedMusic.clear();
        settings = null;
    }

    private static final class Playback {
        final Sound sound;
        final long id;
        final float gain;
        Playback(Sound sound, long id, float gain) {
            this.sound = sound;
            this.id = id;
            this.gain = gain;
        }
    }
}
