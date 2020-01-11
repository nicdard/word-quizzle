package protocol;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

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
}
