package storage.iotasks;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import storage.models.User;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.NoSuchElementException;

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
    static String serialize(User user) throws JsonProcessingException {
        return objectMapper.writeValueAsString(user);
    }

    /**
     * Serializes an user to JSON accordingly to a specified view.
     * @param user
     * @param view
     * @return the user as a JSON string.
     * @throws JsonProcessingException
     */
    static String serialize(User user, Class view) throws JsonProcessingException {
        return objectMapper
                .writerWithView(view)
                .writeValueAsString(user);
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
    static User deserialize(JsonParser parser, Class view) throws IOException {
        return objectMapper
                .readerWithView(view)
                .forType(User.class)
                .readValue(parser);
    }

    /**
     * Search an user (by nickName) in a JSONArray file and retrieve it
     * // FIXME for better performances it can be done with Jackson Streaming API only
     * @param filename
     * @param nick
     * @param view
     * @return an user
     */
    public static User findAndGet(String filename, String nick, Class view) throws IOException, NoSuchElementException {
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
     * Merges two user with the same nick.
     * @param u1
     * @param u2
     * @return the user instance which holds the password
     * filled with all the fields from the other instance.
     */
    static User mergeViews(User u1, User u2) {
        if (!u1.getNick().equals(u2.getNick()))
            throw new IllegalArgumentException("Invalid parameters, u1 and u2 should have the same nick");
        User ret;
        User from;
        if (u1.getPassword() != null) {
            ret = u1;
            from = u2;
        } else {
            ret = u2;
            from = u1;
        }
        ret.setScore(from.getScore());
        ret.setFriends(from.getFriends());
        return ret;
    }
}
