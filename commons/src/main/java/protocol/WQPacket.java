package protocol;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;


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
     * The length of the whole packet (header included) in byte.
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
    public WQPacket(OperationCode opCode, byte[] body) {
        if (opCode == null || body == null)
            throw new IllegalArgumentException("Invalid opCode or body");
        this.opCode = opCode;
        this.body = body;
        this.totalLength = this.body.length + getHeaderByteNumber();
    }
    /**
     * Builds a new packet and calculates the totalLength in bytes.
     * @param opCode
     * @param body
     */
    public WQPacket(OperationCode opCode, String body) {
        if (opCode == null || body == null)
            throw new IllegalArgumentException("Invalid opCode or body");
        this.opCode = opCode;
        this.body = body.getBytes();
        this.totalLength = this.body.length + getHeaderByteNumber();
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

    public ByteBuffer toByteBuffer() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(this.totalLength);
        byteBuffer.put(OperationCode.toOneByte(this.opCode));
        byteBuffer.putInt(this.totalLength);
        byteBuffer.put(this.body);
        byteBuffer.flip();
        return byteBuffer;
    }
    /**
     * @return a byte array containing the packet
     */
    public byte[] toBytes() {
        return this.toByteBuffer().array();
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

    public static WQPacket fromBytes(ByteBuffer ...packetBytes) {
        return WQPacket.fromBytes(concat(packetBytes));
    }

    public static WQPacket fromBytes(List<ByteBuffer> packetBytes) {
        return WQPacket.fromBytes(concat(packetBytes));
    }

    public static int getPacketLengthFromHeaderBytes(List<ByteBuffer> headerChunks) {
        return WQPacket.getPacketLengthFromHeaderBytes(concat(headerChunks));
    }

    public static int getPacketLengthFromHeaderBytes(ByteBuffer header) {
        if (header.remaining() < WQPacket.getHeaderByteNumber()) {
            return -1;
        }
        return header.getInt(WQPacket.getLengthOffeset());
    }

    public static int getHeaderByteNumber() {
        return 5;
    }

    private static int getLengthOffeset() {
        return 1;
    }

    /**
     * Concatenates one or more byte buffers to one large buffer. The combined
     * size of all buffers must not exceed {@link java.lang.Integer#MAX_VALUE}.
     * @param bbs list of byte buffers to combine
     * @return byte buffer containing the combined content of the supplied byte
     *         buffers
     */
    public static ByteBuffer concat(List<ByteBuffer> bbs) {
        long length = 0;
        // get amount of remaining bytes from all buffers
        for (ByteBuffer bb : bbs) {
            bb.rewind();
            length += bb.remaining();
        }
        if (length > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Buffers are too large for concatenation");
        }
        if (length == 0) {
            return ByteBuffer.allocate(0);
        }
        ByteBuffer bbNew = ByteBuffer.allocateDirect((int) length);
        // put all buffers from list
        for (ByteBuffer bb : bbs) {
            bb.rewind();
            bbNew.put(bb);
            bb.rewind();
        }
        bbNew.rewind();
        return bbNew;
    }

    /**
     * Concatenates one or more byte buffers to one large buffer. The combined
     * size of all buffers must not exceed {@link java.lang.Integer#MAX_VALUE}.
     * @param bb one or more byte buffers to combine
     * @return byte buffer containing the combined content of the supplied byte
     *         buffers
     */
    private static ByteBuffer concat(ByteBuffer ...bb) {
        return concat(Arrays.asList(bb));
    }
}
