package storage.iotasks;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import storage.models.User;
import storage.models.UserViews;

public class JSONUserSerializerTest {

    @Test
    void testUserRegistrationView() throws JsonProcessingException {
        String serialized = JSONMapper.serialize(
                new User("a", "!"),
                UserViews.Registration.class
        );
        Assertions.assertEquals("{\n" +
                "  \"n\" : \"a\",\n" +
                "  \"p\" : \"!\"\n" +
                "}", serialized);
    }

    @Test
    void testUserOnlineView() throws JsonProcessingException {
        String serialized = JSONMapper.serialize(
                new User("a", "!"),
                UserViews.Online.class
        );
        Assertions.assertEquals("{\n" +
                "  \"n\" : \"a\",\n" +
                "  \"s\" : 0,\n" +
                "  \"f\" : [ ]\n" +
                "}", serialized);
    }

    @Test
    void testUserWholeSerialization() throws JsonProcessingException {
        String serialized = JSONMapper.serialize(
                new User("a", "!")
        );
        Assertions.assertEquals("{\n" +
                "  \"n\" : \"a\",\n" +
                "  \"p\" : \"!\",\n" +
                "  \"s\" : 0,\n" +
                "  \"f\" : [ ]\n" +
                "}", serialized
        );
    }
}
