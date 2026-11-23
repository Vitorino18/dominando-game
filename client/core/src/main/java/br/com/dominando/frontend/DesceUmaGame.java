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

import br.com.dominando.grpc.EstadoJogoResponse;

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

    private boolean salaPodeIniciar;
    private boolean iniciandoPartida;
    private boolean partidaIniciada;

    private EstadoJogoResponse ultimoEstadoJogo;
    private boolean acompanhandoJogo;
    private boolean falhaStreamingDefinitiva;
    private float tempoReconexaoStreaming;

    private int versaoSessao;

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
        versaoSessao++;
        clienteJogo.pararAcompanhamento();
        acompanhandoJogo = false;
        ultimoEstadoJogo = null;
        partidaIniciada = false;

        if (pairingScreen == null) {
            pairingScreen = new PairingScreen(() -> Gdx.app.postRunnable(this::showOpening), clienteJogo, this::showWaiting);
        }

        setScreen(pairingScreen);
    }

    private void iniciarPartida() {
        if (!criadorSala
            || !salaPodeIniciar
            || iniciandoPartida
            || partidaIniciada
            || getScreen() != waitingScreen) {
            return;
        }

        iniciandoPartida = true;
        salaPodeIniciar = false;

        WaitingScreen telaSolicitante = waitingScreen;

        telaSolicitante.atualizarInicio(false, true);
        telaSolicitante.atualizarStatus(
            "Solicitando início da partida..."
        );

        clienteJogo.iniciarPartida(
            codigoSala,
            tokenJogador,
            resposta -> {
                if (getScreen() != telaSolicitante
                    || waitingScreen != telaSolicitante) {
                    return;
                }

                iniciandoPartida = false;
                partidaIniciada =
                    partidaIniciada || resposta.getIniciada();

                tempoConsultaSala = 0f;
                telaSolicitante.atualizarInicio(false, false);

                if (!partidaIniciada) {
                    telaSolicitante.atualizarStatus(
                        "Início não confirmado.\nConsultando o servidor..."
                    );
                }
            },
            erro -> {
                if (getScreen() != telaSolicitante
                    || waitingScreen != telaSolicitante) {
                    return;
                }

                iniciandoPartida = false;
                tempoConsultaSala = 0f;

                telaSolicitante.atualizarInicio(false, false);

                if (!partidaIniciada) {
                    telaSolicitante.atualizarStatus(
                        "Não foi possível confirmar o início.\n"
                            + "Consultando o servidor..."
                    );
                }
            }
        );
    }

    private void atualizarStreaming() {
        if (!partidaIniciada
            || waitingScreen == null
            || !emFluxoPartida()
            || acompanhandoJogo
            || falhaStreamingDefinitiva) {
            return;
        }

        if (ultimoEstadoJogo != null
            && ultimoEstadoJogo.getCompeticaoEncerrada()) {
            return;
        }

        tempoReconexaoStreaming -= Gdx.graphics.getDeltaTime();

        if (tempoReconexaoStreaming > 0f) {
            return;
        }

        acompanhandoJogo = true;
        iniciandoPartida = false;
        salaPodeIniciar = false;
        int sessao = versaoSessao;

        WaitingScreen telaAcompanhada = waitingScreen;

        telaAcompanhada.atualizarInicio(false, false);
        telaAcompanhada.atualizarStatus(
            "Recebendo estado da partida..."
        );

        clienteJogo.acompanharJogo(
            codigoSala,
            tokenJogador,
            estado -> {
                if (sessao != versaoSessao || !emFluxoPartida()) {
                    return;
                }

                ultimoEstadoJogo = estado;

                if (matchScreen == null) {
                    if (estado.getCompeticaoEncerrada()) {
                        telaAcompanhada.atualizarStatus(
                            "Partida encerrada.\nCampeão: "
                                + estado.getCampeao()
                        );
                    } else {
                        showMatchIntro();
                    }
                } else {
                    matchScreen.setOnlineConnectionMessage(null);
                    matchScreen.aplicarEstadoOnline(estado);
                }

                Gdx.app.log(
                    "Rede",
                    "Estado recebido: mao=" + estado.getMinhaMaoCount()
                        + ", mesa=" + estado.getPecasMesaCount()
                        + ", cava=" + estado.getPecasBaralho()
                );
            },
            erro -> {
                if (sessao != versaoSessao || !emFluxoPartida()) {
                    return;
                }

                acompanhandoJogo = false;

                if (ultimoEstadoJogo != null
                    && ultimoEstadoJogo.getCompeticaoEncerrada()) {
                    return;
                }

                Status.Code codigo =
                    Status.fromThrowable(erro).getCode();

                boolean temporario =
                    codigo == Status.Code.UNAVAILABLE
                        || codigo == Status.Code.DEADLINE_EXCEEDED
                        || codigo == Status.Code.CANCELLED;

                falhaStreamingDefinitiva = !temporario;
                tempoReconexaoStreaming = 3f;

                if (matchScreen != null) {
                    matchScreen.setOnlineConnectionMessage(
                        temporario ? "Reconectando..." : "Sem conexão"
                    );
                } else {
                    telaAcompanhada.atualizarStatus(
                        temporario
                            ? "Conexão de jogo interrompida.\nReconectando..."
                            : "Não foi possível acompanhar a partida.\n"
                            + "Volte e conecte novamente."
                    );
                }
            }
        );
    }

    private void showWaiting(String codigo, String token, String nome, boolean criador) {
        versaoSessao++;
        clienteJogo.pararAcompanhamento();
        ultimoEstadoJogo = null;
        acompanhandoJogo = false;
        falhaStreamingDefinitiva = false;
        tempoReconexaoStreaming = 0f;
        codigoSala = codigo;
        tokenJogador = token;
        nomeJogador = nome;
        criadorSala = criador;
        tempoConsultaSala = 0f;
        consultaSalaEmAndamento = false;
        consultaSalaSuspensa = false;
        salaPodeIniciar = false;
        iniciandoPartida = false;
        partidaIniciada = false;

        WaitingScreen previousWaiting = waitingScreen;

        waitingScreen = new WaitingScreen(codigoSala, criadorSala, () -> Gdx.app.postRunnable(this::showPairing), this::iniciarPartida);

        setScreen(waitingScreen);

        if (previousWaiting != null) {
            previousWaiting.dispose();
        }
    }

    private boolean emFluxoPartida() {
        Screen atual = getScreen();

        return atual != null
            && (atual == waitingScreen
            || atual == matchIntroScreen
            || atual == matchScreen);
    }

    private void showOpening() {
        setScreen(openingScreen);
    }

    private void atualizarEspera() {
        if (waitingScreen == null || getScreen() != waitingScreen || consultaSalaEmAndamento || consultaSalaSuspensa || partidaIniciada) {
            return;
        }

        tempoConsultaSala -= Gdx.graphics.getDeltaTime();

        if (tempoConsultaSala > 0f) {
            return;
        }

        consultaSalaEmAndamento = true;
        WaitingScreen telaConsultada = waitingScreen;

        clienteJogo.consultarSala(codigoSala, tokenJogador, resposta -> {
                if (getScreen() != telaConsultada || waitingScreen != telaConsultada || partidaIniciada) {
                    return;
                }

                consultaSalaEmAndamento = false;
                tempoConsultaSala = 1f;

                partidaIniciada = partidaIniciada || resposta.getIniciada();
                salaPodeIniciar = resposta.getPodeIniciar() && !partidaIniciada;

                if (partidaIniciada) {
                    iniciandoPartida = false;
                }

                telaConsultada.atualizarInicio(
                    salaPodeIniciar,
                    iniciandoPartida
                );

                if (partidaIniciada) {
                    telaConsultada.atualizarStatus("Partida iniciada no servidor.");
                } else if (iniciandoPartida) {
                    telaConsultada.atualizarStatus("Solicitando início da partida...");
                } else if (resposta.getNomesJogadoresCount() == 2) {
                    telaConsultada.atualizarStatus(criadorSala ? "Adversário conectado. Mesa pronta!" : "Conectados! Aguardando o criador.");
                } else {
                    telaConsultada.atualizarStatus("Aguardando o outro jogador...");
                }
            },
            erro -> {
                if (getScreen() != telaConsultada || waitingScreen != telaConsultada || partidaIniciada) {
                    return;
                }

                consultaSalaEmAndamento = false;
                Status.Code codigo = Status.fromThrowable(erro).getCode();

                salaPodeIniciar = false;
                telaConsultada.atualizarInicio(false, iniciandoPartida);

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

    private void showMatchIntro() {
        if (matchScreen == null) {
            matchScreen = new MatchScreen(
                () -> Gdx.app.postRunnable(this::leaveMatch),
                this::playUiClickSound,
                nomeJogador,
                ultimoEstadoJogo
            );
        }

        if (matchIntroScreen == null) {
            matchIntroScreen = new MatchIntroScreen(
                gameSettings,
                () -> Gdx.app.postRunnable(this::showMatch)
            );
        }

        setScreen(matchIntroScreen);
    }

    private void showMatch() {
        if (matchScreen != null) {
            setScreen(matchScreen);
        }
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
        atualizarStreaming();
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
