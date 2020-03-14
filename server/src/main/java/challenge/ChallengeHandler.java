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

    private static final int PLAYERS = 2;

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
    /** Maps player name into his startTime and after into their completion time */
    private Map<String, Long> timeMap;
    /** Maps player name into his temporary scores. */
    private Map<String, Integer> scores;
    /** Maps player name in selectionKey */
    private Map<String, SelectionKey> keys;
    /** The list of the players for this challenge */
    private List<String> players;

    public ChallengeHandler(AsyncRegistrations mainRegistrationQueue, String originalRequester, String player2) {
        this.mainRegistrationQueue = mainRegistrationQueue;
        this.players = Arrays.asList(originalRequester, player2); // Save it for error handling.
        this.iteratorMap = new HashMap<>(PLAYERS);
        this.timeMap = new HashMap<>(PLAYERS);
        this.scores = new HashMap<>(PLAYERS);
        this.keys = new HashMap<>(PLAYERS);
        this.ended = false;
        this.error = false;
        try {
            selector = Selector.open();
            // Builds a dictionary for the challenge.
            this.dictionary = DictionaryService.getInstance().getDictionary(
                    Config.getInstance().getWordsForChallenge()
            );
            for (String player : players) {
                // Initialise list of words for each player.
                this.iteratorMap.put(player, this.dictionary.keySet().iterator());
                // Initialise scores for each player.
                this.scores.put(player, 0);
            }
        } catch (RuntimeException | IOException e) {
            Config.getInstance().debugLogger(e.getMessage());
            this.error = true;
        } finally {
            NotifierService.getInstance().clearPendingResponseEntry(originalRequester);
        }
    }

    public static String getChallengeRules() {
        return CHALLENGE_RULES;
    }

    @Override
    public void run() {
        // If an error occurred  while initialising.
        if (this.error) {
            this.onError();
            return;
        }
        try {
            // Read the setup packet responses to synchronize the clients before challenge start
            // and be sure that the previous ongoing main packet has been written to both.
            for (String player : this.players) {
                State state = NotifierService.getInstance()
                        .getConnection(player);
                // Register the socket into the new selector.
                SelectionKey selectionKey = state.getClient().register(this.selector, SelectionKey.OP_READ, state);
                this.keys.put(player, selectionKey);
            }
            // Stop the challenge if either the timeout is reached or an error occurred
            // or all users have already sent all responses.
            this.start = System.currentTimeMillis() / 1000;
            while (!ended && !error && userCompletionNumber < PLAYERS) {
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
                    } catch (CancelledKeyException | IOException e) {
                        Config.getInstance().debugLogger(e, "Challenge Thread.");
                        error = true;
                    }
                }
                selector.selectedKeys().clear();
                ended = System.currentTimeMillis() / 1000 - start > Config.getInstance().getChallengeTime();
                if (ended) {
                    this.players.forEach(p -> this.timeMap.putIfAbsent(p, System.currentTimeMillis()));
                }
            }

            if (error) {
                this.onError();
            } else {
                // Compute results and send results packets.
                List<String> ranking = this.scores.entrySet().stream()
                        .sorted((e1, e2) -> {
                            int orderByScore = e2.getValue().compareTo(e1.getValue());
                            if (orderByScore == 0) {
                                return this.timeMap.get(e2.getKey()).compareTo(this.timeMap.get(e1.getKey()));
                            } else return orderByScore;
                        })
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());

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
                            "The winner is " + winner // info about who wins
                            + ".\nYou got " + this.scores.get(p) + " points in this challenge."
                    ));
                    this.registerToMainSelector(p, wqPacket);
                });
            }
        } catch (IOException e) {
            Config.getInstance().debugLogger(e, "IO Exception in challenge thread!");
            this.onError();
        }
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        State clientConnection = (State) key.attachment();
        ByteBuffer buffer = ByteBuffer.allocate(512);
        int read = client.read(buffer);
        if (read == -1) {
            // Register in the main thread so it will deallocate the resources.
            this.mainRegistrationQueue.register(key, SelectionKey.OP_READ);
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
                this.processCommand(packet, key, clientConnection);
            } else {
                // Synchronously register read interest for the client
                // It should finish to read the request packet.
                key.interestOps(SelectionKey.OP_READ);
            }
        }
    }


    private void processCommand(PacketPojo packet,
                                SelectionKey client,
                                State state
    ) {
        if (!packet.isSuccessfullResponse()) {
            // Exit the challenge.
            this.error = true;
            return;
        }
        switch (packet.getOperationCode()) {
            case SETUP_CHALLENGE: {
                Config.getInstance().debugLogger("Client " + state.getClientNick() + " has received the setup packet and is now starting the challenge!");
                // Send the next one.
                next(state, client);
                break;
            }
            case ASK_WORD:
                long delta = this.start - System.currentTimeMillis() / 1000;
                if (delta < Config.getInstance().getChallengeTime()) {
                    // Checks if the translation is correct and in time; updates the user scores.
                    List<String> translations = this.dictionary.get(packet.getWord());
                    // A translation is considered valid if any of the translations from the
                    // Translation service matches the given one.
                    boolean isRight = translations.stream()
                            .anyMatch(t -> t.equalsIgnoreCase(packet.getTranslation()));
                    int score = this.scores.get(state.getClientNick());
                    if (isRight) {
                        // Add points to the user's score.
                        score += Config.getInstance().getWordBonus();
                    } else {
                        // Decrease user's score.
                        score -= Config.getInstance().getWordMalus();
                    }
                    this.scores.put(state.getClientNick(), score);
                } else {
                    // Timeout reached do not count the translation for the scores.
                    // Do nothing just set the challenge termination.
                    this.ended = true;
                }
                // Send the next one or wait for battle to finish.
                next(state, client);
                break;
            default:
                System.out.println("Ignore other packets during challenge!");
                client.interestOps(SelectionKey.OP_READ);
        }
    }

    private void next(State state, SelectionKey client) {
        Iterator<String> iterator = this.iteratorMap.get(state.getClientNick());
        if (iterator.hasNext()) {
            WQPacket wqPacket = new WQPacket(PacketPojo.buildAskWordRequest(iterator.next()));
            state.setPacketToWrite(wqPacket);
            // Send the next one.
            client.interestOps(SelectionKey.OP_WRITE);
        } else {
            // Do nothing. Wait until all players have finished.
            userCompletionNumber++;
            // Sets client completion time.
            this.timeMap.put(state.getClientNick(), System.currentTimeMillis());
        }
    }

    private void write(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        State state = (State) key.attachment();
        ByteBuffer toSend = state.getPacketToWrite();
        client.write(toSend);
        if (toSend.hasRemaining()) {
            // Must finish to write the response
            key.interestOps(SelectionKey.OP_WRITE);
        } else {
            state.setPacketToWrite(null);
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    /**
     * Cleaning method used to send an error message to every client
     * involved in this challenge and setting back the mainKey selectable in the
     * main server selector.
     */
    private void onError() {
        final WQPacket wqPacket = new WQPacket(new PacketPojo(
                OperationCode.STOP_CHALLENGE,
                ResponseCode.ERROR,
                "Unexpected exception"
        ));
        // Use players to be sure no one is missing.
        // Note: It is really the array of players given by the constructor.
        for (String player : this.players) {
            registerToMainSelector(player, wqPacket);
        }
    }

    private void registerToMainSelector(String player, WQPacket wqPacket) {
        // Cancel the challenge thread key interests if already registered.
        SelectionKey key = this.keys.get(player);
        if (key != null && key.isValid()) key.interestOps(0);
        // Set interests in the main key.
        State state = NotifierService.getInstance()
                .getConnection(player);
        state.setMainReadSelectable(true);
        state.setPacketToWrite(wqPacket);
        this.mainRegistrationQueue.register(
                state.getMainKey(),
                SelectionKey.OP_WRITE
        );
    }
}
