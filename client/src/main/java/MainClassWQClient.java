import RMIRegistrationService.RegistrationRemoteService;
import RMIRegistrationService.RegistrationResponseStatusCode;
import connection.ClientState;
import protocol.OperationCode;
import protocol.WQPacket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class MainClassWQClient {

    private RegistrationRemoteService registrationService;

    private final int BUFFER_SIZE = 512;

    private SocketChannel socket;
    private Selector selector;

    private String[] commandAndParams;
    private boolean canTakeCommand;

    private ClientState connection;

    MainClassWQClient() throws IOException, InterruptedException {
        socket = SocketChannel.open();
        socket.configureBlocking(false);
        selector = Selector.open();
        boolean connected = socket.connect(new InetSocketAddress("localhost", 6789));
        if (!connected)
            socket.register(selector, SelectionKey.OP_CONNECT);
        else {
            System.out.println("Client Connected");
        }
        this.canTakeCommand = true;
        this.connection = new ClientState();
        // Connect the client.
        this.manageCommunication();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("[Client] Starting client");
        MainClassWQClient mainClassWQClient = new MainClassWQClient();
        mainClassWQClient.run();
    }

    private void run() {
        try {

            String command;
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            do {
                if (!this.canTakeCommand) {
                    this.manageCommunication();
                    continue;
                }
                System.out.println("> [help]");
                command = br.readLine().toLowerCase();
                this.commandAndParams = command.split(" ");
                switch (commandAndParams[0]) {
                    case "help":
                        performHelp();
                        break;
                    case "register":
                        performRegister();
                        break;
                    case "login":
                        this.sendLogin();
                        break;
                    case "add":
                    case "add-friend":
                        this.sendFriendshipRequest();
                        break;
                    case "list":
                    case "list-friends":
                        System.out.println("Called list");
                        break;
                    case "challenge":
                        System.out.println("Challenge called");
                        break;
                    case "score":
                        this.sendScoreRequest();
                        break;
                    case "show":
                    case "show-rankings":
                    case "show-ranking-list":
                        System.out.println("Ranking list");
                        break;
                    case "logout":
                        System.out.println("Logging you out..");
                        this.sendLogout();
                        break;
                    case "exit":
                        System.out.println("Exiting!");
                        this.socket.close();
                        break;
                }
            } while (!(this.commandAndParams != null
                    && this.commandAndParams.length == 1
                    && this.commandAndParams[0].equalsIgnoreCase("exit")));

        } catch (RemoteException e) {
            System.out.println("[Client] Error in client: " + e.getMessage());
        } catch (NotBoundException e) {
            System.out.println("[Client] Remote object not found: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("[Client] Client successfully closed.");
    }

    private RegistrationRemoteService getRMIService() throws RemoteException, NotBoundException {
        if (this.registrationService == null) {
            Registry registry = LocateRegistry.getRegistry(
                    RegistrationRemoteService.REGISTRY_HOST,
                    RegistrationRemoteService.REGISTRY_PORT);

            this.registrationService = (RegistrationRemoteService)
                    registry.lookup(RegistrationRemoteService.REMOTE_OBJECT_NAME);
        }
        return this.registrationService;
    }

    private void performHelp() {
        System.out.println("Available Commands:");
        System.out.println("- register <nickname> <password>: registers the user to WQ with the given credentials");
        System.out.println("- login <nickname> <password>");
        System.out.println("- logout");
        System.out.println("- add-friend <nickFriend>: sets a friendship between this user and nickFriend");
        System.out.println("- list-friends: lists all friends of this user");
        System.out.println("- challenge: <nickFriend> requests a challenge to nickFriend");
        System.out.println("- score: gets total user score");
        System.out.println("- show-ranking-list: shows the ranking list including only this user and his/her friends");
        System.out.println("- exit");
        System.out.println();
    }

    private void performRegister() throws RemoteException, NotBoundException {
        if (!this.validateParams(2)) return;
        // 1. Get the new nickname
        String nickName = commandAndParams[1];
        // Get the password
        String password = commandAndParams[2];
        RegistrationResponseStatusCode responseCode =
                this.getRMIService()
                        .addUser(nickName, password);
        switch (responseCode) {
            case OK:
                System.out.println("You successfully complete the registration to WQ!");
                break;
            case INVALID_NICK_ERROR:
                System.out.println("The provided nick is invalid");
                break;
            case INVALID_PASSWORD_ERROR:
                System.out.println("The password must be at least 4 character long");
                break;
            case NICK_ALREADY_REGISTERED_ERROR:
                System.out.println("The nickname is already registered to WQ :(");
                break;
            case INTERNAL_ERROR:
            default:
                System.out.println("The server has experienced an internal error");
                break;
        }
    }

    private void sendLogin() throws ClosedChannelException {
        if (this.validateParams(2)) {
            this.connection.setPacketToWrite(new WQPacket(
                    OperationCode.LOGIN,
                    commandAndParams[1] + " " + commandAndParams[2]
            ));
            this.socket.register(selector, SelectionKey.OP_WRITE);
            this.canTakeCommand = false;
        }
    }

    private void sendLogout() throws ClosedChannelException {
        if (this.validateParams(0)) {
            this.connection.setPacketToWrite(new WQPacket(
                    OperationCode.LOGOUT, ""
            ));
            this.socket.register(selector, SelectionKey.OP_WRITE);
            this.canTakeCommand = false;
        }
    }

    private void sendFriendshipRequest() throws ClosedChannelException {
        if (this.validateParams(1)) {
            this.connection.setPacketToWrite(new WQPacket(
                    OperationCode.ADD_FRIEND, this.commandAndParams[1]
            ));
            this.socket.register(selector, SelectionKey.OP_WRITE);
            this.canTakeCommand = false;
        }
    }

    private void sendScoreRequest() throws ClosedChannelException {
        if (this.validateParams(0)) {
            this.connection.setPacketToWrite(new WQPacket(
                    OperationCode.GET_SCORE, ""
            ));
            this.socket.register(selector, SelectionKey.OP_WRITE);
            this.canTakeCommand = false;
        }
    }

    private boolean validateParams(int number) {
        if (this.commandAndParams == null || this.commandAndParams.length != number + 1) {
            System.out.println("Invalid parameters");
            return false;
        }
        return true;
    }

    private void manageCommunication() throws IOException, InterruptedException {
        selector.select();
        for (SelectionKey key : selector.selectedKeys()) {
            if (key.isConnectable()) {
                if (!socket.finishConnect())
                    socket.register(selector, SelectionKey.OP_CONNECT);
                else {
                    System.out.println("Client Connected");
                }
            }
            if (key.isReadable()) {
                System.out.println("Reading..");
                // Read answer from server and prints result.
                ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE);
                int read = socket.read(buf);
                if (read == -1) {
                    System.out.println("Socket closed.");
                    // Server side of the socket is closed.
                    // Close the socket too.
                    socket.close();
                    System.out.println("The server is unreachable!");
                    Thread.sleep(500);
                    return;
                } else {
                    buf.flip();
                    boolean hasReadAllPacket = this.connection.addChunk(buf);
                    if (hasReadAllPacket) {
                        WQPacket wqPacket = this.connection.buildWQPacketFromChucks();
                        System.out.println("Response for op: "
                                + wqPacket.getOpCode()
                                + "\nBody: "
                                + wqPacket.getBody()
                        );
                        // Enable user interaction.
                        this.canTakeCommand = true;
                        // It will set interest to write on the socket after
                        // receiving a command from the terminal.
                    } else {
                        socket.register(selector, SelectionKey.OP_READ);
                    }
                }
            }
            if (key.isWritable()) {
                System.out.println("Writing..");
                socket.write(this.connection.getPacketToWrite());
                if (this.connection.getPacketToWrite().hasRemaining())
                    // Waits to write the next part of the buffer
                    socket.register(selector, SelectionKey.OP_WRITE);
                else {
                    // Client will wait for an answer from the server
                    socket.register(selector, SelectionKey.OP_READ);
                }
            }
        }
        selector.selectedKeys().clear();
    }

}
