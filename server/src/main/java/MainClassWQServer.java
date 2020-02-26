import RMIRegistrationService.RegistrationRemoteService;
import challenge.ChallengeHandler;
import challenge.NotifierService;
import connection.AsyncRegistrations;
import connection.State;
import protocol.Config;
import protocol.OperationCode;
import protocol.ResponseCode;
import protocol.WQPacket;
import protocol.json.PacketPojo;
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
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


/**
 * The server main thread reads/writes packets to/from the clients
 * through multiplexing via selector.
 * The tasks are instead executed asynchronously by the ForJoinCommonPool.
 */
class MainClassWQServer {

    private final int BUFFER_SIZE = 512;

    ServerSocketChannel socket;
    Selector selector;
    Registry registry;
    private AsyncRegistrations asyncRegistrations;

    MainClassWQServer() throws IOException {

        try {
            System.out.println("Starting!");
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
        socket.bind(new InetSocketAddress(Config.TCP_PORT));
        socket.configureBlocking(false);
        selector = Selector.open();
        asyncRegistrations = new AsyncRegistrations(selector);
        socket.register(selector, SelectionKey.OP_ACCEPT);
        // Sets up udp socket.
        NotifierService.getInstance();

        System.out.println("[Server] Finished server setup.");
    }

    public void run() {
        // TODO put termination condition from client
        while(true) {
            try {
                this.asyncRegistrations.callAll();
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
                        configurations.Config.getInstance().debugLogger("Cancelled key");
                        if (key.attachment() != null) {
                            State state = (State) key.attachment();
                            String nick = state.getClientNick();
                            if (nick != null) {
                                UserStorage.getInstance().logOutUser(nick);
                                NotifierService.getInstance().removeConnection(nick);
                            }
                        }
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
        State state = (State) key.attachment();
        configurations.Config.getInstance().debugLogger("Write for client " + state.getClientNick());
        ByteBuffer toSend = state.getPacketToWrite();
        client.write(toSend);
        if (toSend.hasRemaining()) {
            // Must finish to write the response
            key.interestOps(SelectionKey.OP_WRITE);
        } else {
            state.setPacketToWrite(null);
            if (state.isMainReadSelectable()) {
                configurations.Config.getInstance().debugLogger(
                        "Client registered for reading "
                                + state.getClientNick()
                );
                key.interestOps(SelectionKey.OP_READ);
            } else {
                // Rest interests in this selector.
                key.interestOps(0);
            }
        }
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        State clientConnection = (State) key.attachment();
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        int read = client.read(buffer);
        if (read == -1) {
            // The client-server direction has been closed.
            // Close the connection too.
            client.close();
        } else {
            // Prepare buffer to be parsed.
            buffer.flip();
            // Add buffer to those that will be parsed and returns true if the packet is
            // completely arrived.
            boolean hasReadAllPacket = clientConnection.addChunk(buffer);
            if (hasReadAllPacket) {
                PacketPojo packet = clientConnection.buildWQPacketFromChucks();
                configurations.Config.getInstance().debugLogger("Received packet " + packet.getOperationCode());
                this.processCommand(packet, key, clientConnection);
            } else {
                // Synchronously register read interest for the client
                // It should finish to read the request packet.
                key.interestOps(SelectionKey.OP_READ);
            }
        }
    }

    private void accept(SelectionKey key) throws IOException {
        SocketChannel client = socket.accept();
        configurations.Config.getInstance().debugLogger("New Client connected!");
        client.configureBlocking(false);
        State state = new State(client, key);
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
            final PacketPojo packet,
            final SelectionKey client,
            final State state
    ) {
        // Checks that the packet has all the parameters expected.
        final OperationCode operationCode = packet.getOperationCode();
        if (packet.isWellFormedRequestPacket() || packet.isSuccessfullResponse()) {
            // Process async commands
            switch (operationCode) {
                case LOGIN:
                    if (state.isAssigned()) {
                        // If this connection has already a user do not accept login op.
                        state.setPacketToWrite(new WQPacket(
                                new PacketPojo(operationCode, ResponseCode.ERROR, "This connection is already assigned to: " + state.getClientNick()))
                        );
                        client.interestOps(SelectionKey.OP_WRITE);
                    } else {
                        CompletableFuture.supplyAsync(() -> UserStorage.getInstance()
                                .logInUser(packet.getNickName(), packet.getPassword())
                        ).whenComplete((succeed, ex) -> {
                            if (ex == null && succeed) {
                                // Set this client connection information.
                                state.setClientNick(packet.getNickName());
                                state.setUDPPort(packet.getUDPPort());
                                NotifierService.getInstance().addConnection(packet.getNickName(), state);
                            }
                            // Prepare the answer.
                            state.setPacketToWrite(new WQPacket(new PacketPojo(packet.getOperationCode(),
                                    ex == null && succeed
                                            ? ResponseCode.OK
                                            : ResponseCode.ERROR
                            )));
                            this.asyncRegistrations.register(client, SelectionKey.OP_WRITE);
                        });
                    }
                    break;
                case LOGOUT:
                    CompletableFuture.supplyAsync(() -> UserStorage.getInstance()
                            .logOutUser(state.getClientNick())
                    ).thenAccept((succeed) -> {
                        if (succeed) {
                            System.out.println("Logging out");
                            // Remove
                            NotifierService.getInstance().removeConnection(state.getClientNick());
                            // Resets the connection.
                            state.reset();
                        }
                        // Sets the response packet.
                        state.setPacketToWrite(new WQPacket(new PacketPojo(
                                packet.getOperationCode(),
                                succeed
                                        ? ResponseCode.OK
                                        : ResponseCode.ERROR
                        )));
                        this.asyncRegistrations.register(client, SelectionKey.OP_WRITE);

                        // Disconnect the client only when the client closes the socket connection.
                    });
                    break;
                case ADD_FRIEND:
                    CompletableFuture.supplyAsync(() -> UserStorage.getInstance()
                            .addFriend(state.getClientNick(), packet.getFriend())
                    ).thenAccept(succeed -> {
                        state.setPacketToWrite(new WQPacket(new PacketPojo(
                                packet.getOperationCode(),
                                succeed
                                        ? ResponseCode.OK
                                        : ResponseCode.ERROR
                        )));
                        this.asyncRegistrations.register(client, SelectionKey.OP_WRITE);
                    });
                    break;
                case GET_FRIENDS:
                    try {
                        Set<String> friends =
                                UserStorage.getInstance().getFriends(state.getClientNick());
                        state.setPacketToWrite(new WQPacket(
                                PacketPojo.buildGetFriendsResponse(friends)
                        ));
                        client.interestOps(SelectionKey.OP_WRITE);
                    } catch (NoSuchElementException | IllegalArgumentException e) {
                        // Answers immediately if an exception occurs.
                        state.setPacketToWrite(new WQPacket(new PacketPojo(
                                packet.getOperationCode(),
                                ResponseCode.ERROR,
                                e.getMessage()
                        )));
                        client.interestOps(SelectionKey.OP_WRITE);
                    }
                    break;
                case GET_SCORE:
                    // It can be performed synchronously since the user
                    // is already loaded from the file.
                    try {
                        int scores = UserStorage.getInstance().getScore(state.getClientNick());
                        state.setPacketToWrite(new WQPacket(PacketPojo.buildScoreResponse(scores)));
                    } catch (NoSuchElementException e) {
                        state.setPacketToWrite(new WQPacket(new PacketPojo(
                                packet.getOperationCode(),
                                ResponseCode.ERROR)
                        ));
                    }
                    this.asyncRegistrations.register(client, SelectionKey.OP_WRITE);
                    break;
                case GET_RANKING:
                    CompletableFuture.supplyAsync(() -> UserStorage.getInstance()
                                .getRankingList(state.getClientNick())
                        ).whenComplete((list, ex) -> {
                        if (ex == null) {
                            try {
                                state.setPacketToWrite(new WQPacket(PacketPojo.buildRankingResponse(list)));
                            } catch (IllegalArgumentException e) {
                                state.setPacketToWrite(new WQPacket(new PacketPojo(
                                        packet.getOperationCode(),
                                        ResponseCode.ERROR,
                                        e.getMessage()
                                )));
                            }
                        } else {
                            state.setPacketToWrite(new WQPacket(new PacketPojo(
                                    packet.getOperationCode(),
                                    ResponseCode.ERROR
                            )));
                        }
                        this.asyncRegistrations.register(client, SelectionKey.OP_WRITE);
                    });
                    break;
                case REQUEST_CHALLENGE:
                    // Challenge contract checks.
                    try {
                        UserStorage storage = UserStorage.getInstance();
                        if (storage
                                .getFriends(state.getClientNick())
                                .contains(packet.getFriend())
                            && storage.isOnline(packet.getFriend())
                        ) {
                            boolean hasNotified = NotifierService.getInstance()
                                .notifyChallengeRequest(
                                    packet.getFriend(),
                                    state.getClientNick()
                                );
                            if (hasNotified) {
                                // Run in a separate thread so the whole server doesn't block
                                CompletableFuture.runAsync(() -> this.setup(client, state.getClientNick(), state, packet));
                            } else {
                                throw new IOException("Notifier could notify the user.");
                            }
                        } else {
                            throw new NoSuchElementException("Not online!");
                        }
                    } catch (IOException | NoSuchElementException e) {
                        state.setPacketToWrite(new WQPacket(new PacketPojo(
                                packet.getOperationCode(),
                                ResponseCode.ERROR
                        )));
                        client.interestOps(SelectionKey.OP_WRITE);
                    }
                    break;
                case FORWARD_CHALLENGE:
                    System.out.println("Forward " + state.getClientNick() + " sender " + packet.getFriend());
                    String sender = packet.getFriend();
                    if (packet.isSuccessfullResponse()) {
                        NotifierService.getInstance().setNotificationResponse(
                                sender,
                                new WQPacket(new PacketPojo(
                                        OperationCode.SETUP_CHALLENGE,
                                        ResponseCode.OK,
                                        ChallengeHandler.getChallengeRules()
                                ))
                        );
                        System.out.println("Accepted " + state.getClientNick() + " " + sender);
                        this.setup(client, sender, state, packet);
                        // Run the challenge thread.
                        ChallengeHandler challenge = new ChallengeHandler(this.asyncRegistrations, sender, state.getClientNick());
                        Thread executor = new Thread(challenge);
                        executor.start();
                    } else {
                        // Sets error packet so that the requester does not wait till the timeout.
                        NotifierService.getInstance().setNotificationResponse(
                                sender,
                                new WQPacket(new PacketPojo(
                                        OperationCode.SETUP_CHALLENGE,
                                        ResponseCode.ERROR,
                                        "The opponent refuses the challenge"
                                ))
                        );
                        System.out.println("Discarded " + state.getClientNick());
                        this.setup(client, sender, state, packet);
                    }
                    break;
                default:
                    System.out.println("Unhandled command! " + packet.getOperationCode());
                    // Register the client anyway.
                    client.interestOps(SelectionKey.OP_READ);
            }
        } else {
            state.setPacketToWrite(new WQPacket(
                    new PacketPojo(operationCode, ResponseCode.ERROR, "Malformed request")
            ));
            configurations.Config.getInstance().debugLogger("Malformed request from " + state.getClient() + " " + packet.getOperationCode());
            client.interestOps(SelectionKey.OP_WRITE);
        }
    }

    private void setup(SelectionKey client, String requester, State state, PacketPojo packet) {
        try {
            // Waits for the response.
            WQPacket response = NotifierService.getInstance()
                    .getResponse(requester);
            if (OperationCode.SETUP_CHALLENGE.equals(
                    response.getDeserializedBody().getOperationCode())
                    && response.getDeserializedBody().isSuccessfullResponse()
            ) {
                // Exclude selectable state for reading in the main selector.
                state.setMainReadSelectable(false);
            }
            state.setPacketToWrite(response);
        } catch ( ExecutionException
                | InterruptedException
                | NoSuchElementException e)
        {
            state.setPacketToWrite(new WQPacket(new PacketPojo(
                    packet.getOperationCode(),
                    ResponseCode.ERROR
            )));
        } finally {
            this.asyncRegistrations.register(
                    client,
                    SelectionKey.OP_WRITE
            );
        }
    }

    public static void main(String [] args) throws IOException {
        // Parse command line arguments.
        configurations.Config.getInstance().parseCommandLineArguments(args);
        MainClassWQServer s = new MainClassWQServer();
        s.run();
    }
}