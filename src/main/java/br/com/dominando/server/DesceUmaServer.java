package br.com.dominando.server;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

// servidor rodando com apenas o mesmo wifi //
public class DesceUmaServer {
    private static final int PORTA = 50051;

    public static void main(String[] args) throws IOException, InterruptedException {
        SalaService salaService = new SalaService();
        Server servidor = ServerBuilder.forPort(PORTA).
                addService(new DesceUmaGrpcService(salaService)).build().start();

        System.out.println("=================================");
        System.out.println("    SERVIDOR DESCE UMA ONLINE    ");
        System.out.println("=================================");
        System.out.println("Porta: " + PORTA);
        System.out.println("Aguardando jogadores...");

        // Registra uma ação de encerramento quando a JVM for finalizado normalmente //

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Encerrando servidor...");
            servidor.shutdown();
            salaService.encerrar();
            servidor.shutdownNow();
        }));
        // evita encerrar o servidor antes de executar o encerramento do servidor //
        servidor.awaitTermination();
    }
}


