package challenge;

import configurations.Config;
import connection.State;
import protocol.OperationCode;
import protocol.ResponseCode;
import protocol.WQPacket;
import protocol.json.PacketPojo;

import java.io.IOException;
import java.net.*;
import java.util.NoSuchElementException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Notifies a user over UDP. Used to forward request challenge.
 */
public class NotifierService {

    private static final class Responses {

        private CompletableFuture<WQPacket> response;
        private AtomicInteger participants;

        Responses(CompletableFuture<WQPacket> response) {
            this.response = response;
            this.participants = new AtomicInteger(0);
        }

        public AtomicInteger getParticipants() {
            return participants;
        }

        public CompletableFuture<WQPacket> getResponse() {
            return response;
        }
    }

    /**
     * The UDP socket used by the service to write packets to the clients.
     */
    private DatagramSocket udpSocket;
    /**
     * Maps nicknames of online logged users to their connection state.
     */
    private ConcurrentHashMap<String, State> connectionTable;
    /**
     * Stores pending responses of sent notifications.
     */
    private ConcurrentHashMap<String, Responses> pendingResponses;

    private static NotifierService instance;
    private NotifierService() {
        connectionTable = new ConcurrentHashMap<>();
        pendingResponses = new ConcurrentHashMap<>();
        try {
            udpSocket = new DatagramSocket(
                    protocol.Config.UDP_PORT,
                    InetAddress.getByName(protocol.Config.SERVER_NAME)
            );
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
            throw new IllegalStateException("UDP Connection error");
        }
    }
    public static NotifierService getInstance() {
        if (instance == null) {
            instance = new NotifierService();
        }
        return instance;
    }

    /**
     * Creates a new connection instance in the routing table.
     * @param nick
     * @param state
     */
    public void addConnection(String nick, State state) {
        connectionTable.putIfAbsent(nick, state);
    }

    public State getConnection(String nick) {
        return connectionTable.get(nick);
    }

    /**
     * Cleans the tables for the given nickname.
     * @param nick
     */
    public void removeConnection(String nick) {
        connectionTable.remove(nick);
        pendingResponses.remove(nick);
    }

    /**
     * Notifies dest for a challenge from sender. Waits (a limited amount of time)
     * for a response from dest and then send back to the sender the response.
     * @param dest
     * @param sender
     * @return false if either sender or dest are unknown or sender has a pending notification, true if a message was sent to dest.
     * @throws IOException
     * @throws NoSuchElementException
     */
    public boolean notifyChallengeRequest(final String dest, final String sender)
            throws IOException, NoSuchElementException
    {
        // Only one request per sender at a time is allowed.
        if (this.connectionTable.get(dest) == null
            || this.connectionTable.get(sender) == null
            || this.pendingResponses.get(sender) != null
        ) {
            return false;
        }
        // builds challenge request packet
        WQPacket wqPacket = new WQPacket(PacketPojo.buildForwardChallengeRequest(
                sender,
                Config.getInstance().getChallengeRequestTimeout()
        ));
        this.forwardMessage(dest, wqPacket);
        // If anyone will notify the sender before the timeout this future will complete
        // giving a DISCARD challenge response.
        this.setSenderNotificationTimeout(
                sender,
                OperationCode.REQUEST_CHALLENGE,
                ResponseCode.ERROR,
                Config.getInstance().getChallengeRequestTimeout()
        );
        return true;
    }

    /**
     * Writes a packet to dest over UDP.
     * @param dest
     * @param wqPacket
     * @throws IOException
     */
    public void forwardMessage(String dest, WQPacket wqPacket) throws IOException {
        State requesterState = this.connectionTable.get(dest);
        if (requesterState == null)
            throw new NoSuchElementException("The user is not logged-in");
        InetAddress ip = InetAddress.getByName(protocol.Config.SERVER_NAME);
        byte[] sendData = wqPacket.toBytes();
        DatagramPacket packet = new DatagramPacket(
                sendData,
                sendData.length,
                ip,
                requesterState.getUDPPort()
        );
        udpSocket.send(packet);
    }

    /**
     * Sets the packet to be sent as response for a notification if the timer has not expired.
     * @param sender
     * @param wqPacket
     * @return true if and only if the packet has been set.
     */
    public boolean setNotificationResponse(String sender, WQPacket wqPacket) {
        CompletableFuture<WQPacket> pendingResponse = this.pendingResponses.get(sender).getResponse();
        if (pendingResponse != null && !pendingResponse.isDone()) {
            // Replace wqPacket returned as response and completes the future
            // so the get call returns immediately. The challenge sender will unlock and
            // send the the correct response packet.
            return pendingResponse.complete(wqPacket);
        } else {
            return false;
        }
    }

    /**
     * Blocks the current thread
     * @param requester
     * @return the packet to be written to the requester.
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public WQPacket getResponse(String requester)
            throws ExecutionException, InterruptedException, NoSuchElementException
    {
        Responses response = this.pendingResponses.get(requester);
        CompletableFuture<WQPacket> timer = response.getResponse();
        if (timer != null) {
            WQPacket ret = timer.get();
            // Clear notification ongoing table.
            if (response.getParticipants().addAndGet(1) >= 2) {
                this.pendingResponses.remove(requester);
            }
            return ret;
        } else {
            throw new NoSuchElementException("An unexpected error occurred: probably some race condition occurred!");
        }
    }

    /**
     * Sets (and starts) a pending response timer for the given sender that will be available
     * after timeoutMillis ms if nobody fulfill it before the timeout expires.
     * @param sender
     * @param op
     * @param res
     * @param timeoutMillis
     */
    private void setSenderNotificationTimeout(final String sender,
                                      final OperationCode op,
                                      final ResponseCode res,
                                      final int timeoutMillis
    ) {
        // Starts the notification timer and saves a reference to it.
        CompletableFuture<WQPacket> timer = CompletableFuture.supplyAsync(() ->
        {
            try {
                Thread.sleep(timeoutMillis);
            } catch (InterruptedException e) {
                System.out.println("Challenge Notification timeout expired for request by " + sender);
            }
            return new WQPacket(new PacketPojo(
                    op,
                    res
            ));
        });
        this.pendingResponses.putIfAbsent(sender, new Responses(timer));
    }
}
