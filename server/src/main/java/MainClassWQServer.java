import RMIRegistrationService.RegistrationRemoteService;
import connection.Registration;
import connection.Connection;
import protocol.OperationCode;
import protocol.ResponseCode;
import protocol.WQPacket;
import storage.RegistrationRegistry;
import storage.UserStorage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.PriorityBlockingQueue;


/**
 * The server main thread reads/writes packets to/from the clients
 * through multiplexing via selector.
 * The tasks are instead executed asynchronously by the ForJoinCommonPool.
 */
class MainClassWQServer {

    private final int BUFFER_SIZE = 512;
    private final Queue<Registration> registrationQueue = new PriorityBlockingQueue<>();

    ServerSocketChannel socket;
    Selector selector;
    Registry registry;

    MainClassWQServer() throws IOException {

        try {
            System.out.println("[Server] Start SERVER....");
            RegistrationRemoteService remoteService = RegistrationRegistry.getInstance();
            // export RMI registration service
            RegistrationRemoteService stub =
                    (RegistrationRemoteService) UnicastRemoteObject.exportObject(remoteService, 0);
            // register to RMI registry
            registry = LocateRegistry.createRegistry(
                    RegistrationRemoteService.REGISTRY_PORT
            );
            registry.rebind(RegistrationRemoteService.REMOTE_OBJECT_NAME, stub);
        } catch (RemoteException e) {
            System.out.println("[Server] Error setting up RMI server: " + e.getMessage());
            return;
        }

        socket = ServerSocketChannel.open();
        socket.bind(new InetSocketAddress(6789));
        socket.configureBlocking(false);
        selector = Selector.open();
        socket.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("[Server] Finished server setup.");
    }

    public void run() {
        // TODO put termination condition from client
        while(true) {
            try {

                Registration registration = this.registrationQueue.poll();
                while (registration != null) {
                    registration.getClient().register(
                            selector,
                            registration.getSelectionKey(),
                            registration.getConnectionState()
                    );
                    registration = this.registrationQueue.poll();
                }

                selector.select();
                for (SelectionKey key : selector.selectedKeys()) {
                    try {
                        if (key.isAcceptable()) {
                            accept(key);
                        }
                        if (key.isReadable()) {
                            read(key);
                        }
                        if (key.isWritable()) {
                            write(key);
                        }
                    } catch (CancelledKeyException e) {
                        System.out.println("Cancelled key ");
                        key.channel().close();
                    }
                }
                selector.selectedKeys().clear();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void write(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        Connection state = (Connection) key.attachment();
        System.out.println("Write for client " + state.getClientNick());
        ByteBuffer toSend = state.getPacketToWrite();
        client.write(toSend);
        if (toSend.hasRemaining()) {
            // Must finish to write the response
            client.register(selector, SelectionKey.OP_WRITE, state);
        } else {
            System.out.println("Client registered for reading " + state.getClientNick());
            // state.setPacketToWrite(null);
            client.register(selector, SelectionKey.OP_READ, state);
        }
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        Connection clientConnection = (Connection) key.attachment();
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        int read = client.read(buffer);
        if (read == -1) {
            // The client-server direction has been closed.
            // The request packet has been arrived.
            // Process it asynchronously and, once a response is built,
            // register interest for writing to the client.
            try {
                WQPacket packet = clientConnection.buildWQPacketFromChucks();
                this.processCommand(packet, client, clientConnection);
            } catch (IllegalStateException e) {
                if (clientConnection.getClientNick() != null) {
                    // If already online I must remove it also from the storage.
                    CompletableFuture.runAsync(() ->
                            UserStorage.getInstance()
                                    .logOutUser(clientConnection.getClientNick()));

                }
            }
            // Close the connection.
            client.close();
        } else if (read == 0) {
            System.out.println("Letti 0" + clientConnection.getClientNick());
        } else {
            // Prepare buffer to be parsed.
            buffer.flip();
            // Add buffer to those that will be parsed and returns true if the packet is
            // completely arrived.
            boolean hasReadAllPacket = clientConnection.addChunk(buffer);
            if (hasReadAllPacket) {
                WQPacket packet = clientConnection.buildWQPacketFromChucks();
                System.out.println("Received packet " + packet.getOpCode() + packet.getBody());
                this.processCommand(packet, client, clientConnection);
            } else {
                // Synchronously register read interest for the client
                // It should finish to read the request packet.
                client.register(selector,
                        SelectionKey.OP_READ,
                        clientConnection
                );
            }
        }
    }

    private void accept(SelectionKey key) throws IOException {
        SocketChannel client = socket.accept();
        System.out.println("New Client connected");
        client.configureBlocking(false);
        Connection state = new Connection();
        // The server will wait for client's commands
        client.register(selector, SelectionKey.OP_READ, state);
    }

    /**
     * An async executor of operations.
     * It executes a command given in a packet and once the computation has finished
     * it may compile a response (based upon the results obtained) and registers the
     * interest to write back to the client or closes the connection.
     * @param packet
     * @param client
     * @param state
     * @throws ClosedChannelException
     */
    private void processCommand(
            final WQPacket packet,
            final SocketChannel client,
            final Connection state
    ) throws ClosedChannelException {
        // Checks that the packet has all the parameters expected.
        final String[] requestParameters = packet.getParameters();
        final OperationCode operationCode = packet.getOpCode();
        if (!Connection.isWellFormedRequestPacket(requestParameters, operationCode)) {
            state.setPacketToWrite(new WQPacket(packet.getOpCode(), ResponseCode.ERROR.name()));
            client.register(selector, SelectionKey.OP_WRITE, state);
        } else {
            // Process async commands
            switch (operationCode) {
                case LOGIN:
                    if (state.getClientNick() != null) {
                        // If this connection has already a user do not accept login op.
                        state.setPacketToWrite(new WQPacket(
                                OperationCode.LOGIN,
                                ResponseCode.ERROR.name()
                        ));
                        client.register(selector, SelectionKey.OP_WRITE, state);
                    } else {
                        CompletableFuture.supplyAsync(() -> UserStorage.getInstance()
                                .logInUser(requestParameters[0], requestParameters[1])
                        ).whenComplete((succeed, ex) -> {
                            if (ex != null) {
                                // Set this client connection information.
                                state.setClientNick(requestParameters[0]);
                            }
                            // Prepare the answer.
                            state.setPacketToWrite(new WQPacket(
                                    OperationCode.LOGIN,
                                    ex != null && succeed
                                            ? ResponseCode.OK.name()
                                            : ResponseCode.ERROR.name()
                            ));
                            this.registrationQueue.offer(new Registration(
                                    client,
                                    SelectionKey.OP_WRITE,
                                    state
                            ));
                            selector.wakeup();
                        });
                    }
                    break;
                case LOGOUT:
                    CompletableFuture.supplyAsync(() -> UserStorage.getInstance()
                            .logOutUser(state.getClientNick())
                    ).thenAccept((succeed) -> {
                        System.out.println("Logging out");
                        state.setPacketToWrite(new WQPacket(
                                OperationCode.LOGOUT,
                                succeed
                                        ? ResponseCode.OK.name()
                                        : ResponseCode.ERROR.name()
                        ));
                        state.setClientNick(null);
                        this.registrationQueue.offer(new Registration(
                                client,
                                SelectionKey.OP_WRITE,
                                state
                        ));
                        selector.wakeup();
                        // Disconnect the client only when the client closes the socket connection.
                    });
                    break;
                case ADD_FRIEND:
                    CompletableFuture.supplyAsync(() -> UserStorage.getInstance()
                            .addFriend(state.getClientNick(), requestParameters[0])
                    ).thenAccept(succeed -> {
                        state.setPacketToWrite(new WQPacket(
                                OperationCode.ADD_FRIEND,
                                succeed
                                        ? ResponseCode.OK.name()
                                        : ResponseCode.ERROR.name()
                        ));
                        this.registrationQueue.offer(new Registration(
                                client,
                                SelectionKey.OP_WRITE,
                                state
                        ));
                        selector.wakeup();
                    });
                    break;
                case GET_FRIENDS:
                    // TODO answer with JSON
                    break;
                case REQUEST_CHALLENGE:
                    // TODO
                    break;
                case GET_SCORE:
                    // It can be performed synchronously since the user
                    // is already loaded from the file.
                    try {
                        int score = UserStorage.getInstance().getScore(state.getClientNick());
                        state.setPacketToWrite(new WQPacket(
                                OperationCode.GET_SCORE,
                                ResponseCode.OK.name() + " " + score
                        ));
                    } catch (NoSuchElementException e) {
                        state.setPacketToWrite(new WQPacket(
                                OperationCode.GET_SCORE,
                                ResponseCode.ERROR.name()
                        ));
                    }
                    client.register(selector, SelectionKey.OP_WRITE, state);
                    break;
                case GET_RANKING:
                    // TODO JSON
                    break;
                case FORWARD_CHALLENGE:
                    break;
                case ASK_WORD:
                    break;
            }
        }
    }

    public static void main(String [] args) throws IOException {
        MainClassWQServer s = new MainClassWQServer();
        s.run();
    }
}