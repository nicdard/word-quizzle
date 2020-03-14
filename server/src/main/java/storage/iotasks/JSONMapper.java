package storage.iotasks;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import storage.models.User;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Defines an unique objectMapper to serialize and deserialize JSON
 * and some utility functions for User.
 * (This is considered a good practice when using Jackson library.
 * The objectMapper is indeed an heavy thread-safe object, therefor
 * having around a few instances is preferable).
 */
public class JSONMapper {
    final static ObjectMapper objectMapper = new ObjectMapper();
    static {
        objectMapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /**
     * Serializes an user to JSON.
     * @param user
     * @return the user as a JSON string
     * @throws JsonProcessingException
     */
    static String serialize(final User user) throws JsonProcessingException {
        return objectMapper.writeValueAsString(user);
    }

    /**
     * Serializes an user to JSON accordingly to a specified view.
     * @param user
     * @param view
     * @return the user as a JSON string.
     * @throws JsonProcessingException
     */
    static String serialize(final User user,final Class view) throws JsonProcessingException {
        return objectMapper
                .writerWithView(view)
                .writeValueAsString(user);
    }

    /**
     * Serializes an user to JSON accordingly to a specified view.
     * @param user
     * @param view
     * @return the user as a JSON string.
     * @throws JsonProcessingException
     */
    static void serializeToFile(final User user,
                                final Class view,
                                final JsonGenerator jsonGenerator
    ) throws IOException {
        objectMapper
                .writerWithView(view)
                .writeValue(jsonGenerator, user);
    }

    /**
     * Deserialize a JSON string to an user
     * @param jsonUser
     * @return the user.
     * @throws IOException
     */
    static User deserialize(String jsonUser) throws IOException {
        return objectMapper.readValue(jsonUser, User.class);
    }

    /**
     * Deserialize a JSON string to an User according to a provided view.
     * @param jsonUser
     * @param view
     * @return the parsed user.
     * @throws IOException
     */
    static User deserialize(String jsonUser, Class view) throws IOException {
        return objectMapper
                .readerWithView(view)
                .forType(User.class)
                .readValue(jsonUser);
    }

    /**
     * Parses an JSON user from a JsonParser source using a provided view.
     * @param parser
     * @param view
     * @return The parsed user
     * @throws IOException
     */
    static User deserialize(final JsonParser parser, final Class view) throws IOException {
        return objectMapper
                .readerWithView(view)
                .forType(User.class)
                .readValue(parser);
    }

    /**
     * Search an user (by nickName) in a JSONArray file and retrieve it.
     * NOTE: It could have been written using the homologous function below
     * wrapping nick in a singletonSet, but this way it is more efficient on
     * large files.
     * @param filename
     * @param nick
     * @param view
     * @return an user
     */
    public static User findAndGet(final String filename,
                                  final String nick,
                                  final Class view
    ) throws IOException, NoSuchElementException {
        JsonFactory jsonFactory = JSONMapper.objectMapper.getFactory();
        // Opens the file and get a parser to traverse it
        try (InputStream inputStream = Files.newInputStream(Paths.get(filename));
             JsonParser parser = jsonFactory.createParser(inputStream)
        ) {
            // Check the first token
            if (parser.nextToken() != JsonToken.START_ARRAY) {
                throw new IllegalStateException("Expected content to be an array");
            }
            // Iterate over the tokens until the end of the array
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                // Get an user using Jackson data-binding
                User user = JSONMapper.deserialize(parser, view);
                if (nick.equals(user.getNick())) {
                    return user;
                }
            }
            throw new NoSuchElementException("User was not in the file");
        }
    }


    /**
     * Search users for a set of users' nickname and retrieves them.
     * @param filename
     * @param nicks
     * @param view
     * @return a set of user, it can be empty
     */
    public static TreeSet<User> findAndGet(final String filename,
                                           final Set<String> nicks,
                                           final Class view
    ) throws IOException {
        JsonFactory jsonFactory = JSONMapper.objectMapper.getFactory();
        // Opens the file and get a parser to traverse it
        try (InputStream inputStream = Files.newInputStream(Paths.get(filename));
             JsonParser parser = jsonFactory.createParser(inputStream)
        ) {
            // Check the first token
            if (parser.nextToken() != JsonToken.START_ARRAY) {
                throw new IllegalStateException("Expected content to be an array");
            }
            TreeSet<User> users = new TreeSet<>();
            // Iterate over the tokens until the end of the array
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                // Get an user using Jackson data-binding
                User user = JSONMapper.deserialize(parser, view);
                if (nicks.contains(user.getNick())) {
                    users.add(user);
                }
            }
            return users;
        }
    }

    /**
     * Makes a copy of a file and if an user is found it replaces it.
     * (Actually it ignores the file instance and writes the new one at last)
     * @param filename
     * @param user
     * @param view
     * @throws IOException
     */
    public static boolean copyAndUpdate(final String filename,
                                        final User user,
                                        final Class view
    ) throws IOException {
        // To avoid code replication.
        return copyAndUpdate(filename, Collections.singletonList(user), view);
    }

    /**
     * The same function as above but handles a set of users.
     * Makes a copy of a file and if an user is found it replaces it with the provided instance,
     * if some users are not in the file already it adds them.
     * (Actually it ignores the file instance and writes the new one at last)
     * @param filename
     * @param users
     * @param view
     * @throws IOException
     */
    public static boolean copyAndUpdate(final String filename,
                                        final List<User> users,
                                        final Class view
    ) throws IOException {
        String tempFilename = stripExtension(filename) + "_temp" + ".json";
        Path tempPath = Paths.get(tempFilename);
        Files.deleteIfExists(tempPath);
        Files.createFile(tempPath);
        JsonFactory jsonFactory = JSONMapper.objectMapper.getFactory();
        // Opens the file and get a parser to traverse it
        try (InputStream inputStream = Files.newInputStream(Paths.get(filename));
             JsonParser parser = jsonFactory.createParser(inputStream);
             OutputStream outputStream = Files.newOutputStream(tempPath);
             JsonGenerator generator = jsonFactory.createGenerator(outputStream)
        ) {
            // Check the first token.
            if (parser.nextToken() != JsonToken.START_ARRAY) {
                throw new IllegalStateException("Expected content to be an array");
            }
            // Writes the Array opening token.
            generator.writeStartArray();
            // Iterate over the tokens until the end of the array
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                // Get an user using Jackson data-binding
                User parsedUser = JSONMapper.deserialize(parser, view);
                // Do not copy the old user instance into the new file
                if (!users.contains(parsedUser)) {
                    // Immediately copy only those that are not updated
                    JSONMapper.serializeToFile(parsedUser, view, generator);
                } else {
                    // Merges the two objects
                    User update = users.get(users.indexOf(parsedUser));
                    update.addFriends(parsedUser.getFriends());
                }
            }
            // Writes the merged user instances
            for (User user : users) {
                JSONMapper.serializeToFile(user, view, generator);
            }
            // Writes closing Array token
            generator.writeEndArray();
        }
        // The above resources are closed when exiting the try-with.
        File storage = new File(filename);
        File current = new File(tempFilename);
        return current.renameTo(storage);
    }

    private static String stripExtension(final String s) {
        return s != null && s.lastIndexOf(".") > 0
                ? s.substring(0, s.lastIndexOf("."))
                : s;
    }
}
