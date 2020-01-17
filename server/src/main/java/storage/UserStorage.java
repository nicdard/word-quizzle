package storage;

import configurations.Config;
import protocol.json.RankingListItem;
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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Manages the online users info and the disk files related to all WQ users.
 */
public class UserStorage {

    /**
     * Paths for the db files.
     */
    private String registrationPath;
    private String onlinePath;

    /**
     * The file accessing policy
     */
    private Policy policy;

    /**
     * Stores logged users by nickname
     */
    private Map<String, User> onlineUsers;

    /**
     * Locks for updating and reading db files.
     */
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();

    private static UserStorage instance = UserStorage.getInstance();
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
        }
        return instance;
    }

    /**
     * Registers an user to WQ. IMMEDIATELY writes the new user to the storage file.
     * NOTE: Doesn't checks for the registration contract, checks only that the
     * representation invariant of this class holds (nick and password not null and not empty)
     * The concurrency management is delegated to the caller (@link RegistrationRegistry)
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
     * Checks if nickName is registered to WQ.
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
     * then loads all the information from file (scores and friends).
     * @param nickName
     * @param password
     * @return true if the user is correctly logged in
     */
    public boolean logInUser(String nickName, String password) {
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
     * @param nickName
     */
    public boolean logOutUser(String nickName) {
        if (nickName == null || !this.isOnline(nickName)) return false;
        User user = this.onlineUsers.remove(nickName);
        if (this.policy.equals(Policy.ON_SESSION_CLOSE)) {
            // Writes changes for this user to file
            if (user.hasBeenModified()) {
                this.writeLock.lock();
                try {
                    JSONMapper.copyAndUpdate(
                            this.onlinePath,
                            user,
                            UserViews.Online.class
                    );
                } catch (IOException e) {
                    // It has some data lost
                    e.printStackTrace();
                } finally {
                    this.writeLock.unlock();
                }
            }
        }
        return true;
    }

    /**
     * Stores the friendship between the requester user to the
     * recipient user if both requester and recipientNick are valid
     * registered WQ users.
     * @param requester
     * @param recipientNick
     * @return true if all conditions for a friendship request hold and the request is stored.
     */
    public boolean addFriend(String requester, String recipientNick) {
        if (requester == null
            || recipientNick == null
            || requester.equals(recipientNick)
        ) {
            return false;
        }
        User requesterUser = this.onlineUsers.get(requester);
        if (requesterUser == null
            || !this.exists(recipientNick)
            || requesterUser.hasFriend(recipientNick)
        ) {
            return false;
        }
        requesterUser.addFriend(recipientNick);
        User recipientUser = this.loadUserOnlineInfo(recipientNick);
        recipientUser.addFriend(requester);
        List<User> updates = new ArrayList<>(2);
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
            this.writeLock.lock();
            try {
                JSONMapper.copyAndUpdate(
                        this.onlinePath,
                        updates,
                        UserViews.Online.class
                );
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } finally {
                this.writeLock.unlock();
            }
        }
        return true;
    }


    /**
     * Returns all friends of a given user if it exists and is currently online.
     * @param nickname
     * @return the sets of friends' nicks of the user.
     * @throws NoSuchElementException if the user is not online.
     */
    public Set<String> getFriends(String nickname) throws NoSuchElementException {
        // Not using containsKey for concurrency safety,
        // really not needed in this particular case but as a good practice.
        if (nickname == null) throw new NoSuchElementException("The user must be a valid one");
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
    public int getScore(String nickname) throws NoSuchElementException {
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
     * @throws RuntimeException
     */
    public List<RankingListItem> getRankingList(String nickname) throws RuntimeException {
        User user = this.onlineUsers.get(nickname);
        if (user == null) {
            throw new IllegalStateException("An user must be online to request the score ranking list");
        }
        try {
            TreeSet<User> rankingList = JSONMapper.findAndGet(
                    this.onlinePath,
                    user.getFriends(),
                    UserViews.Online.class
            );
            rankingList.add(user);
            List<RankingListItem> serializableList = rankingList.stream()
                    .sorted()
                    .map(u -> new RankingListItem(u.getNick(), u.getScore()))
                    .collect(Collectors.toList());
            return serializableList;
        } catch (IOException e) {
            throw new RuntimeException("Internal error");
        }
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
    /**
     * @param user
     * @return true if the user is online.
     */
    private boolean isOnline(User user) {
        return this.isOnline(user.getNick());
    }

    private User loadUserRegistrationInfo(String nickname) throws NoSuchElementException {
        this.readLock.lock();
        try {
            return this.loadUserInfo(nickname, this.registrationPath, UserViews.Registration.class);
        } finally {
            this.readLock.unlock();
        }
    }

    private User loadUserOnlineInfo(String nickname) throws NoSuchElementException {
        if (this.isOnline(nickname)) {
            return this.onlineUsers.get(nickname);
        }
        this.readLock.lock();
        try {
            return this.loadUserInfo(nickname, this.onlinePath, UserViews.Online.class);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Load an user from the given file searching by nickname and returns it
     * @param nickname
     * @param filename
     * @param view
     * @return return the user parsed from json with the given view
     * @throws NoSuchElementException
     */
    private User loadUserInfo(String nickname, String filename, Class view) throws NoSuchElementException {
        try {
            return JSONMapper.findAndGet(filename, nickname, view);
        } catch (IOException e) {
            //e.printStackTrace();
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
    private boolean append(User user) {
        this.writeLock.lock();
        try {
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
                return true;
            } catch (RuntimeException | InterruptedException | ExecutionException e) {
                e.printStackTrace();
                return false;
            }
        } finally {
            this.writeLock.unlock();
        }
    }
}
