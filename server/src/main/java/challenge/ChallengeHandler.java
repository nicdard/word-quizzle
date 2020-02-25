package challenge;

import configurations.Config;
import connection.AsyncRegistrations;
import connection.State;
import protocol.OperationCode;
import protocol.ResponseCode;
import protocol.WQPacket;
import protocol.json.PacketPojo;
import storage.UserStorage;
import translation.DictionaryService;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.stream.Collectors;

public class ChallengeHandler implements Runnable {

    private static final String CHALLENGE_RULES = "You and your opponent will have " + Config.getInstance().getChallengeTime() + " seconds to translate " + Config.getInstance().getWordsForChallenge() + " words.";

    /**
     * The registration queue of the main selector: used to reinsert the clients
     * in the main flow after the challenge.
     */
    private AsyncRegistrations mainRegistrationQueue;
    /** The dictionary for this challenge. */
    private Map<String, List<String>> dictionary;
    /**
     * The selector for this challenge -> different from the main selector
     * because the communication starts from the server to the client.
     */
    private Selector selector;
    /** start time of the challenge */
    private long start;
    /** Stops the thread selector loop when the timeout is reached */
    private boolean ended;
    /** Stops the thread selector loop if an error occurred */
    private boolean error;
    /** The number of users which completes the challenge */
    private int userCompletionNumber = 0;
    /** Maps players name into words iterators */
    private Map<String, Iterator<String>> iteratorMap;
    /** Maps player name into their startTimes and after into their completion time */
    private Map<String, Long> timeMap;
    /** Maps player name into their temporary scores. */
    private Map<String, Integer> scores;

    public ChallengeHandler(AsyncRegistrations mainRegistrationQueue, String ...players) {
        this.mainRegistrationQueue = mainRegistrationQueue;
        this.iteratorMap = new HashMap<>(2);
        this.timeMap = new HashMap<>(2);
        this.scores = new HashMap<>(2);
        this.ended = false;
        try {
            selector = Selector.open();
            // Builds a dictionary for the challenge.
            this.dictionary = DictionaryService.getInstance().getDictionary(
                    Config.getInstance().getWordsForChallenge()
            );
            for (String player : players) {
                this.iteratorMap.put(player, this.dictionary.keySet().iterator());
            }
        } catch (NoSuchElementException | NullPointerException | IOException e) {
            this.error = true;
        }
    }

    public static String getChallengeRules() {
        return CHALLENGE_RULES;
    }

    @Override
    public void run() {
        try {
            // Read the setup packet responses to synchronize the clients before challenge start
            // and be sure that the previous ongoing main packet has been written to both.
            for (String player : this.iteratorMap.keySet()) {
                State state = NotifierService.getInstance()
                        .getConnection(player);
                state.getClient().register(this.selector, SelectionKey.OP_READ, state);
            }
            // Stop the challenge if either the timeout is reached or an error occurred
            // or all users have already sent all responses.
            this.start = System.currentTimeMillis() / 1000;
            while (!ended || !error || userCompletionNumber != 2) {
                // Every second maximum checks if the timer has expired.
                selector.select(1000);
                for (SelectionKey key : selector.selectedKeys()) {
                    try {
                        if (key.isReadable()) {
                            read(key);
                        }
                        if (key.isWritable()) {
                            write(key);
                        }
                    } catch (IOException e) {
                        error = true;
                    }
                }
                selector.selectedKeys().clear();
                ended = System.currentTimeMillis() / 1000 - start > Config.getInstance().getChallengeTime();
            }

            if (error) {
                for (String player : this.iteratorMap.keySet()) {
                    State state = NotifierService.getInstance()
                            .getConnection(player);
                    state.setMainReadSelectable(true);
                    state.setPacketToWrite(new WQPacket(new PacketPojo(
                            OperationCode.STOP_CHALLENGE,
                            ResponseCode.ERROR,
                            "Unexpected exception"
                    )));
                    this.mainRegistrationQueue.register(
                            state.getClient(),
                            SelectionKey.OP_WRITE,
                            state
                    );
                }
            } else {
                // Compute results and send results packets.
                List<String> ranking = this.scores.entrySet().stream()
                        .sorted(Comparator.comparing(Map.Entry::getValue))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());
                if (ranking.size() >= 2) {
                    String winner = ranking.get(0);
                    this.scores.put(
                            winner,
                            this.scores.get(winner)
                                    + Config.getInstance().getWinnerExtraPoints()
                    );
                    // Update scores and set final message for all participants;
                    // register back to the main selector.
                    ranking.forEach(p -> {
                        // 1. Update scores.
                        UserStorage.getInstance().updateUserScore(
                                p,
                                this.scores.get(p)
                        );
                        // 2. Set message.
                        WQPacket wqPacket = new WQPacket(new PacketPojo(
                                OperationCode.STOP_CHALLENGE,
                                ResponseCode.OK,
                                winner // info about who wins
                        ));
                        State state = NotifierService.getInstance()
                                .getConnection(p);
                        state.setPacketToWrite(wqPacket);
                        // 3. Register back in the main server selector.
                        state.setMainReadSelectable(true);
                        this.mainRegistrationQueue.register(
                                state.getClient(),
                                SelectionKey.OP_WRITE,
                                state
                        );
                    });
                }
            }
        } catch (IOException e) {
            // TODO
            e.printStackTrace();
        }
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        State clientConnection = (State) key.attachment();
        ByteBuffer buffer = ByteBuffer.allocate(512);
        int read = client.read(buffer);
        if (read == -1) {
            if (clientConnection.isAssigned()) {
                // If already online I must remove it also from the storage.
                UserStorage.getInstance()
                        .logOutUser(clientConnection.getClientNick());
                NotifierService.getInstance()
                        .removeConnection(clientConnection.getClientNick());
                clientConnection.reset();
            }
            // Close the connection.
            client.close();
        } else {
            // Prepare buffer to be parsed.
            buffer.flip();
            // Add buffer to those that will be parsed and returns true if the packet is
            // completely arrived.
            boolean hasReadAllPacket = clientConnection.addChunk(buffer);
            if (hasReadAllPacket) {
                PacketPojo packet = clientConnection.buildWQPacketFromChucks();
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


    private void processCommand(PacketPojo packet,
                                SocketChannel client,
                                State state
    ) throws ClosedChannelException {
        if (!packet.isSuccessfullResponse()) {
            // Exit the challenge.
            this.error = true;
            return;
        }
        switch (packet.getOperationCode()) {
            case SETUP_CHALLENGE: {
                Config.getInstance().debugLogger("Client " + state.getClientNick() + " has received the setup packet and is now starting the challenge!");
                // Sets the client start time.
                this.timeMap.put(state.getClientNick(), System.currentTimeMillis() / 1000);
                // Initialise the scores.
                this.scores.put(state.getClientNick(), 0);
                // Send the next one.
                next(state, client);
                break;
            }
            case ASK_WORD:
                long delta = this.timeMap.get(state.getClientNick()) - System.currentTimeMillis() / 1000;
                if (delta < Config.getInstance().getChallengeTime()) {
                    // Checks if the translation is correct and in time; updates the user scores.
                    List<String> translations = this.dictionary.get(state.getClientNick());
                    // A translation is considered valid if any of the translations from the
                    // Translation service matches the given one.
                    boolean isRight = translations.stream()
                            .anyMatch(t -> t.equalsIgnoreCase(packet.getWord()));
                    int score = this.scores.get(state.getClientNick());
                    if (isRight) {
                        // Add points to the user's score.
                        score += Config.getInstance().getWordBonus();
                    } else {
                        // Decrease the user's score.
                        score -= Config.getInstance().getWordMalus();
                    }
                    this.scores.put(state.getClientNick(), score);
                    // Send the next one.
                    next(state, client);
                } else {
                    // Timeout reached do not count the translation for the scores.
                    // Do nothing.
                    this.ended = true;
                }
                break;
            default:
                System.out.println("Ignore other packets during challenge!");
                client.register(selector, SelectionKey.OP_READ, state);
        }
    }

    private void next(State state, SocketChannel client) throws ClosedChannelException {
        Iterator<String> iterator = this.iteratorMap.get(state.getClientNick());
        if (iterator.hasNext()) {
            WQPacket wqPacket = new WQPacket(PacketPojo.buildAskWordRequest(iterator.next()));
            state.setPacketToWrite(wqPacket);
            // Send the next one.
            client.register(selector, SelectionKey.OP_WRITE, state);
        } else {
            // Do nothing. Wait until all players have finished.
            userCompletionNumber++;
            this.timeMap.put(state.getClientNick(),
                    System.currentTimeMillis() / 1000 - this.timeMap.get(state.getClientNick())
            );
        }
    }

    private void write(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        State state = (State) key.attachment();
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

    /**
     * Cleaning method used to send an error message to every client
     * involved in this challenge and register back all sockets in the
     * main server selector.
     */
    private void onError() {

    }

    /**
     * Registers the clients back in the main selector.
     */
    private void exit() {

    }

}
