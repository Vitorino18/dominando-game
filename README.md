gRpc:
É um framework de código aberto que implementa o modelo de comunicação RPC.
Enquanto o RPC é um conceito de comunicação, o gRPC é a tecnologia que utilizamos para colocar esse conceito em prática.

utilizamos o gRPC para disponibilizar oito operações remotas:
CriarSala: Criar uma sala no servidor.
EntrarSala: Adicionar um jogador à sala.
ConsultarSala: Consultar informações da sala.
IniciarPartida: Iniciar a competição.
ConsultarEstadoJogo: Consultar o estado atual do jogo.
JogarPeca: Jogar uma peça na mesa.
ComprarPeca: Comprar uma peça do cava.
PassarVez: Passar o turno.
Nós usamos a comunicação Unary RPC no jogo. O Unary RPC é Uma solicitação e uma resposta como comunicação do cliente com o servidor.
RPC (Remote Procedure Call):
RPC ou Chamado de Procedimento Remoto, é um modelo de comunicação que permite que uma aplicação execute um procedimento em outra aplicação, que pode estar localizada em outro dispositivo. É como chamar um método Java normalmente, porém esse método será executado em outra máquina.
No nosso jogo, o celular solicita uma ação, mas quem realmente executa essa ação é o servidor no notebook. Isso garante que as regras do jogo sejam centralizadas.
Protocol Buffers:
Protocol Buffers é um mecanismo de serialização de dados desenvolvido pelo Google, independente de linguagem de programação e plataforma. Ele também permite definir o contrato de comunicação entre o cliente e o servidor.
A serialização é o processo de transformar dados estruturados em um formato que possa ser armazenado ou transmitido pela rede.
O Protobuf utiliza menos espaço que formatos textuais como XML e JSON para representar dados estruturados E ele permite que o servidor e o aplicativo Android compartilhem o mesmo contrato, sem precisar compartilhar todas as classes Java do jogo.
HTTP/2:
É um protocolo de comunicação da camada de aplicação utilizado para transportar informações entre cliente e servidor. Ele permite reutilizar a conexão para realizar diversas operações, como consultar a mesa, comprar uma peça e realizar uma jogada.
O gRPC utiliza HTTP/2 como sua base de comunicação. 
Netty:
Ele trabalha por trás do gRPC, sendo responsável por atividades como gerenciamento de conexões, recebimento e envio de dados, processamento de eventos de rede e suporte à comunicação não bloqueante. 
Quando iniciamos nosso servidor na porta 50051, é ela que transporta e permite receber as conexões dos clientes.

Arquitetura completa do Desce Uma:
Segui um fluxograma de como todas as tecnologias estão trabalhando em conjuntas.
