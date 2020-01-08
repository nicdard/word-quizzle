package storage;

import configurations.Config;
import storage.iotasks.JSONMapper;
import storage.iotasks.JSONUserAppender;
import storage.models.User;
import storage.models.UserViews;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * This is the store mechani
 */
class UserStorage {

    private String registrationPath;
    private String onlinePath;

    private Policy policy;

    private static UserStorage instance;
    private UserStorage() {
        // Initialise storage directories
        Config config = Config.getInstance();
        Path storagePath = Paths.get(config.getStoragePath());
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
        // Use File.separator for portability
        String path = config.getStoragePath();
        this.onlinePath = String.join(File.separator,
                path,
                UserViews.Online.FILE
        );
        this.registrationPath = String.join(
                File.separator,
                path,
                UserViews.Registration.FILE
        );
        this.policy = config.getStorageAccessPolicy();
    }

    public static UserStorage getInstance() {
        if (instance == null) {
            instance = new UserStorage();
            instance.onlineUsers = new ConcurrentHashMap<>();
            //instance.ongoingFriendshipRequests = new ConcurrentHashMap<>();
        }
        return instance;
    }

    /**
     * Stores logged users by nickname
     */
    private Map<String, User> onlineUsers;

    /**
     * Stores friendship requests, the key is the original recipient
     * and the value is the original sender of the request, this way
     * we provide an efficient way to send the request when the recipient
     * logsIn.
     * // TODO FUTURE
     */
    // private Map<String, String> ongoingFriendshipRequests;

    /**
     * Registers an user to WQ. IMMEDIATELY writes the new user to the storage file.
     * NOTE: Doesn't checks for the registration contract, checks only that the
     * representation invariant of this class holds (nick and password not null and not empty)
     */
    boolean register(String nickname, String password) {
        try {
            User user = new User(nickname, password);
            return this.append(user);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Checks if nickName is registered to WQ
     *
     * @param nickName
     */
    boolean exists(String nickName) {
        // To avoid file scanning
        if (this.isOnline(nickName)) return true;
        try {
            this.loadUserRegistrationInfo(nickName);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Checks if nickName exists and if the password is correct,
     * then load all the information from file (scores and friends).
     *
     * @param nickName
     * @param password
     * @return true if the user is correctly logged in
     */
    boolean logInUser(String nickName, String password) {
        // Fail on logging in an user which is already online
        if (this.isOnline(nickName)) return false;
        try {
            User user = this.loadUserRegistrationInfo(nickName);
            if (user.getPassword().equals(password)) {
                User userInfo = this.loadUserOnlineInfo(nickName);
                this.onlineUsers.putIfAbsent(nickName, userInfo);
                return true;
            } else {
                return false;
            }
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Drops an user from the loaded users. Eventually updates its records.
     *
     * @param nickName
     */
    void logOutUser(String nickName) {
        User user = this.onlineUsers.remove(nickName);
        if (this.policy.equals(Policy.ON_SESSION_CLOSE)) {
            // Writes changes for this user to file
            if (user.hasBeenModified()) {
                try {
                    JSONMapper.copyAndUpdate(
                            this.onlinePath,
                            user,
                            UserViews.Online.class
                    );
                } catch (IOException e) {
                    // It has some data lost
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * // TODO FUTURE
     * Stores the friendship request from the requester user to the
     * recipient user if both requester and recipientNick are valid
     * registered WQ users.
     * // FIXME for now implement only project requirement
     *
     * @param requester
     * @param recipientNick
     * @return true if all conditions for a friendship request hold and the request is stored.
     */
    boolean requestFirendship(String requester, String recipientNick) {
        User requesterUser = this.onlineUsers.get(requester);
        if (requesterUser == null
            || !this.exists(recipientNick)
        ) {
            return false;
        }
        requesterUser.addFriend(recipientNick);
        User recipientUser = this.loadUserOnlineInfo(recipientNick);
        recipientUser.addFriend(requester);
        Set<User> updates = new HashSet<>();
        // This section implements the storage updates policy
        if (this.policy == Policy.IMMEDIATELY) {
            updates.add(recipientUser);
            updates.add(requesterUser);
        } else if (this.policy == Policy.ON_SESSION_CLOSE
                    && !this.isOnline(recipientNick)
        ) {
            updates.add(recipientUser);
        }
        // In general do not add anything to the collection
        if (!updates.isEmpty()) {
            try {
                JSONMapper.copyAndUpdate(
                        this.onlinePath,
                        updates,
                        UserViews.Online.class
                );
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }


    /**
     * Sets recipientFriend and requester friendship.
     * Accordingly to the chosen storage policy, will at some point
     * write the information to the filesystem
     *
     * @param recipientFriend
     * @param requester
     */
    /*
    void acceptFriendship(String recipientFriend, String requester) {
    // TODO FUTURE
    }
    */

    /**
     * Returns all friends of a given user if it exists and is currently online.
     *
     * @param nickname
     * @return the sets of friends' nicks of the user.
     * @throws NoSuchElementException if the user is not online.
     */
    Set<String> getFriends(String nickname) throws NoSuchElementException {
        // Not using containsKey for concurrency safety,
        // really not needed in this particular case but as a good practice.
        User user = this.onlineUsers.get(nickname);
        if (user != null) {
            return user.getFriends();
        }
        throw new NoSuchElementException("The user is not currently online");
    }

    /**
     * Returns the scores of a given user if online.
     * @param nickname
     * @return
     * @throws NoSuchElementException
     */
    int getScore(String nickname) throws NoSuchElementException {
        User user = this.onlineUsers.get(nickname);
        if (user != null) {
            return user.getScore();
        }
        throw new NoSuchElementException("The user is not currently online");
    }

    /**
     * Calculates the ranking list of a given user with his friends.
     * @param nickname
     * @return
     * @throws IOException
     */
    TreeSet<User> getRankingList(String nickname) throws IOException {
        User user = this.onlineUsers.get(nickname);
        if (user == null) {
            throw new IllegalStateException("An user must be online to request the score ranking list");
        }
        TreeSet<User> rankingList = JSONMapper.findAndGet(
                this.onlinePath,
                user.getFriends(),
                UserViews.Online.class
        );
        rankingList.add(user);
        return rankingList;
    }

    String getOnlinePath() {
        return onlinePath;
    }

    String getRegistrationPath() {
        return registrationPath;
    }

    /**
     * @param nickname
     * @return true if the user is online.
     */
    private boolean isOnline(String nickname) {
        return this.onlineUsers.get(nickname) != null;
    }

    private boolean isOnline(User user) {
        return this.onlineUsers.get(user.getNick()) != null;
    }

    private User loadUserRegistrationInfo(String nickname) throws NoSuchElementException {
        return this.loadUserInfo(nickname, this.registrationPath, UserViews.Registration.class);
    }

    private User loadUserOnlineInfo(String nickname) throws NoSuchElementException {
        if (this.isOnline(nickname)) {
            return this.onlineUsers.get(nickname);
        }
        return this.loadUserInfo(nickname, this.onlinePath, UserViews.Online.class);
    }

    /**
     * Load an user from the given file searching by nickname and returns it
     * @param nickname
     * @param filename
     * @param view
     * @return return the user parsed from json with the given view
     * @throws NoSuchElementException
     */
    synchronized private User loadUserInfo(String nickname, String filename, Class view) throws NoSuchElementException {
        try {
            return JSONMapper.findAndGet(filename, nickname, view);
        } catch (IOException e) {
            e.printStackTrace();
            throw new NoSuchElementException(
                    "Impossible to retrieve the user from "
                            + this.registrationPath
                            + " due to some IOError"
            );
        }
    }

    /**
     * Appends concurrently an user to all storage files,
     * doesn't check if the user exists already
     */
    synchronized private boolean append(User user) {
        CompletableFuture<Void> registrationAppend = CompletableFuture.runAsync(
                new JSONUserAppender(this.registrationPath,
                        user,
                        UserViews.Registration.class
                )
        );
        CompletableFuture<Void> onlineAppend = CompletableFuture.runAsync(
                new JSONUserAppender(this.onlinePath,
                        user,
                        UserViews.Online.class
                )
        );
        try {
            // Waits for both tasks to complete
            CompletableFuture.allOf(registrationAppend, onlineAppend).get();
        } catch (RuntimeException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
