package br.com.dominando.frontend;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;

import br.com.dominando.frontend.screens.OpeningScreen;
import br.com.dominando.frontend.screens.PairingScreen;
import br.com.dominando.frontend.screens.WaitingScreen;

public class DesceUmaGame extends Game {

    private OpeningScreen openingScreen;
    private PairingScreen pairingScreen;
    private WaitingScreen waitingScreen;

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
            waitingScreen = new WaitingScreen("51A7", () -> Gdx.app.postRunnable(this::showPairing));
        }
        setScreen(waitingScreen);
    }

    private void showOpening() {
        setScreen(openingScreen);
    }

    @Override
    public void dispose() {
        setScreen(null);

        if (waitingScreen != null) {
            waitingScreen.dispose();
        }

        if (pairingScreen != null) {
            pairingScreen.dispose();
        }

        if (openingScreen != null) {
            openingScreen.dispose();
        }
    }
}
