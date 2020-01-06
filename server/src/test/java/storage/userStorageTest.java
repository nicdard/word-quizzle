package storage;

import configurations.Config;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class userStorageTest {

    private static UserStorage userStorage;

    @BeforeAll
    static void before() {
        String[] options = {"-useStoragePath=internal/test"};
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
    }

    @Test
    void test() {
        userStorage.register("Nicola", "assad");
    }

    @Test
    void test2() {
        userStorage.register("Lando", "dvfdvf");
    }
}
