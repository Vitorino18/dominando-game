package br.com.dominando.frontend.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;

import java.util.Locale;

public class PlayerInfoView extends Group implements Disposable {
    public static final float WIDTH = 328f;
    public static final float BODY_HEIGHT = 112f;
    public static final float SHADOW_HEIGHT = 4f;
    public static final float TOTAL_HEIGHT = BODY_HEIGHT + SHADOW_HEIGHT;

    private final boolean avatarOnRight;

    private final Texture backgroundTexture;
    private final Texture normalAvatarTexture;
    private final Texture activeAvatarTexture;

    private final BitmapFont initialFont;
    private final BitmapFont nameFont;
    private final BitmapFont statusFont;

    private final Image avatar;
    private final Label initialLabel;
    private final Label nameLabel;
    private final Label statusLabel;

    private int displayedCount;
    private boolean activeTurn;

    public PlayerInfoView(boolean avatarOnRight, String playerName, int displayedCount) {
        this.avatarOnRight = avatarOnRight;
        setSize(WIDTH, TOTAL_HEIGHT);
        setTouchable(Touchable.disabled);

        backgroundTexture = createBackground();
        normalAvatarTexture = loadTexture("avatar-normal.png");
        activeAvatarTexture = loadTexture("avatar-turno.png");

        initialFont = createFont(32);
        nameFont = createFont(24);
        statusFont = createFont(20);

        Image background = new Image(backgroundTexture);
        background.setBounds(0f, 0f, WIDTH, TOTAL_HEIGHT);
        addActor(background);

        float avatarX = avatarOnRight ? 228f : 12f;
        float textX = avatarOnRight ? 12f : 112f;
        int textAlignment = avatarOnRight ? Align.right : Align.left;

        avatar = new Image(normalAvatarTexture);
        place(avatar, avatarX, 12f, 88f, 88f);
        addActor(avatar);

        initialLabel = new Label("", new Label.LabelStyle(initialFont, Color.valueOf("f0dcb2")));
        initialLabel.setAlignment(Align.center);
        place(initialLabel, avatarX, 12f, 88f, 88f);
        addActor(initialLabel);

        nameLabel = new Label("", new Label.LabelStyle(nameFont, Color.valueOf("234730")));
        nameLabel.setAlignment(textAlignment);
        nameLabel.setEllipsis(true);
        place(nameLabel, textX, 24f, 204f, 32f);
        addActor(nameLabel);

        statusLabel = new Label("", new Label.LabelStyle(statusFont, Color.valueOf("3f2412")));
        statusLabel.setAlignment(textAlignment);
        place(statusLabel, textX, 60f, 204f, 28f);
        addActor(statusLabel);

        setPlayerName(playerName);
        setDisplayedCount(displayedCount);
        setActiveTurn(false);
    }

    public void setPlayerName(String name) {
        String displayedName = name == null ? "" : name.trim();

        if (displayedName.isEmpty()) {
            displayedName = "Jogador";
        }

        nameLabel.setText(displayedName);

        int firstCharacter = displayedName.codePointAt(0);
        String initial = new String(Character.toChars(firstCharacter));

        initialLabel.setText(initial.toUpperCase(Locale.ROOT));
    }

    public void setDisplayedCount(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("A quantidade de pedras nao pode ser negativa");
        }

        displayedCount = count;
        refreshStatus();
    }

    public void setActiveTurn(boolean active) {
        activeTurn = active;
        Texture selectedTexture = active ? activeAvatarTexture : normalAvatarTexture;
        avatar.setDrawable(new TextureRegionDrawable(new TextureRegion(selectedTexture)));
        refreshStatus();
    }

    private void refreshStatus() {
        if (avatarOnRight && activeTurn) {
            statusLabel.setText("Sua Vez");
            return;
        }

        String suffix = displayedCount == 1 ? "pedra" : "pedras";
        statusLabel.setText(displayedCount + suffix);
    }

    private void place(com.badlogic.gdx.scenes.scene2d.Actor actor, float x, float top, float width, float height) {
        actor.setBounds(x, TOTAL_HEIGHT - top - height, width, height);
        actor.setTouchable(Touchable.disabled);
    }

    private Texture loadTexture(String filename) {
        Texture texture = new Texture(Gdx.files.internal("ui/profile/" + filename));
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return texture;
    }

    private BitmapFont createFont(int size) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/NunitoSans-Regular.ttf"));

        try {
            FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.size = size;
            parameter.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "áàâãéêíóôõúüçÁÀÂÃÉÊÍÓÔÕÚÜÇ";
            parameter.minFilter = Texture.TextureFilter.Linear;
            parameter.magFilter = Texture.TextureFilter.Linear;

            return generator.generateFont(parameter);
        } finally {
            generator.dispose();
        }
    }
    private Texture createBackground() {
        Pixmap pixmap = new Pixmap(656, 232, Pixmap.Format.RGBA8888);

        try {
            pixmap.setColor(Color.valueOf("3F2412"));
            fillRounded(pixmap, 0, 8, 656, 224, 112);
            fillRounded(pixmap, 0, 0, 656, 224, 112);

            pixmap.setColor(Color.valueOf("F0DCB2"));
            fillRounded(pixmap, 6, 6, 644, 212, 106);

            Texture texture = new Texture(pixmap);
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

            return texture;
        } finally {
            pixmap.dispose();
        }
    }

    private void fillRounded(Pixmap pixmap, int x, int y, int width, int height, int radius) {
        pixmap.fillRectangle(x + radius, y, width - radius * 2, height);
        pixmap.fillRectangle(x, y + radius, width, height - radius * 2);

        pixmap.fillCircle(x + radius, y + radius, radius);
        pixmap.fillCircle(x + width - radius - 1, y + radius, radius);
        pixmap.fillCircle(x + radius, y + height - radius - 1, radius);
        pixmap.fillCircle(x + width - radius - 1, y + height - radius - 1, radius);
    }

    @Override
    public void dispose() {
        backgroundTexture.dispose();
        normalAvatarTexture.dispose();
        activeAvatarTexture.dispose();
        initialFont.dispose();
        nameFont.dispose();
        statusFont.dispose();
    }
}
