package connection;

import protocol.OperationCode;
import protocol.ResponseCode;
import protocol.WQPacket;
import protocol.json.PacketPojo;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
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

    /** The port on which the host listen for challenges forwarded requests */
    private int UDPPort;
    /** Excludes the channel associated with this from the main server selector. */
    private boolean isMainReadSelectable;
    /** client socketChannel */
    private SocketChannel client;

    /**
     * Constructor for the host writing to UDPConnection.
     */
    public State(SocketChannel client) {
        this.client = client;
        this.isMainReadSelectable = true;
        this.packetChunks = new ArrayList<>();
    }

    /**
     * Builds and returns a WQPacket from the chunks queue.
     * It fails if some bytes are yet to be read.
     * After building a packet it reset the reading fields.
     * @return
     */
    public PacketPojo buildWQPacketFromChucks() {
        // Requesting a packet means that all chunks have been read.
        PacketPojo packetPojo = WQPacket.fromBytes(packetChunks);
        this.resetReading();
        return packetPojo;
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
        final int packetTotalDimension = WQPacket.getPacketLengthFromHeaderBytes(this.packetChunks);
        return packetTotalDimension > 0 && packetTotalDimension - receivedBytes == 0;
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
    }

    public int getUDPPort() {
        return UDPPort;
    }

    public void setUDPPort(int UDPPort) {
        this.UDPPort = UDPPort;
    }

    public boolean isMainReadSelectable() {
        return isMainReadSelectable;
    }

    public void setMainReadSelectable(boolean mainReadSelectable) {
        isMainReadSelectable = mainReadSelectable;
    }

    public SocketChannel getClient() {
        return client;
    }
}
