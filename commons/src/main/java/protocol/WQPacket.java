package protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import protocol.json.PacketPojo;
import protocol.json.JSONMapper;

import java.io.IOException;
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
 * The WQPacket actually is a wrapper of the real packet (PacketPojo) which
 * adds the header containing the length.
 */
public class WQPacket {

    /**
     * The length of the whole packet (header included) in byte.
     * 32 bits.
     */
    private int totalLength;
    /**
     * The body of the packet containing the parameters of a request.
     * It is always a json.
     * @link PacketPojo.
     */
    private byte[] body;

    /**
     * The packet
     */
    private PacketPojo deserializedBody;

    /**
     * Builds a new packet and calculates the totalLength in bytes.
     * @param body
     */
    public WQPacket(byte[] body) {
        if (body == null)
            throw new IllegalArgumentException("Invalid opCode or body");
        this.body = body;
        this.totalLength = this.body.length + getHeaderByteNumber();
        try {
            this.deserializedBody = JSONMapper.objectMapper.readValue(this.body, PacketPojo.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Not deserializable");
        }
    }

    /**
     * Builds a new packet and calculates the totalLength in bytes.
     * @param body
     */
    public WQPacket(String body) {
        this(body != null ? body.getBytes() : null);
    }

    /**
     * Wraps a packetPojo and serializes it to bytes. Adds the header indicating the totalLength.
     * @param commandPojo
     */
    public WQPacket(PacketPojo commandPojo) {
        if (commandPojo != null) {
            try {
                this.deserializedBody = commandPojo;
                this.body = JSONMapper.objectMapper.writeValueAsBytes(commandPojo);
                this.totalLength = this.body.length + WQPacket.getHeaderByteNumber();
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Not serializable");
            }
        } else {
            throw new IllegalArgumentException("Invalid parameter.");
        }
    }

    /**
     * @return the body length according to the header of the packet.
     */
    public long getPacketBodyLength() {
        return this.totalLength - WQPacket.getHeaderByteNumber();
    }

    private long getTotalLength() {
        return totalLength;
    }

    public PacketPojo getDeserializedBody() {
        return this.deserializedBody;
    }

    /**
     * @return the byteBuffer, ready to be sent or read representing this packet.
     */
    public ByteBuffer toByteBuffer() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(this.totalLength);
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
     * @return the packet in its object representation.
     */
    public static PacketPojo fromBytes(ByteBuffer packetBytes) {
        int totalLength = packetBytes.getInt();
        byte[] body = new byte[packetBytes.remaining()];
        packetBytes.get(body, 0, totalLength - WQPacket.getHeaderByteNumber());
        WQPacket wqPacket = new WQPacket(body);
        if (wqPacket.getTotalLength() != totalLength) {
            throw new IllegalStateException("Invalid packet parsing, bytes differ");
        }
        return wqPacket.getDeserializedBody();
    }

    public static PacketPojo fromBytes(ByteBuffer ...packetBytes) {
        return WQPacket.fromBytes(concat(packetBytes));
    }

    public static PacketPojo fromBytes(List<ByteBuffer> packetBytes) {
        return WQPacket.fromBytes(concat(packetBytes));
    }

    public static int getPacketLengthFromHeaderBytes(List<ByteBuffer> headerChunks) {
        return WQPacket.getPacketLengthFromHeaderBytes(concat(headerChunks));
    }

    /**
     * @param header
     * @return the totalLength of a packet; -1 indicates that the header has not yet been read.
     */
    public static int getPacketLengthFromHeaderBytes(ByteBuffer header) {
        if (header.remaining() < WQPacket.getHeaderByteNumber()) {
            return -1;
        }
        return header.getInt();
    }

    public static int getPacketLengthFromHeaderBytes(byte[] header) {
        return getPacketLengthFromHeaderBytes(ByteBuffer.wrap(header));
    }

    public static int getHeaderByteNumber() {
        return 4;
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
