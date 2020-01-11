package protocol;

import java.nio.ByteBuffer;


/**
 * This is a packet exchanged from the client to the server and vice versa.
 *
 * The sender will set the opCode for the request and the body will contain
 * the parameters of requested operation.
 *
 * The receiver (when answering to the sender request) will copy the opCode
 * from the request and the body will contain the ResponseCode.
 *
 * The parameters of a packet are split using " " character and are parsed and
 * taken according to their index (positional parameters) and the opCode.
 */
public class WQPacket {

    /**
     * Operation code.
     * 8 bit.
     */
    private OperationCode opCode;
    /**
     * The length of the whole packet in byte.
     * 32 bits.
     */
    private int totalLength;
    /**
     * The body of the packet containing the parameters of a request.
     * Each parameter has the following format: [property_name]=[value];
     */
    private byte[] body;

    /**
     * Builds a new packet and calculates the totalLength in bytes.
     * @param opCode
     * @param body
     */
    WQPacket(OperationCode opCode, byte[] body) {
        if (opCode == null || body == null)
            throw new IllegalArgumentException("Invalid opCode or body");
        this.opCode = opCode;
        this.body = body;
        this.totalLength = this.body.length + 4 + 1;
    }
    /**
     * Builds a new packet and calculates the totalLength in bytes.
     * @param opCode
     * @param body
     */
    WQPacket(OperationCode opCode, String body) {
        if (opCode == null || body == null)
            throw new IllegalArgumentException("Invalid opCode or body");
        this.opCode = opCode;
        this.body = body.getBytes();
        this.totalLength = this.body.length + 4 + 1;
    }

    private int getTotalLength() {
        return totalLength;
    }

    public OperationCode getOpCode() {
        return opCode;
    }

    public String getBody() {
        return new String(this.body);
    }

    public String[] getParameters() {
        return this.getBody().split(" ");
    }

    /**
     * @return a byte array containing the packet
     */
    public byte[] toBytes() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(this.totalLength);
        byteBuffer.put(OperationCode.toOneByte(this.opCode));
        byteBuffer.putInt(this.totalLength);
        byteBuffer.put(this.body);
        return byteBuffer.array();
    }

    /**
     * Returns a WQPacket Object from a byte serialized version.
     * Checks that the length in the packet is the actual length
     * of the parsed one.
     * @param packetBytes
     * @return
     */
    public static WQPacket fromBytes(ByteBuffer packetBytes) {
        OperationCode opCode = OperationCode.fromByte(packetBytes.get());
        int totalLength = packetBytes.getInt();
        byte[] body = new byte[packetBytes.remaining()];
        packetBytes.get(body, 0, body.length);
        WQPacket wqPacket = new WQPacket(opCode, body);
        if (wqPacket.getTotalLength() != totalLength) {
            throw new IllegalStateException("Invalid packet parsing, bytes differ");
        }
        return wqPacket;
    }
}
