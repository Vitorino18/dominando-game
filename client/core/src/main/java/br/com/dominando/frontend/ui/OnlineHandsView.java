package br.com.dominando.frontend.ui;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;

import br.com.dominando.grpc.PecaMensagem;

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

    public OnlineHandsView(DominoAssets assets, float designWidth, float designHeight, float humanY, float opponentY) {
        this.assets = assets;
        this.designWidth = designWidth;
        this.humanY = humanY;
        this.opponentY = opponentY;

        setSize(designWidth, designHeight);
        setTouchable(Touchable.disabled);
    }

    public void setHands(List<PecaMensagem> hand, int opponentCount) {
        if (opponentCount < 0) {
            throw new IllegalArgumentException("A quantidade de peças não pode ser negativa.");
        }

        if (!previousHand.equals(hand)) {
            clearTiles(humanTiles);

            for (PecaMensagem piece : hand) {
                DominoTile tile = new DominoTile(assets, piece.getLadoA(), piece.getLadoB());

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

                opponentTiles.add(tile);
                addActor(tile);
            }

            layoutTiles(opponentTiles, false);
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
