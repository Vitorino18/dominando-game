package br.com.dominando.frontend;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;

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

    @Override
    public void create() {
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
           matchScreen = new MatchScreen();
       }

       if (matchIntroScreen == null) {
           matchIntroScreen = new MatchIntroScreen(() -> Gdx.app.postRunnable(this::showMatchDemo));
       }
       setScreen(matchIntroScreen);
    }
    private void showMatchDemo() {
        setScreen(matchScreen);
    }

    @Override
    public void dispose() {
        setScreen(null);

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
