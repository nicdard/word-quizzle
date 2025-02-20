package storage;

import configurations.Config;
import org.junit.jupiter.api.*;
import storage.iotasks.JSONMapper;
import storage.models.User;
import storage.models.UserViews;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class userStorageTest {

    private static UserStorage userStorage;

    @BeforeAll
    static void before() {
        // Policy: on session close
        String[] options = {"-useStoragePath=internal/test", "-useStoragePolicy=ON_SESSION_CLOSE"};
        Config.getInstance().parseCommandLineArguments(
                options
        );
        userStorage = UserStorage.getInstance();
        try {
            Files.deleteIfExists(Paths.get(userStorage.getOnlinePath()));
            Files.deleteIfExists(Paths.get(userStorage.getRegistrationPath()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        testRegisterAndLogin1();
        testRegisterAndLogin2();
    }

    static void testRegisterAndLogin1() {
        Assertions.assertTrue(userStorage.register("user1", "11111"));
        Assertions.assertTrue(userStorage.logInUser("user1", "11111"));
    }

    static void testRegisterAndLogin2() {
        Assertions.assertTrue(userStorage.register("user2", "22222"));
        Assertions.assertTrue(userStorage.logInUser("user2", "22222"));
    }

    @Test
    @Order(2)
    void testModifyOnline12notLoaded3() throws IOException {
        Assertions.assertTrue(userStorage.register("user3", "33333"));
        // Makes them all friends, but it should update only user 3
        // because 1 and 2 are online (accordingly to the policy).
        userStorage.addFriend("user1", "user2");
        userStorage.addFriend("user1", "user3");
        userStorage.addFriend("user2", "user3");
        userStorage.updateUserScore("user1", 3);
        // Verifies user 3 updated and 1 and 2 not yet.
        User user3 = JSONMapper.findAndGet(userStorage.getOnlinePath(), "user3", UserViews.Online.class);
        Set<String> expectedJSON3Friends = new HashSet<>(Arrays.asList("user1", "user2"));
        Assertions.assertEquals(expectedJSON3Friends, user3.getFriends());
        // In json 1 is not friend with 2 and 3, but in the storage it has them among his friends.
        Set<String> expected1Friends = new HashSet<>(Arrays.asList("user2", "user3"));
        Assertions.assertEquals(expected1Friends, userStorage.getFriends("user1"));
        User user1 = JSONMapper.findAndGet(userStorage.getOnlinePath(), "user1", UserViews.Online.class);
        Assertions.assertEquals(new HashSet<>(), user1.getFriends());
        Assertions.assertEquals(0, user1.getScore());
        // After logout all online modified users are written to the files
        userStorage.logOutUser("user1");
        userStorage.logOutUser("user2");
        user1 = JSONMapper.findAndGet(userStorage.getOnlinePath(), "user1", UserViews.Online.class);
        Assertions.assertEquals(expected1Friends, user1.getFriends());
        Assertions.assertEquals(3, user1.getScore());
    }

    @Test
    @Order(3)
    void testRankingList() {
        Assertions.assertTrue(userStorage.logInUser("user1", "11111"));
        Assertions.assertEquals(3, userStorage.getRankingList("user1").size());
        userStorage.logOutUser("user1");
    }
}
