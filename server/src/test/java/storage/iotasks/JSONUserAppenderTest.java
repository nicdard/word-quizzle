package storage.iotasks;

import configurations.Config;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import storage.models.User;
import storage.models.UserViews;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JSONUserAppenderTest {

    private final static String TEST_REGISTRATION_FILE =  String.join(File.separator,
            "internal", "test", "user_appender.json"
    );

    @BeforeAll
    static void cleanup() {
        // Initialise storage directories
        Path storagePath = Paths.get(Config.getInstance().getStoragePath());
        if (!Files.exists(storagePath)) {
            try {
                Files.createDirectories(storagePath);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Storage directory "
                        + storagePath.toAbsolutePath()
                        + " unavailable"
                );
            }
        }
        // Deletes existing test file
        try {
            Files.deleteIfExists(Paths.get(TEST_REGISTRATION_FILE));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testJSONUserAppender() {
        User testUser = new User("a", "!");
        new JSONUserAppender(
                TEST_REGISTRATION_FILE,
                testUser,
                UserViews.Registration.class
        ).run();
        testUser = new User("b", "!");
        new JSONUserAppender(
                TEST_REGISTRATION_FILE,
                testUser,
                UserViews.Registration.class
        ).run();
        Assertions.assertTrue(isJSONFileValid(TEST_REGISTRATION_FILE));
    }

    /**
     * Checks that the given file is a valid JSON file.
     * @param filename
     * @return
     */
    private static boolean isJSONFileValid(String filename) {
        try {
            String jsonInString = String.join("",
                    Files.readAllLines(Paths.get(filename))
            );
            JSONMapper.objectMapper.readTree(jsonInString);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
