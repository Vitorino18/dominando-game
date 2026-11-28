package br.com.dominando.server;

import br.com.dominando.grpc.*;
import br.com.dominando.model.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import io.grpc.StatusRuntimeException;

public class DesceUmaGrpcService extends DesceUmaServiceGrpc.DesceUmaServiceImplBase{
    private final SalaService salaService;
    private final ExecutorService executorStreaming = Executors.newFixedThreadPool(4);

    public DesceUmaGrpcService(SalaService salaService) {
        this.salaService = salaService;
    }

    @Override
    public void criarSala(CriarSalaRequest request, StreamObserver<CriarSalaResponse> responseObserver) {

        try {
            Sala sala = salaService.criarSala(request.getNomeCriador());

            CriarSalaResponse response = CriarSalaResponse.newBuilder().setCodigoSala(sala.getCodigo()).
                    setTokenJogador(sala.getCriador().getToken()).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (RuntimeException e) {
            responderErro(responseObserver, e);
        }
    }

    @Override
    public void entrarSala(EntrarSalaRequest request, StreamObserver<EntrarSalaResponse> responseObserver) {

        try {
            ParticipanteSala participante = salaService.entrarNaSala(request.getCodigoSala(), request.getNomeJogador());

            EntrarSalaResponse response = EntrarSalaResponse.newBuilder().setCodigoSala(request.getCodigoSala().trim().toUpperCase()).
                    setTokenJogador(participante.getToken()).build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (RuntimeException e) {
            responderErro(responseObserver, e);
        }
    }

    @Override
    public void consultarSala(ConsultarSalaRequest request, StreamObserver<ConsultarSalaResponse> responseObserver) {

        try {
            Sala sala = buscarSalaAutorizada(request.getCodigoSala(), request.getTokenJogador());

            if (sala == null) {
                throw new IllegalArgumentException("Sala não encontrada.");
            }

            if (sala.buscarPorToken(request.getTokenJogador()) == null) {
                throw new SecurityException("Jogador não autorizado.");
            }

            ConsultarSalaResponse response = ConsultarSalaResponse.newBuilder().setCodigoSala(sala.getCodigo()).addAllNomesJogadores(sala.getJogadores())
                            .setIniciada(sala.isIniciada()).setPodeIniciar(sala.podeIniciar()).build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (RuntimeException e) {
            responderErro(responseObserver, e);
        }
    }

    @Override
    public void iniciarPartida(IniciarPartidaRequest request, StreamObserver<IniciarPartidaResponse> responseObserver) {

        try {
            Sala sala = buscarSalaAutorizada(request.getCodigoSala(), request.getTokenCriador());
            sala.iniciarPartida(request.getTokenCriador());
            sala.notificarJogadores();

            IniciarPartidaResponse response = IniciarPartidaResponse.newBuilder().setIniciada(true).build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (RuntimeException e) {
            responderErro(responseObserver, e);
        }
    }

    @Override
    public void consultarEstadoJogo(EstadoJogoRequest request, StreamObserver<EstadoJogoResponse> responseObserver) {

        try {
            Sala sala = buscarSalaAutorizada(request.getCodigoSala(), request.getTokenJogador());

            EstadoJogoResponse resposta = sala.consultarEstadoJogo(request.getTokenJogador());

            responseObserver.onNext(resposta);
            responseObserver.onCompleted();

        } catch (RuntimeException e) {
            responderErro(responseObserver, e);
        }
    }

    @Override
    public void acompanharJogo(EstadoJogoRequest request, StreamObserver<EstadoJogoResponse> responseObserver) {
        try {
            Sala sala = buscarSalaAutorizada(request.getCodigoSala(), request.getTokenJogador());
            sala.acompanharJogo(request.getTokenJogador(), responseObserver, executorStreaming);
        } catch (RuntimeException e) {
            responderErro(responseObserver, e);
        }
    }

    @Override
    public void jogarPeca(JogarPecaRequest request, StreamObserver<AcaoJogoResponse> responseObserver) {

        try {
            Sala sala = buscarSalaAutorizada(request.getCodigoSala(), request.getTokenJogador());

            LadoMesa lado = switch (request.getLado()) {
                case ESQUERDA -> LadoMesa.ESQUERDA;
                case DIREITA -> LadoMesa.DIREITA;
                default -> throw new IllegalArgumentException("Lado da mesa inválido.");
            };

            ResultadoJogada resultado = sala.executarAcao(request.getTokenJogador(),
                    AcaoJogo.JOGAR_PECA, request.getIndicePeca(), lado
            );

            enviarResultado(sala, resultado, responseObserver);
            sala.notificarJogadores();

        } catch (RuntimeException e) {
            responderErro(responseObserver, e);
        }
    }

    @Override
    public void comprarPeca(AcaoSimplesRequest request, StreamObserver<AcaoJogoResponse> responseObserver) {

        try {
            Sala sala = buscarSalaAutorizada(request.getCodigoSala(), request.getTokenJogador());
            ResultadoJogada resultado = sala.executarAcao(request.getTokenJogador(),
                    AcaoJogo.COMPRAR_PECA, null, null
            );

            enviarResultado(sala, resultado, responseObserver);
            sala.notificarJogadores();

        } catch (RuntimeException e) {
            responderErro(responseObserver, e);
        }
    }

    private TipoPetiscos converterTipoPetisco(TipoPetiscoMensagem tipo) {
        return switch (tipo) {
            case OVO_COLORIDO -> TipoPetiscos.OVO_COLORIDO;
            case COXINHA -> TipoPetiscos.COXINHA;
            case TORRESMO -> TipoPetiscos.TORRESMO;
            default -> throw new IllegalArgumentException("Tipo de petisco inválido.");
        };
    }

    @Override
    public void comprarPetisco(ComprarPetiscoRequest request, StreamObserver<ComprarPetiscoResponse> responseObserver) {
        try {
            Sala sala = buscarSalaAutorizada(request.getCodigoSala(), request.getTokenJogador());
            TipoPetiscos tipo = converterTipoPetisco(request.getTipo());

            ResultadoPetiscosLoja resultado = sala.comprarPetisco(request.getTokenJogador(), tipo);

            ComprarPetiscoResponse resposta = ComprarPetiscoResponse.newBuilder().setResultado(resultado.name()).build();

            responseObserver.onNext(resposta);
            responseObserver.onCompleted();

        } catch (RuntimeException e) {
            responderErro(responseObserver, e);
        }
    }

    @Override
    public void utilizarPetisco(UtilizarPetiscoRequest request, StreamObserver<UtilizarPetiscoResponse> responseObserver) {
        try {
            Sala sala = buscarSalaAutorizada(request.getCodigoSala(), request.getTokenJogador());

            TipoPetiscos tipo = converterTipoPetisco(request.getTipo());

            Integer indicePeca = request.hasIndicePeca() ? request.getIndicePeca() : null;

            UsoPetisco uso = sala.utilizarPetisco(request.getTokenJogador(), tipo, indicePeca);

            UtilizarPetiscoResponse.Builder resposta = UtilizarPetiscoResponse.newBuilder().setResultado(uso.resultado().name());

            if (uso.sucesso() && uso.pecaRevelada() != null) {
                Peca peca = uso.pecaRevelada();
                PecaMensagem pecaMensagem = PecaMensagem.newBuilder().setLadoA(peca.getLadoA()).setLadoB(peca.getLadoB()).build();

                resposta.setPecaRevelada(pecaMensagem);
                resposta.setDuracaoRevelacaoSegundos(uso.duracaoRevelacaoSegundos());
            }

            responseObserver.onNext(resposta.build());
            responseObserver.onCompleted();
        } catch (RuntimeException e) {
            responderErro(responseObserver, e);
        }
    }

    @Override
    public void confirmarPreparacao(AcaoSimplesRequest request, StreamObserver<ConfirmarPreparacaoResponse> responseObserver) {
        try {
            Sala sala = buscarSalaAutorizada(request.getCodigoSala(), request.getTokenJogador());
            boolean confirmado = sala.confirmarPreparacao(request.getTokenJogador());

            ConfirmarPreparacaoResponse resposta = ConfirmarPreparacaoResponse.newBuilder().setConfirmado(confirmado).build();

            responseObserver.onNext(resposta);
            responseObserver.onCompleted();
        } catch (RuntimeException e) {
            responderErro(responseObserver, e);
        }
    }

    @Override
    public void passarVez(AcaoSimplesRequest request, StreamObserver<AcaoJogoResponse> responseObserver) {

        try {

            Sala sala = buscarSalaAutorizada(request.getCodigoSala(), request.getTokenJogador());
            ResultadoJogada resultado = sala.executarAcao(request.getTokenJogador(), AcaoJogo.PASSAR_VEZ, null, null);

            enviarResultado(sala, resultado, responseObserver);
            sala.notificarJogadores();

        } catch (RuntimeException e) {
            responderErro(responseObserver, e);
        }
    }

    private Sala buscarSalaObrigatoria(String codigo) {
        Sala sala = salaService.buscarSala(codigo);

        if (sala == null) {
            throw new IllegalArgumentException("Sala não encontrada.");
        }
        return sala;
    }

    private Sala buscarSalaAutorizada(String codigo, String token) {
        Sala sala = buscarSalaObrigatoria(codigo);
        sala.validarRequisicao(token);

        return sala;
    }

    private void enviarResultado(Sala sala, ResultadoJogada resultado, StreamObserver<AcaoJogoResponse> responseObserver) {

        AcaoJogoResponse.Builder resposta = AcaoJogoResponse.newBuilder().setResultado(resultado.name());

        // Consulta o estado final da competição
        PartidaGeral partidaGeral = sala.getPartidaGeral();

        if (partidaGeral != null && partidaGeral.competicaoEncerrada()) {
            resposta.setCompeticaoEncerrada(true);
            resposta.setCampeao(partidaGeral.getCampeao().getNome());
        }

        responseObserver.onNext(resposta.build());
        responseObserver.onCompleted();
    }


    private void responderErro(StreamObserver<?> observer, RuntimeException e) {
        Status status;

        if (e instanceof StatusRuntimeException erroGrpc) {
            observer.onError(erroGrpc);
            return;
        }
        else if (e instanceof SecurityException) {
            status = Status.PERMISSION_DENIED;
        } else if (e instanceof IllegalArgumentException) {
            status = Status.INVALID_ARGUMENT;
        } else if (e instanceof IllegalStateException) {
            status = Status.FAILED_PRECONDITION;
        } else {
            status = Status.INTERNAL;
        }
        observer.onError(status.withDescription(status == Status.INTERNAL ? "Erro interno do servidor." : e.getMessage()).asRuntimeException());
    }
}
