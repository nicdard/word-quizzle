package connection;

import protocol.OperationCode;
import protocol.ResponseCode;
import protocol.WQPacket;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores and processes packets of a client session.
 */
public class State {

    /**
     * The user nickname using this connection.
     * When the user is not loggedIn this field is null.
     */
    private String clientNick;
    /** The packet to be written over the socket serialized in a byteBuffer. */
    private ByteBuffer packetToWrite;
    /** The chunks of a packet that is being read. */
    private List<ByteBuffer> packetChunks;
    /**
     * The remaining bits to be read from the socket to complete the packet.
     * It is initially set to the header length of a packet, after having
     * read the header it is set to the current remaining length.
     */
    private int readRemainingBytes;
    /** The packet total dimension as written in the packet header. */
    private int packetTotalDimension;

    /** The port on which the host listen for challenges forwarded requests */
    private int UDPPort;

    /**
     * Constructor for the host writing to UDPConnection.
     */
    public State() {
        this.packetChunks = new ArrayList<>();
        this.readRemainingBytes = WQPacket.getHeaderByteNumber(); // The bytes of the header.
        this.packetTotalDimension = -1;
    }

    /**
     * Builds and returns a WQPacket from the chunks queue.
     * It fails if some bytes are yet to be read.
     * After building a packet it reset the reading fields.
     * @return
     */
    public WQPacket buildWQPacketFromChucks() throws IllegalStateException {
        if (this.readRemainingBytes != 0) {
            throw new IllegalStateException("Packet is not yet arrived");
        }
        // Requesting a packet means that all chunks have been read.
        WQPacket wqPacket = WQPacket.fromBytes(packetChunks);
        this.resetReading();
        return wqPacket;
    }

    /**
     * Sets a packet to be written to the socket.
     * If wqPacket == null it allocates a 0-length byteBuffer.
     * @param wqPacket
     */
    public void setPacketToWrite(WQPacket wqPacket) {
        this.packetToWrite = wqPacket != null
                ? wqPacket.toByteBuffer()
                : ByteBuffer.allocate(0);
    }

    /**
     * Checks if the packet contains the expected number of parameters.
     * @param params
     * @param opCode
     * @return true if right number of parameters is found for the given operation.
     */
    public static boolean isWellFormedRequestPacket(String[] params, OperationCode opCode) {
        switch (opCode) {
            case LOGIN:
                // User, password, udpPort
                try {
                    return params.length == 3
                            && !params[0].isEmpty()
                            && !params[1].isEmpty()
                            && !params[2].isEmpty()
                            && Integer.parseInt(params[2]) >= 0;
                } catch (NumberFormatException e) {
                    return false;
                }
            case FORWARD_CHALLENGE:
                // Response code and player1 name
                return params.length == 2
                        && (ResponseCode.ACCEPT.name().equals(params[0])
                            || ResponseCode.DISCARD.name().equals(params[0]))
                        && !params[1].isEmpty();
            case ADD_FRIEND:
                // User name of the friend
            case REQUEST_CHALLENGE:
                // player2 name
            case ASK_WORD:
                // word to be asked
                return params.length == 1 && !params[0].isEmpty();
            case LOGOUT:
            case GET_FRIENDS:
            case GET_SCORE:
            case GET_RANKING:
            default:
                // No params
                return params.length == 1 && params[0].isEmpty();
        }
    }

    /**
     * @return the current client nickname associated to this connection.
     */
    public String getClientNick() {
        return clientNick;
    }

    /**
     * Sets a new client nickname associated to this connection only if the
     * connection is not yet associated (i.e. clientNick == null).
     * NOTE: to set a new client nickname reset method should be called first.
     * @param clientNick
     */
    public boolean setClientNick(String clientNick) {
        if (this.clientNick == null) {
            this.clientNick = clientNick;
            return true;
        } else return false;
    }

    /**
     * Adds a chunk to the list of read chunks.
     * It also tries to get the packet total dimension from the chunks
     * already arrived.
     * @param chunk
     * @return true if the bytes expected of the original packet are all arrived.
     */
    public boolean addChunk(ByteBuffer chunk) {
        this.packetChunks.add(chunk);
        int receivedBytes = this.packetChunks.stream()
                .mapToInt(Buffer::remaining)
                .sum();
        if (this.packetTotalDimension <= WQPacket.getHeaderByteNumber()
            && receivedBytes >= WQPacket.getHeaderByteNumber()
        ) {
            this.packetTotalDimension = WQPacket.getPacketLengthFromHeaderBytes(this.packetChunks);
        }
        if (this.readRemainingBytes != 0) {
            this.readRemainingBytes = this.packetTotalDimension >= 0
                    ? this.packetTotalDimension - receivedBytes
                    : this.readRemainingBytes;
        }
        return this.readRemainingBytes == 0;
    }

    /**
     * @return the ongoing packet.
     */
    public ByteBuffer getPacketToWrite() {
        return packetToWrite;
    }

    /**
     * @return true if the connection is already assigned to a login session.
     */
    public boolean isAssigned() {
        return this.clientNick != null;
    }

    /**
     * Resets this connection so it can be reassigned to a new client.
     */
    public void reset() {
        this.clientNick = null;
        this.setPacketToWrite(null);
        this.resetReading();
    }

    /** Resets the reading fields of this connection */
    private void resetReading() {
        // Empty the queue.
        this.packetChunks.clear();
        // Reset counters.
        this.packetTotalDimension = -1;
        this.readRemainingBytes = WQPacket.getHeaderByteNumber();
    }

    public int getUDPPort() {
        return UDPPort;
    }

    public void setUDPPort(int UDPPort) {
        this.UDPPort = UDPPort;
    }
}
