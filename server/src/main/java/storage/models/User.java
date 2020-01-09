package storage.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import java.util.HashSet;
import java.util.Set;

/**
 * The type of a WQ user.
 * A score based order and a lexicographical nickname equality notions
 * are defined on the elements of this class.
 */
public class User implements Comparable<User> {

    @JsonView({UserViews.Registration.class, UserViews.Online.class})
    @JsonProperty("n")
    private String nick;

    @JsonView(UserViews.Registration.class)
    @JsonProperty("p")
    private String password;

    @JsonView(UserViews.Online.class)
    @JsonProperty("s")
    private int score;

    @JsonView(UserViews.Online.class)
    @JsonProperty("f")
    private Set<String> friends;

    @JsonIgnore
    private boolean hasBeenModified = false;

    public User() {}

    /**
     * Initially an user must have nick
     * @param nick
     * @throws IllegalArgumentException if nick is null
     */
    public User(String nick) throws IllegalArgumentException {
        if (nick == null || nick.isEmpty()) throw new IllegalArgumentException();
        this.nick = nick;
        this.friends = new HashSet<>();
        this.score = 0;
    }

    /**
     * constructs an user with password
     * @param password
     * @param nick
     * @throws IllegalArgumentException
     */
    public User(String nick, String password) throws IllegalArgumentException {
        this(nick);
        if (password == null || password.isEmpty())
            throw new IllegalArgumentException();
        this.password = password;
    }

    public String getNick() {
        return nick;
    }

    public String getPassword() {
        return password;
    }

    public Set<String> getFriends() {
        return friends;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.hasBeenModified = true;
        this.score = score;
    }

    public void setFriends(Set<String> friends) {
        this.hasBeenModified = true;
        this.friends = friends;
    }

    /**
     * Adds a friend to the user's set.
     * @param nickFriend
     * @return true if the friend was added.
     */
    public boolean addFriend(String nickFriend) {
        this.hasBeenModified = true;
        return this.friends.add(nickFriend);
    }

    /**
     * Adds friends to the user's set.
     * @param nicks
     */
    public boolean addFriends(Set<String> nicks) {
        this.hasBeenModified = true;
        return this.friends.addAll(nicks);
    }

    /**
     * @return true if the user has been modified
     * (score updating or friend additions happened)
     */
    public boolean hasBeenModified() {
        return hasBeenModified;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o == null || o.getClass() != this.getClass())
            return false;
        User user = (User) o;
        return this.getNick().equals(user.getNick());
    }

    /**
     * Compares two user by score.
     * @param user
     * @return
     */
    @Override
    public int compareTo(User user) {
        return this.getScore() - user.getScore();
    }
}
