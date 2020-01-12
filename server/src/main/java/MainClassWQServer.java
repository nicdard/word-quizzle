import connection.Connection;
import protocol.OperationCode;
import protocol.ResponseCode;
import protocol.WQPacket;
import storage.UserStorage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.concurrent.CompletableFuture;

class MainClassWQServer {

    private final int BUFFER_SIZE = 512;

    ServerSocketChannel socket;
    Selector selector;

    MainClassWQServer() throws IOException {
        socket = ServerSocketChannel.open();
        socket.bind(new InetSocketAddress(6789));
        socket.configureBlocking(false);
        selector = Selector.open();
        socket.register(selector, SelectionKey.OP_ACCEPT);
    }

    public void run() {
        // TODO put termination condition from client
        while(true) {
            try {
                selector.select();
                for (SelectionKey key : selector.selectedKeys()) {
                    if (key.isAcceptable()){
                        accept(key);
                    }
                    if (key.isReadable()){
                        read(key);
                    }
                    if (key.isWritable()){
                        write(key);
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
        ByteBuffer toSend = state.getPacketToWrite();
        client.write(toSend);
        if (toSend.hasRemaining()) {
            // Must finish to write the response
            client.register(selector, SelectionKey.OP_WRITE, state);
        } else {
            state.setPacketToWrite(null);
            client.register(selector, SelectionKey.OP_READ, state);
        }
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        Connection clientConnection = (Connection) key.attachment();
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        int read = client.read(buffer);
        if (read == -1) {
            // The request packet has been arrived.
            // Process it asynchronously and, once a response is built,
            // register interest for writing to the client.
            WQPacket packet = clientConnection.buildWQPacketFromChucks();
            this.processCommand(packet, client, clientConnection);
        } else {
            // Prepare buffer to be parsed
            buffer.flip();
            // Add buffer to those that will be parsed
            clientConnection.addChunk(buffer);
            // Synchronously register read interest for the client
            // It should finish to read the request packet.
            client.register(selector,
                    SelectionKey.OP_READ,
                    clientConnection
            );
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
                    CompletableFuture.supplyAsync(() -> UserStorage.getInstance()
                            .logInUser(requestParameters[0], requestParameters[1])
                    ).thenAccept(succeed -> {
                        // Set this client connection information.
                        state.setClientNick(requestParameters[0]);
                        // Prepare the answer.
                        state.setPacketToWrite(new WQPacket(
                                OperationCode.LOGIN,
                                succeed
                                        ? ResponseCode.OK.name()
                                        : ResponseCode.ERROR.name()
                        ));
                        try {
                            client.register(selector, SelectionKey.OP_WRITE, state);
                        } catch (ClosedChannelException e) {
                            e.printStackTrace();
                        }
                    });
                    break;
                case LOGOUT:
                    CompletableFuture.runAsync(() -> UserStorage.getInstance()
                            .logOutUser(state.getClientNick())
                    ).thenRun(() -> {
                        // Disconnect the client.
                        try {
                            client.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                    break;
                case ADD_FRIEND:
                    CompletableFuture.supplyAsync(() -> UserStorage.getInstance()
                            .addFriend(state.getClientNick(), requestParameters[1])
                    ).thenAccept(succeed -> {
                        state.setPacketToWrite(new WQPacket(
                                OperationCode.ADD_FRIEND,
                                succeed
                                        ? ResponseCode.OK.name()
                                        : ResponseCode.ERROR.name()
                        ));
                        try {
                            client.register(selector, SelectionKey.OP_WRITE, state);
                        } catch (ClosedChannelException e) {
                            e.printStackTrace();
                        }
                    });
                    break;
                case GET_FRIENDS:
                    // TODO answer with JSON
                    break;
                case REQUEST_CHALLENGE:
                    // TODO
                    break;
                case GET_SCORE:
                    CompletableFuture.supplyAsync(() -> UserStorage.getInstance()
                            .getScore(state.getClientNick())
                    ).whenComplete((score, ex) -> {
                        state.setPacketToWrite(new WQPacket(
                                OperationCode.ADD_FRIEND,
                                ex != null
                                        ? ResponseCode.ERROR.name()
                                        : ResponseCode.OK.name() + " " + score
                        ));
                        try {
                            client.register(selector, SelectionKey.OP_WRITE, state);
                        } catch (ClosedChannelException e) {
                            e.printStackTrace();
                        }
                    });
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