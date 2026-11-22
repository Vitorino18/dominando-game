package br.com.dominando.frontend;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

import br.com.dominando.frontend.settings.GameSettings;

import br.com.dominando.frontend.screens.OpeningScreen;
import br.com.dominando.frontend.screens.PairingScreen;
import br.com.dominando.frontend.screens.WaitingScreen;
import br.com.dominando.frontend.screens.MatchIntroScreen;
import br.com.dominando.frontend.screens.MatchScreen;

import br.com.dominando.frontend.audio.GameAudio;

import br.com.dominando.frontend.network.ClienteJogo;

import io.grpc.Status;


public class DesceUmaGame extends Game {

    private OpeningScreen openingScreen;
    private PairingScreen pairingScreen;
    private WaitingScreen waitingScreen;
    private MatchIntroScreen matchIntroScreen;
    private MatchScreen matchScreen;

    private GameSettings gameSettings;
    private Music menuMusic;
    private Sound uiClickSound;
    private boolean menuMusicStarted;

    private ClienteJogo clienteJogo;
    private String codigoSala;
    private String tokenJogador;
    private String nomeJogador;
    private boolean criadorSala;

    private float tempoConsultaSala;
    private boolean consultaSalaEmAndamento;
    private boolean consultaSalaSuspensa;

    @Override
    public void create() {
        gameSettings = new GameSettings();
        GameAudio.init(gameSettings);
        clienteJogo = new ClienteJogo("192.168.100.57", 50051);
        uiClickSound = GameAudio.newSound(Gdx.files.internal("audio/clique-botao.wav"));
        menuMusic = Gdx.audio.newMusic(Gdx.files.internal("audio/samba-menu-loop.ogg"));
        menuMusic.setLooping(true);
        openingScreen = new OpeningScreen(() -> Gdx.app.postRunnable(this::showPairing));
        setScreen(openingScreen);
    }

    private void showPairing() {
        if (pairingScreen == null) {
            pairingScreen = new PairingScreen(() -> Gdx.app.postRunnable(this::showOpening), clienteJogo, this::showWaiting);
        }

        setScreen(pairingScreen);
    }

    private void showWaiting(String codigo, String token, String nome, boolean criador
    ) {
        codigoSala = codigo;
        tokenJogador = token;
        nomeJogador = nome;
        criadorSala = criador;
        tempoConsultaSala = 0f;
        consultaSalaEmAndamento = false;
        consultaSalaSuspensa = false;

        WaitingScreen previousWaiting = waitingScreen;

        waitingScreen = new WaitingScreen(codigoSala, criadorSala, () -> Gdx.app.postRunnable(this::showPairing));

        setScreen(waitingScreen);

        if (previousWaiting != null) {
            previousWaiting.dispose();
        }
    }

    private void showOpening() {
        setScreen(openingScreen);
    }

    private void atualizarEspera() {
        if (waitingScreen == null || getScreen() != waitingScreen || consultaSalaEmAndamento || consultaSalaSuspensa) {
            return;
        }

        tempoConsultaSala -= Gdx.graphics.getDeltaTime();

        if (tempoConsultaSala > 0f) {
            return;
        }

        consultaSalaEmAndamento = true;
        WaitingScreen telaConsultada = waitingScreen;

        clienteJogo.consultarSala(codigoSala, tokenJogador, resposta -> {
                if (getScreen() != telaConsultada || waitingScreen != telaConsultada) {
                    return;
                }

                consultaSalaEmAndamento = false;
                tempoConsultaSala = 1f;

                if (resposta.getIniciada()) {
                    telaConsultada.atualizarStatus("Partida iniciada no servidor.");
                } else if (resposta.getNomesJogadoresCount() == 2) {
                    telaConsultada.atualizarStatus(criadorSala ? "Adversário conectado. Mesa pronta!" : "Conectados! Aguardando o criador.");
                } else {
                    telaConsultada.atualizarStatus("Aguardando o outro jogador...");
                }
            },
            erro -> {
                if (getScreen() != telaConsultada || waitingScreen != telaConsultada) {
                    return;
                }

                consultaSalaEmAndamento = false;
                Status.Code codigo = Status.fromThrowable(erro).getCode();

                if (codigo == Status.Code.UNAVAILABLE || codigo == Status.Code.DEADLINE_EXCEEDED) {
                    tempoConsultaSala = 3f;
                    telaConsultada.atualizarStatus("Conexão indisponível.\nTentando novamente...");
                } else {
                    consultaSalaSuspensa = true;
                    telaConsultada.atualizarStatus("Não foi possível consultar a mesa.\n" + "Volte e conecte novamente.");
                }
            }
        );
    }

    private void showMatchIntroDemo() {
        if (matchScreen == null) {
            matchScreen = new MatchScreen(() -> Gdx.app.postRunnable(this::leaveMatch), this::playUiClickSound);
        }

        if (matchIntroScreen == null) {
            matchIntroScreen = new MatchIntroScreen(gameSettings, () -> Gdx.app.postRunnable(this::showMatchDemo));
        }

        setScreen(matchIntroScreen);
    }

    private void showMatchDemo() {
        setScreen(matchScreen);
    }

    private void playUiClickSound() {
        if (audioInForeground && gameSettings.isSoundEffectsEnabled()) {
            GameAudio.play(uiClickSound, 1f);
        }
    }

    private void leaveMatch() {
        if (matchScreen == null || getScreen() != matchScreen) {
            return;
        }

        playUiClickSound();
        MatchScreen previousMatch = matchScreen;
        showPairing();
        matchScreen = null;
        previousMatch.dispose();
    }

    @Override
    public void render() {
        super.render();
        atualizarEspera();
        updateMenuMusic();
        GameAudio.update();
    }

    private boolean audioInForeground = true;

    public void setAudioInForeground(boolean foreground) {
        audioInForeground = foreground;
        GameAudio.setForeground(foreground);

        if (!foreground && menuMusic != null && menuMusic.isPlaying()) {
            menuMusic.pause();
        }
    }

    @Override
    public void pause() {
        setAudioInForeground(false);
        super.pause();
    }

    @Override
    public void resume() {
        super.resume();
        setAudioInForeground(true);
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

        if (clienteJogo != null) {
            clienteJogo.dispose();
        }

        menuMusic.dispose();
        GameAudio.dispose(uiClickSound);

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

        GameAudio.dispose();
    }
}
