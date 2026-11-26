package br.com.dominando.frontend.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.audio.Music;

import br.com.dominando.frontend.audio.GameAudio;

import br.com.dominando.grpc.PetiscoLojaMensagem;
import br.com.dominando.grpc.TipoPetiscoMensagem;

import java.util.ArrayList;
import java.util.List;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;

public final class IntervalView extends Group implements Disposable {
    public enum Snack {
        OVO(2, 645f, 732f, "Ovo Colorido", "Olho no Jogo",
            "No início do seu turno, antes de jogar ou cavar, revele uma peça "
                + "aleatória do adversário por 5 segundos. Só você vê."),
        TORRESMO(4, 1355f, 732f, "Torresmo", "Casca Grossa",
            "Quando estiver sem jogada, ative antes de cavar. Retire apenas uma peça. "
                + "Se ela não encaixar, seu turno termina."),
        COXINHA(4, 990f, 932f, "Coxinha", "Troca de Recheio",
            "No início do seu turno, antes de jogar ou cavar, escolha uma peça da sua "
                + "mão para trocar por uma peça aleatória da cava.");

        private final int price;
        private final float centerX;
        private final float statusTop;
        private final String displayName;
        private final String effect;
        private final String description;

        Snack(int price, float centerX, float statusTop, String displayName, String effect, String description) {
            this.price = price;
            this.centerX = centerX;
            this.statusTop = statusTop;
            this.displayName = displayName;
            this.effect = effect;
            this.description = description;
        }

        public int getPrice() {
            return price;
        }
    }

    private static final Color CREAM = Color.valueOf("F0DCB2");
    private static final Color GREEN = Color.valueOf("234730");
    private static final Color BROWN = Color.valueOf("3F2412");
    private final List<Texture> textures = new ArrayList<>();
    private final BitmapFont font;
    private final Label balanceLabel;
    private final Label timerLabel;
    private final Label warningTimerLabel;
    private final Image warning;
    private final Label[] missingBalance = new Label[Snack.values().length];
    private final Image[] boughtBadges = new Image[Snack.values().length];
    private float remaining;
    private enum OrderState {DETAIL, SENDING, CONFIRMED, REFUSED}
    private final BitmapFont detailFont;
    private final BitmapFont displayFont;
    private final Group orderPanel = new Group();
    private final Image dimmer;
    private final Image reconnectDialog;
    private final Label orderTitle;
    private final Label orderSubtitle;
    private final Label orderDescription;
    private final Label orderNote;
    private final Label orderBalance;
    private final Label closeOrder;
    private final TextButton orderButton;

    private final Set<Snack> boughtSnacks = EnumSet.noneOf(Snack.class);
    private Consumer<Snack> onPurchaseConfirmed;
    private Snack selectedSnack;
    private OrderState orderState = OrderState.DETAIL;
    private int balance;
    private boolean refuseNextOrder;
    private float responseTime;
    private boolean boughtThisInterval;
    private final Runnable onUiClick;

    private final Group trayScene = new Group();
    private final Image trayBackground;

    private boolean onlineMode;
    private Consumer<Snack> onOnlinePurchaseRequested;
    private final List<PetiscoLojaMensagem> onlineOffers = new ArrayList<>();

    private final Music shopAmbience;
    private float shopAmbienceLevel;
    private float warningPulseTime;

    private BitmapFont createFont(String file, int size) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/" + file));

        try {
            FreeTypeFontGenerator.FreeTypeFontParameter params = new FreeTypeFontGenerator.FreeTypeFontParameter();
            params.size = size;
            params.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "áàâãéêíóôõúçÁÀÂÃÉÊÍÓÔÕÚÇ×";
            params.minFilter = Texture.TextureFilter.Linear;
            params.magFilter = Texture.TextureFilter.Linear;
            return generator.generateFont(params);
        } finally {
            generator.dispose();
        }
    }

    public IntervalView(Runnable onUiClick) {
        this.onUiClick = onUiClick;

        shopAmbience = GameAudio.newMusic(Gdx.files.internal("audio/ambiente-bar.ogg"));
        shopAmbience.setLooping(true);
        GameAudio.setVolume(shopAmbience, 0f);

        setSize(1920f, 1080f);
        setVisible(false);
        setTouchable(Touchable.enabled);

        font = createFont("NunitoSans-SemiBold.ttf", 32);
        detailFont = createFont("NunitoSans-Regular.ttf", 26);
        displayFont = createFont("LilitaOne-Regular.ttf", 48);

        trayBackground = addImage(load("bandeja-ampliada.png"), -581.7f, 19.2f, 3182.4f, 1060.8f);

        balanceLabel = addLabel(GREEN, 112f, 980f, 52f, 40f);
        timerLabel = addLabel(CREAM, 1728f, 984f, 140f, 48f);

        Texture boughtTexture = load("comprado.png");
        for (Snack snack : Snack.values()) {
            int i = snack.ordinal();
            float x = snack.centerX - 135f;

            missingBalance[i] = addLabel(CREAM, x, 1048f - snack.statusTop, 270f, 32f);
            missingBalance[i].setFontScale(22f / 32f);
            boughtBadges[i] = addImage(boughtTexture, x, 1008f - snack.statusTop, 270f, 72f);
        }

        warning = addImage(load("aviso.png"), 1408f, 960f, 464f, 80f);
        warningTimerLabel = addLabel(BROWN, 1766f, 974f, 80f, 56f);
        warningTimerLabel.setFontScale(1.25f);
        addSnackHotspot(Snack.OVO, 580f, 446f, 330f, 186f);
        addSnackHotspot(Snack.TORRESMO, 1050f, 446f, 360f, 190f);
        addSnackHotspot(Snack.COXINHA, 770f, 275f, 450f, 221f);

        while (getChildren().size > 0) {
            Actor actor = getChildren().first();
            actor.remove();
            trayScene.addActor(actor);
        }

        trayScene.setSize(1920f, 1080f);
        trayScene.setOrigin(960f, 540f);
        trayScene.setTouchable(Touchable.childrenOnly);
        addActor(trayScene);

        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        try {
            pixel.setColor(Color.BLACK);
            pixel.fill();
            Texture texture = new Texture(pixel);
            textures.add(texture);
            dimmer = addImage(texture, 0f, 0f, 1920f, 1080f);
        } finally {
            pixel.dispose();
        }

        dimmer.getColor().a = 0.5f;
        dimmer.setTouchable(Touchable.enabled);
        dimmer.setVisible(false);

        orderPanel.setBounds(610f, 120f, 700f, 840f);
        orderPanel.setVisible(false);
        addActor(orderPanel);

        Image paper = new Image(load("papel-compra.png"));
        paper.setSize(700f, 840f);
        paper.setTouchable(Touchable.disabled);
        orderPanel.addActor(paper);

        orderTitle = panelLabel(displayFont, Align.center, 140f, 688f, 420f, 56f);
        orderSubtitle = panelLabel(detailFont, Align.center, 140f, 640f, 420f, 40f);
        orderDescription = panelLabel(detailFont, Align.topLeft, 110f, 448f, 480f, 144f);
        orderDescription.setWrap(true);
        orderNote = panelLabel(detailFont, Align.topLeft, 110f, 308f, 480f, 96f);
        orderNote.setWrap(true);
        orderNote.setFontScale(24f / 26f);
        orderBalance = panelLabel(detailFont, Align.left, 110f, 216f, 480f, 64f);

        closeOrder = panelLabel(font, Align.center, 530f, 740f, 44f, 44f);
        closeOrder.setText("×");
        closeOrder.setTouchable(Touchable.enabled);
        onClick(closeOrder, () -> {
            onUiClick.run();
            closeOrderPanel();
        });

        TextureRegionDrawable enabled = new TextureRegionDrawable(
            new TextureRegion(load("botao-compra.png"))
        );
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle(
            enabled, enabled, null, displayFont
        );
        style.fontColor = BROWN;
        style.disabled = new TextureRegionDrawable(
            new TextureRegion(load("botao-desabilitado.png"))
        );
        style.disabledFontColor = Color.valueOf("6B5840");

        orderButton = new TextButton("", style);
        orderButton.getLabel().setFontScale(0.75f);
        orderButton.padBottom(8f);
        orderButton.setBounds(185f, 64f, 331f, 96f);
        orderPanel.addActor(orderButton);
        onClick(orderButton, this::handleOrderButton);

        reconnectDialog = addImage(load("reconexao.png"), 640f, 341f, 640f, 390f);
        reconnectDialog.setVisible(false);
    }

    private void updateShopAmbience(float delta) {
        float target = isReconnecting() ? 0f : 0.32f * Math.min(1f, Math.max(0f, remaining));

        float step = Math.max(0f, delta) * 0.60f;
        float difference = target - shopAmbienceLevel;

        shopAmbienceLevel += Math.max(-step, Math.min(step, difference));

        GameAudio.setVolume(shopAmbience, shopAmbienceLevel);

        if (shopAmbienceLevel <= 0.001f) {
            GameAudio.stop(shopAmbience);
        } else if (!shopAmbience.isPlaying()) {
            GameAudio.play(shopAmbience);
        }
    }

    private void updateWarningMotion(float delta) {
        boolean shake = remaining > 0f && remaining <= 5f && !isReconnecting();
        warningPulseTime = shake ? warningPulseTime + Math.max(0f, delta) : 0f;
        float offset = shake ? (float) Math.sin(warningPulseTime * 26f) * 5f : 0f;

        warning.setX(1408f + offset);
        warningTimerLabel.setX(1766f + offset);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        if (!visible && shopAmbience != null) {
            GameAudio.stop(shopAmbience);
            shopAmbienceLevel = 0f;
            warningPulseTime = 0f;
        }
    }

    public void resize(GameViewport viewport) {
        viewport.cover(dimmer);

        float halfWidth = Math.min(
            960f - trayBackground.getX(),
            trayBackground.getX() + trayBackground.getWidth() - 960f
        );

        float halfHeight = Math.min(
            540f - trayBackground.getY(),
            trayBackground.getY() + trayBackground.getHeight() - 540f
        );

        float scale = Math.max(
            viewport.getWorldWidth() / (2f * halfWidth),
            viewport.getWorldHeight() / (2f * halfHeight)
        );

        trayScene.setScale(scale);
    }

    private Label panelLabel(BitmapFont font, int alignment,
                             float x, float y, float width, float height) {
        Label label = new Label("", new Label.LabelStyle(font, BROWN));
        label.setBounds(x, y, width, height);
        label.setAlignment(alignment);
        label.setTouchable(Touchable.disabled);
        orderPanel.addActor(label);
        return label;
    }

    private void onClick(Actor actor, Runnable action) {
        actor.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }
        });
    }

    private void addSnackHotspot(Snack snack, float x, float y, float w, float h) {
        Actor hotspot = new Actor();
        hotspot.setBounds(x, y, w, h);
        addActor(hotspot);

        onClick(hotspot, () -> {
            if (isTimeUp()
                || isReconnecting()
                || selectedSnack != null) {
                return;
            }

            onUiClick.run();
            selectedSnack = snack;
            orderState = OrderState.DETAIL;
            refreshOrder();
            refreshModalVisibility();
        });
    }

    public void open(int balance, Set<Snack> alreadyBought, boolean boughtThisInterval, boolean refuseNextOrder, Consumer<Snack> onPurchaseConfirmed) {

        this.boughtThisInterval = boughtThisInterval;
        this.balance = balance;
        this.refuseNextOrder = refuseNextOrder;
        this.onPurchaseConfirmed = onPurchaseConfirmed;

        boughtSnacks.clear();
        boughtSnacks.addAll(alreadyBought);
        selectedSnack = null;
        orderState = OrderState.DETAIL;
        remaining = 30f;

        setReconnecting(false);
        refreshOffers();
        refreshClock();
        setVisible(true);

        onlineMode = false;
        onOnlinePurchaseRequested = null;
    }

    public void openOnline(int balance, float secondsRemaining, boolean alreadyBought, Consumer<Snack> onPurchaseRequested) {
        onlineOffers.clear();
        onlineMode = true;
        onOnlinePurchaseRequested = onPurchaseRequested;
        onPurchaseConfirmed = null;

        this.balance = balance;
        boughtThisInterval = alreadyBought;
        refuseNextOrder = false;

        boughtSnacks.clear();
        selectedSnack = null;
        orderState = OrderState.DETAIL;
        remaining = Math.max(0f, secondsRemaining);

        setReconnecting(false);
        refreshOffers();
        refreshClock();
        setVisible(true);
    }

    public void setOnlineOffers(List<PetiscoLojaMensagem> offers) {
        if (!onlineMode || onlineOffers.equals(offers)) {
            return;
        }

        onlineOffers.clear();
        onlineOffers.addAll(offers);

        refreshOffers();

        if (selectedSnack != null) {
            refreshOrder();
        }
    }

    private PetiscoLojaMensagem findOnlineOffer(Snack snack) {
        TipoPetiscoMensagem tipo =
            snack == Snack.OVO ? TipoPetiscoMensagem.OVO_COLORIDO : snack == Snack.COXINHA ? TipoPetiscoMensagem.COXINHA : TipoPetiscoMensagem.TORRESMO;

        for (PetiscoLojaMensagem offer : onlineOffers) {
            if (offer.getTipo() == tipo) {
                return offer;
            }
        }

        return null;
    }

    private String onlineUnavailableReason(PetiscoLojaMensagem offer) {
        if (boughtThisInterval) {
            return "Limite do intervalo";
        }

        if (offer == null) {
            return "Indisponível";
        }

        switch (offer.getMotivoIndisponibilidade()) {
            case "JA_COMPROU_NESTA_LOJA":
                return "Limite do intervalo";

            case "PETISCO_REPETIDO":
                return "Repetido da loja anterior";

            case "JA_POSSUI_PETISCO":
                return "Já possui um petisco";

            case "SALDO_INSUFICIENTE":
                int missing = Math.max(0, offer.getCusto() - balance);

                if (missing == 1) {
                    return "Falta 1 moeda";
                }

                return missing > 1 ? "Faltam " + missing + " moedas" : "Saldo insuficiente";

            case "LOJA_FECHADA":
                return "Intervalo encerrado";

            default:
                return "Indisponível";
        }
    }

    public void updateOnline(
        int balance,
        float secondsRemaining,
        boolean alreadyBought
    ) {
        if (!onlineMode) {
            return;
        }

        this.balance = balance;
        remaining = Math.max(0f, secondsRemaining);
        boughtThisInterval = boughtThisInterval || alreadyBought;

        if (alreadyBought
            && selectedSnack != null
            && orderState == OrderState.SENDING) {
            confirmOnlinePurchase();
        }

        refreshOffers();
        refreshClock();

        if (selectedSnack != null) {
            refreshOrder();
        }
    }

    public void confirmOnlinePurchase() {
        if (!onlineMode || selectedSnack == null) {
            return;
        }

        boughtThisInterval = true;
        boughtSnacks.add(selectedSnack);
        orderState = OrderState.CONFIRMED;

        refreshOffers();
        refreshOrder();
    }

    public void refuseOnlinePurchase() {
        if (!onlineMode
            || selectedSnack == null
            || orderState != OrderState.SENDING) {
            return;
        }

        orderState = OrderState.REFUSED;
        refreshOrder();
    }

    public void closeOnline() {
        setVisible(false);
        selectedSnack = null;
        onOnlinePurchaseRequested = null;
        onlineMode = false;
    }

    private void refreshOffers() {
        balanceLabel.setText(Integer.toString(balance));

        for (Snack snack : Snack.values()) {
            int i = snack.ordinal();

            if (onlineMode) {
                PetiscoLojaMensagem offer = findOnlineOffer(snack);
                boolean bought = boughtSnacks.contains(snack);

                boolean available =
                    offer != null
                        && offer.getPodeComprar()
                        && !boughtThisInterval;

                boughtBadges[i].setVisible(bought);
                missingBalance[i].setVisible(!bought && !available);
                missingBalance[i].setText(onlineUnavailableReason(offer));
                continue;
            }

            int missing = snack.price - balance;
            boolean bought = boughtSnacks.contains(snack);
            boolean intervalLimit = boughtThisInterval;

            boughtBadges[i].setVisible(bought);
            missingBalance[i].setVisible(!bought && (intervalLimit || missing > 0));

            if (intervalLimit) {
                missingBalance[i].setText("Limite do intervalo");
            } else if (missing == 1) {
                missingBalance[i].setText("Falta 1 moeda");
            } else {
                missingBalance[i].setText("Faltam " + missing + " moedas");
            }
        }
    }

    private void refreshOrder() {
        Snack snack = selectedSnack;
        int price = snack.price;

        String title = snack.displayName;
        String subtitle = snack.effect;
        String description = snack.description;
        String note = "Uso manual na próxima rodada. Expira ao final dela, mesmo sem usar.";

        String priceLabel = "Preço";
        boolean disabled = balance < price;
        String buttonText = disabled ? "SEM SALDO" : "COMPRAR";
        boolean canClose = true;

        if (onlineMode) {
            PetiscoLojaMensagem offer = findOnlineOffer(snack);

            if (offer != null) {
                title = offer.getNome();
                description = offer.getDescricao();
                price = offer.getCusto();
            }

            disabled =
                offer == null
                    || !offer.getPodeComprar()
                    || boughtThisInterval;

            buttonText = disabled ? "INDISPONÍVEL" : "COMPRAR";

            if (snack == Snack.TORRESMO) {
                subtitle = "Nova mão";
                note = "Use na preparação da próxima rodada. Expira ao final dela.";
            }

            if (disabled) {
                note = onlineUnavailableReason(offer);
            }
        }

        if (orderState == OrderState.DETAIL
            && (boughtThisInterval || boughtSnacks.contains(snack))) {
            buttonText = "VOLTAR À LOJA";
            disabled = false;
            canClose = true;

            note = boughtSnacks.contains(snack) ? "Você já comprou este petisco. " + "Consulte acima como utilizar." : "Você já comprou neste intervalo. " + "Pode consultar os efeitos dos outros petiscos.";
        }

        switch (orderState) {
            case SENDING:
                buttonText = "ENVIANDO...";
                disabled = true;
                canClose = false;
                break;

            case CONFIRMED:
                title = "Pedido Confirmado!";
                subtitle = snack.displayName;
                description = "Seu petisco está reservado para a próxima rodada. " + "Aguarde o fim do intervalo.";
                note = "Você já comprou neste intervalo. " + "Poderá comprar outro no próximo.";
                priceLabel = "Compra";
                buttonText = "VOLTAR À LOJA";
                disabled = false;
                canClose = true;
                break;

            case REFUSED:
                title = "Pedido recusado";
                subtitle = snack.displayName;
                description = "Seu pedido não foi aceito. Nenhuma moeda foi descontada.";
                note = "Volte à bandeja para consultar os petiscos enquanto o intervalo estiver aberto.";
                buttonText = "VOLTAR";
                disabled = false;
                break;

            default:
                break;
        }

        orderTitle.setText(title);
        orderTitle.setFontScale(orderState == OrderState.DETAIL || orderState == OrderState.SENDING ? 1f : 40f / 48f);
        orderSubtitle.setText(subtitle);
        orderDescription.setText(description);
        orderNote.setText(note);
        orderBalance.setText(priceLabel + ": " + price + " moedas\nSeu saldo: " + balance + " moedas");
        orderButton.setText(buttonText);
        orderButton.setDisabled(disabled);
        closeOrder.setVisible(canClose);
    }

    private void handleOrderButton() {
        if (isTimeUp() || isReconnecting()
            || orderButton.isDisabled()) {
            return;
        }

        if (orderState == OrderState.REFUSED || orderState == OrderState.CONFIRMED || (orderState == OrderState.DETAIL
            && (boughtThisInterval || boughtSnacks.contains(selectedSnack)))) {
            onUiClick.run();
            closeOrderPanel();
            return;
        }

        if (boughtThisInterval || orderState != OrderState.DETAIL) {
            return;
        }

        onUiClick.run();
        orderState = OrderState.SENDING;
        refreshOrder();

        if (onlineMode) {
            if (onOnlinePurchaseRequested != null) {
                onOnlinePurchaseRequested.accept(selectedSnack);
            } else {
                refuseOnlinePurchase();
            }
        } else {
            responseTime = 0.6f;
        }
    }

    private void completeOrder() {
        if (refuseNextOrder) {
            refuseNextOrder = false;
            orderState = OrderState.REFUSED;
        } else {
            balance -= selectedSnack.price;
            boughtSnacks.add(selectedSnack);
            boughtThisInterval = true;
            orderState = OrderState.CONFIRMED;
            onPurchaseConfirmed.accept(selectedSnack);
        }

        refreshOffers();
        refreshOrder();
    }

    private void closeOrderPanel() {
        selectedSnack = null;
        orderState = OrderState.DETAIL;
        refreshModalVisibility();
    }

    public void setReconnecting(boolean reconnecting) {
        reconnectDialog.setVisible(reconnecting);
        refreshModalVisibility();
    }

    public boolean isReconnecting() {
        return reconnectDialog.isVisible();
    }

    private void refreshModalVisibility() {
        boolean showOrder = selectedSnack != null && !isReconnecting();
        orderPanel.setVisible(showOrder);
        dimmer.setVisible(showOrder || isReconnecting());
    }

    @Override
    public void act(float delta) {
        if (!isVisible()) {
            return;
        }
        super.act(delta);
        if (!onlineMode) {
            remaining = Math.max(0f, remaining - delta);
        }
        refreshClock();
        updateShopAmbience(delta);
        updateWarningMotion(delta);

        if (!onlineMode && orderState == OrderState.SENDING && !isReconnecting() && !isTimeUp()) {
            responseTime -= delta;
            if (responseTime <= 0f) {
                completeOrder();
            }
        }
    }

    public boolean isTimeUp() {
        return remaining <= 0f;
    }

    private void refreshClock() {
        int seconds = (int) Math.ceil(remaining);
        boolean lastSeconds = seconds <= 5;
        String time = seconds + "s";

        timerLabel.setText(time);
        timerLabel.setVisible(!lastSeconds);
        warning.setVisible(lastSeconds);
        warningTimerLabel.setText(time);
        warningTimerLabel.setVisible(lastSeconds);
    }


    private Texture load(String name) {
        Texture texture = new Texture(Gdx.files.internal("ui/interval/" + name));
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        textures.add(texture);
        return texture;
    }

    private Image addImage(Texture texture, float x, float y, float width, float height) {
        Image image = new Image(texture);
        image.setBounds(x, y, width, height);
        image.setTouchable(Touchable.disabled);
        addActor(image);
        return image;
    }

    private Label addLabel(Color color, float x, float y, float width, float height) {
        Label label = new Label("", new Label.LabelStyle(font, color));
        label.setBounds(x, y, width, height);
        label.setAlignment(Align.center);
        label.setTouchable(Touchable.disabled);
        addActor(label);
        return label;
    }

    @Override
    public void dispose() {
        font.dispose();
        detailFont.dispose();
        displayFont.dispose();
        GameAudio.dispose(shopAmbience);
        for (Texture texture : textures) {
            texture.dispose();
        }
    }
}
