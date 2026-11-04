package br.com.dominando.frontend.settings;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class GameSettings {
    private static final String FILE_NAME = "desce-uma-settings";
    private static final String MUSIC = "music";
    private static final String SOUND_EFFECTS = "sound-effects";
    private static final String VIBRATION = "vibration";
    private final Preferences preferences;

    public GameSettings() {
        preferences = Gdx.app.getPreferences(FILE_NAME);
    }

    public boolean isMusicEnabled() {
        return preferences.getBoolean(MUSIC, true);
    }
    public void setMusicEnabled(boolean enabled) {
        preferences.putBoolean(MUSIC, enabled);
        preferences.flush();
    }

    public boolean isSoundEffectsEnabled() {
        return preferences.getBoolean(SOUND_EFFECTS, true);
    }
    public void setSoundEffectsEnabled(boolean enabled) {
        preferences.putBoolean(SOUND_EFFECTS, enabled);
        preferences.flush();
    }

    public boolean isVibrationEnabled() {
        return preferences.getBoolean(VIBRATION, true);
    }
    public void setVibrationEnabled(boolean enabled) {
        preferences.putBoolean(VIBRATION, enabled);
        preferences.flush();
    }
}
