package br.com.dominando.frontend;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import br.com.dominando.frontend.settings.GameSettings;

import br.com.dominando.frontend.screens.OpeningScreen;
import br.com.dominando.frontend.screens.PairingScreen;
import br.com.dominando.frontend.screens.WaitingScreen;
import br.com.dominando.frontend.screens.MatchIntroScreen;
import br.com.dominando.frontend.screens.MatchScreen;

public class DesceUmaGame extends Game {

    private OpeningScreen openingScreen;
    private PairingScreen pairingScreen;
    private WaitingScreen waitingScreen;
    private MatchIntroScreen matchIntroScreen;
    private MatchScreen matchScreen;

    private GameSettings gameSettings;
    private Music menuMusic;
    private boolean menuMusicStarted;

    @Override
    public void create() {
        gameSettings = new GameSettings();
        menuMusic = Gdx.audio.newMusic(Gdx.files.internal("audio/samba-menu-loop.ogg"));
        menuMusic.setLooping(true);
        openingScreen = new OpeningScreen(() -> Gdx.app.postRunnable(this::showPairing));
        setScreen(openingScreen);
    }

    private void showPairing() {
        if (pairingScreen == null) {
            pairingScreen = new PairingScreen(() -> Gdx.app.postRunnable(this::showOpening), () -> Gdx.app.postRunnable(this::showWaitingDemo));
        }
        setScreen(pairingScreen);
    }

    private void showWaitingDemo() {
        if (waitingScreen == null) {
            waitingScreen = new WaitingScreen("51A7", () -> Gdx.app.postRunnable(this::showPairing), () -> Gdx.app.postRunnable(this::showMatchIntroDemo));
        }
        setScreen(waitingScreen);
    }

    private void showOpening() {
        setScreen(openingScreen);
    }

    private void showMatchIntroDemo() {
       if (matchScreen == null) {
           matchScreen = new MatchScreen(() -> Gdx.app.postRunnable(this::leaveMatch));
       }

       if (matchIntroScreen == null) {
           matchIntroScreen = new MatchIntroScreen(gameSettings, () -> Gdx.app.postRunnable(this::showMatchDemo));
       }
       setScreen(matchIntroScreen);
    }
    private void showMatchDemo() {
        setScreen(matchScreen);
    }

    private void leaveMatch() {
        if (matchScreen == null || getScreen() != matchScreen) {
            return;
        }

        MatchScreen previousMatch = matchScreen;
        showPairing();
        matchScreen = null;
        previousMatch.dispose();
    }

    @Override
    public void render() {
        super.render();
        updateMenuMusic();
    }

    private boolean audioInForeground = true;

    public void setAudioInForeground(boolean foreground) {
        audioInForeground = foreground;

        if (!foreground && menuMusic != null && menuMusic.isPlaying()) {
            menuMusic.pause();
        }
    }

    private void updateMenuMusic() {
        Screen currentScreen = getScreen();
        float level = 0f;

        if (currentScreen == openingScreen
            || currentScreen == pairingScreen
            || currentScreen == waitingScreen) {
            level = 1f;
        } else if (currentScreen instanceof MatchIntroScreen) {
            MatchIntroScreen intro = (MatchIntroScreen) currentScreen;
            level = 1f - intro.getEntranceFade();
        }

        if (level <= 0f) {
            if (menuMusicStarted) {
                menuMusic.stop();
                menuMusicStarted = false;
            }
            return;
        }

        if (!audioInForeground || !gameSettings.isMusicEnabled()) {
            if (menuMusic.isPlaying()) {
                menuMusic.pause();
            }
            return;
        }

        menuMusic.setVolume((gameSettings.getMusicVolume() / 100f) * level);
        if (!menuMusic.isPlaying()) {
            menuMusic.play();
            menuMusicStarted = true;
        }
    }

    @Override
    public void dispose() {
        setScreen(null);
        menuMusic.dispose();

        if (matchIntroScreen != null) {
            matchIntroScreen.dispose();
        }

        if (waitingScreen != null) {
            waitingScreen.dispose();
        }

        if (pairingScreen != null) {
            pairingScreen.dispose();
        }

        if (openingScreen != null) {
            openingScreen.dispose();
        }

        if (matchScreen != null) {
            matchScreen.dispose();
        }
    }
}
