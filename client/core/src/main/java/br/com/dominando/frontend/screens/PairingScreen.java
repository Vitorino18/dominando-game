package br.com.dominando.frontend.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class PairingScreen extends ScreenAdapter {
    private static final float WIDTH = 1920f;
    private static final float HEIGHT = 1080f;
    private final Color background = Color.valueOf("f0dcb2");
    private final Color brown = Color.valueOf("3f2412");
    private final Stage stage;
    private final BitmapFont font;

    public PairingScreen(Runnable onBack) {
        stage = new Stage(new FitViewport(WIDTH, HEIGHT));
        font = createFont();
        Table content = new Table();
        content.setFillParent(true);
        stage.addActor(content);

        Label title = new Label("ENCONTRE UMA MESA", new Label.LabelStyle(font, brown));

        content.add(title).padBottom(60f);
        content.row();
        TextButton.TextButtonStyle backStyle = new TextButton.TextButtonStyle();

        backStyle.font = font;
        backStyle.fontColor = brown;
        backStyle.downFontColor = Color.valueOf("A66A28");

        TextButton backButton = new TextButton("VOLTAR", backStyle);
        content.add(backButton).width(320f).height(96f);
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                onBack.run();
            }
        });
    }

    private BitmapFont createFont() {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/LilitaOne-Regular.ttf"));

        try {
            FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.size = 40;
            parameter.minFilter = Texture.TextureFilter.Linear;
            parameter.magFilter = Texture.TextureFilter.Linear;

            return generator.generateFont(parameter);
        } finally {
            generator.dispose();
        }
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(background);
        stage.getViewport().apply();
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void hide() {
        if (Gdx.input.getInputProcessor() == stage) {
            Gdx.input.setInputProcessor(null);
        }
    }

    @Override
    public void dispose() {
        hide();
        stage.dispose();
        font.dispose();
    }
}
