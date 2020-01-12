package connection;

import protocol.OperationCode;
import protocol.WQPacket;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class Connection {
    private ByteBuffer packetToWrite;
    private List<ByteBuffer> packetChunks;

    private String clientNick;

    public Connection() {
        this.packetChunks = new ArrayList<>();
    }

    public WQPacket buildWQPacketFromChucks() {
        // Requesting a packet means that all chunks have been read.
        WQPacket wqPacket = WQPacket.fromBytes(packetChunks);
        // Empty the queue.
        this.packetChunks.clear();
        return wqPacket;
    }

    public void setPacketToWrite(WQPacket wqPacket) {
        this.packetToWrite = wqPacket != null
                ? wqPacket.toByteBuffer()
                : null;
    }

    public static boolean isWellFormedRequestPacket(String[] params, OperationCode opCode) {
        switch (opCode) {
            case LOGIN:
                // User and password
                return params.length == 2 && !params[0].isEmpty() && !params[1].isEmpty();
            case ADD_FRIEND:
                // User name of the friend
            case REQUEST_CHALLENGE:
                // player2 name
            case FORWARD_CHALLENGE:
                // player1 name
            case ENTERING_CHALLENGE:
                // challenger nickname
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

    public String getClientNick() {
        return clientNick;
    }

    public void setClientNick(String clientNick) {
        this.clientNick = clientNick;
    }

    public boolean addChunk(ByteBuffer chunk) {
        return this.packetChunks.add(chunk);
    }

    public ByteBuffer getPacketToWrite() {
        return packetToWrite;
    }
}
