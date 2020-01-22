package challenge;

import configurations.Config;
import connection.State;
import protocol.OperationCode;
import protocol.ResponseCode;
import protocol.WQPacket;

import java.io.IOException;
import java.net.*;
import java.util.NoSuchElementException;
import java.util.concurrent.*;

/**
 * Notifies a user over UDP. Used to forward request challenge.
 */
public class NotifierService {

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
    private ConcurrentHashMap<String, CompletableFuture<WQPacket>> pendingResponses;

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

    public void addConnection(String nick, State state) {
        connectionTable.putIfAbsent(nick, state);
    }

    public void removeConnection(String nick) {
        connectionTable.remove(nick);
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
        WQPacket wqPacket = new WQPacket(
                OperationCode.FORWARD_CHALLENGE,
                sender
        );
        this.forwardMessage(dest, wqPacket);
        // If anyone will notify the sender before the timeout this future will complete
        // giving a DISCARD challenge response.
        this.setSenderNotificationTimeout(
                sender,
                OperationCode.REQUEST_CHALLENGE,
                ResponseCode.DISCARD + " " + dest,
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
        InetAddress ip = InetAddress.getByName("localhost");
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
        CompletableFuture<WQPacket> pendingResponse = this.pendingResponses.get(sender);
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
        CompletableFuture<WQPacket> timer = this.pendingResponses.remove(requester);
        if (timer != null) {
            return timer.get();
        } else {
            throw new NoSuchElementException("An unexpected error occurred: probably some race condition occurred!");
        }
    }

    /**
     * Sets (and starts) a pending response timer for the given sender that will be available
     * after timeoutMillis ms if nobody fulfill it before the timeout expires.
     * @param sender
     * @param op
     * @param responseBody
     * @param timeoutMillis
     */
    private void setSenderNotificationTimeout(final String sender,
                                      final OperationCode op,
                                      final String responseBody,
                                      final int timeoutMillis
    ) {
        // Starts the notification timer and saves a reference to it.
        this.pendingResponses.putIfAbsent(sender, CompletableFuture.supplyAsync(() ->
                {
                    try {
                        Thread.sleep(timeoutMillis);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return new WQPacket(
                            op,
                            responseBody
                    );
                })
        );
    }
}
