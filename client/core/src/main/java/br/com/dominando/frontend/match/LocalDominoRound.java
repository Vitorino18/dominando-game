package br.com.dominando.frontend.match;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class LocalDominoRound {
    public enum Player {
        HUMAN, OPPONENT;

        private Player other() {
            return this == HUMAN ? OPPONENT : HUMAN;
        }
    }

    public enum Side { LEFT, RIGHT }

    public static final class Stone {
        public final int first;
        public final int second;

        private Stone(int first, int second) {
            this.first = first;
            this.second = second;
        }

        public boolean isDouble() {
            return first == second;
        }

        private boolean matches(int value) {
            return first == value || second == value;
        }
    }

    public static final class Placement {
        public final Stone stone;
        public final int left;
        public final int right;

        private Placement(Stone stone, int left, int right) {
            this.stone = stone;
            this.left = left;
            this.right = right;
        }

        public static Placement fromValues(int left, int right) {
            if (left < 0 || left > 6 || right < 0 || right > 6) {
                throw new IllegalArgumentException("Os valores da peça devem estar entre 0 e 6.");
            }

            return new Placement(
                new Stone(left, right), left, right
            );
        }
    }

    private final List<Stone> humanHand = new ArrayList<>();
    private final List<Stone> opponentHand = new ArrayList<>();
    private final List<Stone> stock = new ArrayList<>();
    private final List<Placement> chain = new ArrayList<>();
    private Stone requiredOpeningStone;
    private Player turn;
    private Player winner;
    private boolean finished;
    private int consecutivePasses;

    public LocalDominoRound(Random random, Player preferredStarter) {
        for (int first = 0; first <= 6; first++) {
            for (int second = first; second <= 6; second++) {
                stock.add(new Stone(first, second));
            }
        }

        Collections.shuffle(stock, random);
        for (int i = 0; i < 7; i++) {
            humanHand.add(takeFromStock());
            opponentHand.add(takeFromStock());
        }

        if (preferredStarter != null) {
            turn = preferredStarter;
            requiredOpeningStone = highestDouble(mutableHand(preferredStarter));
        } else {
            Stone humanDouble = highestDouble(humanHand);
            Stone opponentDouble = highestDouble(opponentHand);
            boolean opponentStarts = opponentDouble != null
                && (humanDouble == null || opponentDouble.first > humanDouble.first);
            turn = opponentStarts ? Player.OPPONENT : Player.HUMAN;
            requiredOpeningStone = opponentStarts ? opponentDouble : humanDouble;
        }
    }

    public Player turn() {
        return turn;
    }

    public Stone openingStone() {
        return requiredOpeningStone;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isBlocked() {
        return consecutivePasses == 2;
    }
    public Player winner() {
        return winner;
    }

    public List<Stone> hand(Player player) {
        return Collections.unmodifiableList(mutableHand(player));
    }

    public int stockCount() {
        return stock.size();
    }

    public List<Placement> chain() {
        return Collections.unmodifiableList(chain);
    }

    public Placement preview(Stone stone, Side side) {
        if (finished || stone == null || side == null || !mutableHand(turn).contains(stone)) {
            return null;
        }

        if (chain.isEmpty()) {
            if (side != Side.LEFT || (requiredOpeningStone != null && stone != requiredOpeningStone)) {
                return null;
            }
            return new Placement(stone, stone.first, stone.second);
        }

        if (side == Side.LEFT) {
            int edge = chain.get(0).left;
            if (!stone.matches(edge)) {
                return null;
            }
            int outside = stone.first == edge ? stone.second : stone.first;
            return new Placement(stone, outside, edge);
        }

        int edge = chain.get(chain.size() - 1).right;
        if (!stone.matches(edge)) {
            return null;
        }
        int outside = stone.first == edge ? stone.second : stone.first;
        return new Placement(stone, edge, outside);
    }

    public Placement play(Stone stone, Side side) {
        Placement placement = preview(stone, side);
        if (placement == null) {
            return null;
        }

        if (side == Side.LEFT) {
            chain.add(0, placement);
        } else {
            chain.add(placement);
        }

        List<Stone> hand = mutableHand(turn);
        hand.remove(stone);
        consecutivePasses = 0;

        if (hand.isEmpty()) {
            winner = turn;
            finished = true;
        } else {
            turn = turn.other();
        }
        return placement;
    }

    public boolean canDraw() {
        return !finished && !stock.isEmpty() && !hasLegalMove();
    }

    public Stone draw() {
        if (!canDraw()) {
            return null;
        }
        Stone stone = takeFromStock();
        mutableHand(turn).add(stone);
        return stone;
    }

    public Stone swapStone(Stone offered, Random random) {
        List<Stone> hand = mutableHand(turn);
        int handIndex = hand.indexOf(offered);
        if (finished || stock.isEmpty() || handIndex < 0) {
            return null;
        }

        int stockIndex = random.nextInt(stock.size());
        Stone replacement = stock.set(stockIndex, offered);
        hand.set(handIndex, replacement);

        if (chain.isEmpty()) {
            requiredOpeningStone = highestDouble(hand);
        }

        return replacement;
    }

    public Stone drawWithTorresmo() {
        Stone stone = draw();

        if (stone != null && !hasLegalMove()) {
            if (stock.isEmpty()) {
                pass();
            } else {
                turn = turn.other();
            }
        }

        return stone;
    }

    public boolean canPass() {
        return !finished && stock.isEmpty() && !hasLegalMove();
    }

    public boolean pass() {
        if (!canPass()) {
            return false;
        }

        consecutivePasses++;
        if (consecutivePasses == 2) {
            int humanPoints = handPoints(Player.HUMAN);
            int opponentPoints = handPoints(Player.OPPONENT);
            if (humanPoints < opponentPoints) {
                winner = Player.HUMAN;
            } else if (opponentPoints < humanPoints) {
                winner = Player.OPPONENT;
            }
            finished = true;
        } else {
            turn = turn.other();
        }
        return true;
    }

    public int handPoints(Player player) {
        int total = 0;
        for (Stone stone : mutableHand(player)) {
            total += stone.first + stone.second;
        }
        return total;
    }

    private boolean hasLegalMove() {
        for (Stone stone : mutableHand(turn)) {
            if (preview(stone, Side.LEFT) != null || preview(stone, Side.RIGHT) != null) {
                return true;
            }
        }
        return false;
    }

    private List<Stone> mutableHand(Player player) {
        return player == Player.HUMAN ? humanHand : opponentHand;
    }

    private Stone takeFromStock() {
        return stock.remove(stock.size() - 1);
    }

    private static Stone highestDouble(List<Stone> hand) {
        Stone best = null;
        for (Stone stone : hand) {
            if (stone.isDouble() && (best == null || stone.first > best.first)) {
                best = stone;
            }
        }
        return best;
    }
}
