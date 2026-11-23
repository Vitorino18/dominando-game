package br.com.dominando.frontend.ui;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import br.com.dominando.grpc.PecaMensagem;

import java.util.function.IntConsumer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class OnlineHandsView extends Group {

    private final DominoAssets assets;
    private final float designWidth;
    private final float humanY;
    private final float opponentY;

    private final List<DominoTile> humanTiles = new ArrayList<>();
    private final List<DominoTile> opponentTiles = new ArrayList<>();

    private List<PecaMensagem> previousHand = Collections.emptyList();

    private IntConsumer onSelect = index -> {};
    private boolean selectionEnabled;
    private int selectedIndex = -1;

    public OnlineHandsView(DominoAssets assets, float designWidth, float designHeight, float humanY, float opponentY) {
        this.assets = assets;
        this.designWidth = designWidth;
        this.humanY = humanY;
        this.opponentY = opponentY;

        setSize(designWidth, designHeight);
        setTouchable(Touchable.childrenOnly);
    }

    public void setHands(List<PecaMensagem> hand, int opponentCount) {
        if (opponentCount < 0) {
            throw new IllegalArgumentException("A quantidade de peças não pode ser negativa.");
        }

        if (!previousHand.equals(hand)) {
            selectedIndex = -1;
            clearTiles(humanTiles);

            for (PecaMensagem piece : hand) {
                DominoTile tile = new DominoTile(assets, piece.getLadoA(), piece.getLadoB());

                final int index = humanTiles.size();

                tile.setTouchable(selectionEnabled ? Touchable.enabled : Touchable.disabled);
                tile.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        if (selectionEnabled && getTile(true, index) == tile) {
                            onSelect.accept(index);
                        }
                    }
                });

                humanTiles.add(tile);
                addActor(tile);
            }

            previousHand = new ArrayList<>(hand);
            layoutTiles(humanTiles, true);
        }

        if (opponentTiles.size() != opponentCount) {
            clearTiles(opponentTiles);

            for (int i = 0; i < opponentCount; i++) {
                DominoTile tile = new DominoTile(assets, 0, 0);
                tile.setFaceDown(true);

                tile.setTouchable(Touchable.disabled);

                opponentTiles.add(tile);
                addActor(tile);
            }

            layoutTiles(opponentTiles, false);
        }
    }

    public void setOnSelect(IntConsumer onSelect) {
        this.onSelect = onSelect;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int index) {
        int validIndex =
            index >= 0 && index < humanTiles.size() ? index : -1;

        if (selectedIndex == validIndex) {
            return;
        }

        selectedIndex = validIndex;

        for (int i = 0; i < humanTiles.size(); i++) {
            humanTiles.get(i).setSelected(i == selectedIndex);
        }
    }

    public void setSelectionEnabled(boolean enabled) {
        selectionEnabled = enabled;

        for (DominoTile tile : humanTiles) {
            tile.setTouchable(
                enabled ? Touchable.enabled : Touchable.disabled
            );
        }

        if (!enabled) {
            setSelectedIndex(-1);
        }
    }

    public DominoTile getTile(boolean human, int index) {
        List<DominoTile> tiles = human ? humanTiles : opponentTiles;

        if (index < 0 || index >= tiles.size()) {
            return null;
        }

        return tiles.get(index);
    }

    private void clearTiles(List<DominoTile> tiles) {
        for (DominoTile tile : tiles) {
            tile.remove();
        }

        tiles.clear();
    }

    private void layoutTiles(List<DominoTile> tiles, boolean human) {
        if (tiles.isEmpty()) {
            return;
        }

        float tileWidth = human ? 88f : 44f;
        float tileHeight = human ? 164f : 82f;
        float gap = human ? 12f : 10f;
        float availableWidth = human ? 720f : 480f;
        float centerY = human ? humanY : opponentY;

        if (human && tiles.size() > 1) {
            float fittingGap = (availableWidth - tiles.size() * tileWidth) / (tiles.size() - 1);
            gap = Math.max(-10f, Math.min(gap, fittingGap));
        }

        float totalWidth = tiles.size() * tileWidth + (tiles.size() - 1) * gap;

        float scale = Math.min(1f, availableWidth / totalWidth);
        float startX = (designWidth - totalWidth * scale) / 2f;

        for (int i = 0; i < tiles.size(); i++) {
            DominoTile tile = tiles.get(i);

            float x = startX + i * (tileWidth + gap) * scale;
            float y = centerY - tileHeight * scale / 2f;

            tile.setBounds(x, y, tileWidth * scale, tileHeight * scale);
        }
    }
}
