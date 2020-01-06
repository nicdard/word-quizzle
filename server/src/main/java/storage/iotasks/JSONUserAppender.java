package storage.iotasks;

import com.fasterxml.jackson.core.JsonProcessingException;
import storage.models.User;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

/**
 * Appends an user to the specified json file according to the given view.
 * It implements Runnable so it can synchronously append info to more than one file.
 */
public class JSONUserAppender implements Runnable {

    private Path filepath;
    private User user;
    private Class view;

    public JSONUserAppender(String filename, User user, Class view) {
        this.filepath = Paths.get(filename);
        this.user = user;
        this.view = view;
    }

    @Override
    public void run() {
        String serializedUser;
        try {
            serializedUser = JSONMapper.serialize(user);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to serialize: " + this.user);
        }
        StringBuilder builder = new StringBuilder();
        Set<StandardOpenOption> options = new HashSet<>();
        options.add(StandardOpenOption.WRITE);
        if (!Files.exists(filepath)) {
            // Appends also opening parenthesis
            builder.append("[");
            // Sets option to create
            options.add(StandardOpenOption.CREATE);
        }
        try (FileChannel writeChannel = FileChannel.open(
                filepath,
                options
        )) {
            if (!options.contains(StandardOpenOption.CREATE)) {
                writeChannel.position(writeChannel.size() - 1);
                builder.append(",");
            }
            builder.append(serializedUser);
            builder.append("]");
            writeChannel.write(ByteBuffer.wrap(builder.toString().getBytes()));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("File appending " + this.filepath + " error");
        }
    }
}
