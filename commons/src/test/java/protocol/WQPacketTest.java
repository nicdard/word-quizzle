package protocol;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import protocol.json.PacketPojo;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class WQPacketTest {

    @Test
    void deserializingSerializedPacketEqualityTest() {
        PacketPojo loginPacket = PacketPojo.buildLoginRequest(
                "user1",  "11111", 23
        );
        WQPacket wqPacket = new WQPacket(loginPacket);
        ByteBuffer byteBuffer = ByteBuffer.wrap(wqPacket.toBytes());
        PacketPojo deserializedPacket = WQPacket.fromBytes(byteBuffer);
        Assertions.assertTrue(deserializedPacket.isRequest());
        Assertions.assertFalse(deserializedPacket.isResponse());
        Assertions.assertEquals(loginPacket.getNickName(), deserializedPacket.getNickName());
        Assertions.assertEquals(loginPacket.getPassword(), deserializedPacket.getPassword());
        Assertions.assertEquals(OperationCode.LOGIN, deserializedPacket.getOperationCode());
    }

    @Test
    void deserielizeFromByteBuffersTest() {
        PacketPojo packet = new PacketPojo(OperationCode.GET_FRIENDS);
        WQPacket wqPacket = new WQPacket(packet);
        byte[] wqBytes = wqPacket.toBytes();
        int slice = wqBytes.length / 2;
        ByteBuffer byteBuffer1 = ByteBuffer.wrap(Arrays.copyOfRange(wqBytes, 0, slice));
        ByteBuffer byteBuffer2 = ByteBuffer.wrap(Arrays.copyOfRange(wqBytes, slice, wqBytes.length));
        PacketPojo deserializedPacket = WQPacket.fromBytes(byteBuffer1, byteBuffer2);
        Assertions.assertEquals(packet.getOperationCode(), deserializedPacket.getOperationCode());
        Assertions.assertEquals(OperationCode.GET_FRIENDS, deserializedPacket.getOperationCode());
    }
}
