package br.com.dominando.frontend;

import com.badlogic.gdx.Game;
import br.com.dominando.frontend.screens.OpeningScreen;

public class DesceUmaGame extends Game {

    @Override
    public void create() {
        setScreen(new OpeningScreen());
    }

    @Override
    public void dispose() {
        super.dispose();

        if (getScreen() != null) {
            getScreen().dispose();
        }
    }
}
