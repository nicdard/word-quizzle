package protocol;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class WQPacketTest {

    @Test
    void deserializingSerializedPacketEqualityTest() {
        WQPacket wqPacket = new WQPacket(OperationCode.LOGIN, "user1 11111");
        ByteBuffer byteBuffer = ByteBuffer.wrap(wqPacket.toBytes());
        WQPacket deserializedPacket = WQPacket.fromBytes(byteBuffer);
        Assertions.assertEquals(wqPacket.getBody(), deserializedPacket.getBody());
        Assertions.assertEquals(wqPacket.getOpCode(), deserializedPacket.getOpCode());
        Assertions.assertEquals(OperationCode.LOGIN, deserializedPacket.getOpCode());
        Assertions.assertEquals("user1 11111", deserializedPacket.getBody());
    }

    @Test
    void deserielizeFromByteBuffersTest() {
        WQPacket wqPacket = new WQPacket(OperationCode.GET_FRIENDS, "user1 11111");
        byte[] wqBytes = wqPacket.toBytes();
        int slice = wqBytes.length / 2;
        ByteBuffer byteBuffer1 = ByteBuffer.wrap(Arrays.copyOfRange(wqBytes, 0, slice));
        ByteBuffer byteBuffer2 = ByteBuffer.wrap(Arrays.copyOfRange(wqBytes, slice, wqBytes.length));
        WQPacket deserializedPacket = WQPacket.fromBytes(byteBuffer1, byteBuffer2);
        Assertions.assertEquals(wqPacket.getBody(), deserializedPacket.getBody());
        Assertions.assertEquals(wqPacket.getOpCode(), deserializedPacket.getOpCode());
        Assertions.assertEquals(OperationCode.GET_FRIENDS, deserializedPacket.getOpCode());
        Assertions.assertEquals("user1 11111", deserializedPacket.getBody());
    }
}
