package protocol.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import protocol.OperationCode;
import protocol.ResponseCode;

import java.util.List;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PacketPojo {

    private PacketPojo() { }

    public PacketPojo(OperationCode operationCode) {
        this.operationCode = operationCode;
        this.timestamp = System.currentTimeMillis();
    }


    public PacketPojo(OperationCode operationCode, ResponseCode responseCode) {
        this(operationCode);
        this.responseCode = responseCode;
    }

    public PacketPojo(OperationCode operationCode, ResponseCode responseCode, String message) {
        this(operationCode, responseCode);
        this.message = message;
    }

    /**
     * Used to build an ASK_WORD packet.
     * @param word
     */
    public static PacketPojo buildAskWordRequest(String word) {
        return new PacketPojo(OperationCode.ASK_WORD)
                .setWord(word);
    }

    public static PacketPojo buildChallengeRequest(String player2) {
        return new PacketPojo(OperationCode.REQUEST_CHALLENGE)
                .setFriend(player2);
    }

    public static PacketPojo buildForwardChallengeRequest(String sender, Integer ttl) {
        return new PacketPojo(OperationCode.FORWARD_CHALLENGE)
                .setFriend(sender)
                .setTtl(ttl);
    }

    public static PacketPojo buildAddFriendRequest(String friend) {
        return new PacketPojo(OperationCode.ADD_FRIEND).setFriend(friend);
    }

    public static PacketPojo buildLoginRequest(String nickName, String passw, Integer udpPort) {
        return new PacketPojo(OperationCode.LOGIN)
                .setNickName(nickName)
                .setPassword(passw)
                .setUdpPort(udpPort);
    }

    public static PacketPojo buildGetFriendsResponse(Set<String> friends) {
        return new PacketPojo(OperationCode.GET_FRIENDS, ResponseCode.OK)
                .setFriends(friends);
    }


    public static PacketPojo buildScoreResponse(Integer scores) {
        return new PacketPojo(OperationCode.GET_SCORE, ResponseCode.OK)
                .setScore(scores);
    }

    public static PacketPojo buildRankingResponse(List<RankingListItem> ranking) {
        return new PacketPojo(OperationCode.GET_RANKING, ResponseCode.OK)
                .setRanking(ranking);
    }

    public static PacketPojo buildForwardChallengeResponse(ResponseCode responseCode, String sender, Integer ttl) {
        return new PacketPojo(OperationCode.FORWARD_CHALLENGE, responseCode)
                .setFriend(sender)
                .setTtl(ttl);
    }

    public static PacketPojo buildAskWordResponse(String word) {
        return new PacketPojo(OperationCode.ASK_WORD, ResponseCode.OK)
                .setWord(word);
    }


    /* -------------------------------------------- */
    // Request and response headers.

    /**
     * The operation: needed also in responses to interpret the data.
     */
    @JsonProperty("op")
    OperationCode operationCode;

    /**
     * The response code for the request.
     */
    @JsonProperty("rc")
    ResponseCode responseCode;

    /**
     * The TTL for the packet.
     */
    @JsonProperty("ttl")
    Integer ttl;

    /**
     * The creation timestamp.
     */
    @JsonProperty("ts")
    Long timestamp;
    /* -------------------------------------------- */
    // Request fields for each operation code.

    /**
     * Login request:
     * - nickName: the client nickName which wants to authenticate into the system.
     * - password: the password registered to the nickName.
     * - UDPPort: the port on which the client listen for battle challenges requests.
     */
    @JsonProperty("name")
    String nickName;
    @JsonProperty("passw")
    String password;
    @JsonProperty("port")
    Integer UDPPort;

    /**
     * Add-friend request:
     * - friend nickname.
     * Challenge request:
     * - player2 nickname.
     * Forward request:
     * - player1 nickname.
     */
    @JsonProperty("f")
    String friend;

    /**
     * Ask-word request/response:
     * - the english word to be translated.
     */
    @JsonProperty("w")
    String word;

    /* -------------------------------------------- */
    // General response fields.
    /**
     * A general info message, use it for example to explain the error type.
     */
    @JsonProperty("info")
    String message;

    /* -------------------------------------------- */
    // Response fields for each operation code.

    /**
     * This field is used in a successful GET_RANKING response.
     * @link OperationCode
     */
    @JsonProperty("rank")
    List<RankingListItem> rankingList;

    /**
     * Response to GET_FRIENDS. List of all user's friends.
     */
    @JsonProperty("fl")
    Set<String> friends;

    /**
     * The scores of the client. Response to GET_SCORE.
     */
    @JsonProperty("s")
    Integer scores;


    public OperationCode getOperationCode() {
        return operationCode;
    }

    public String getNickName() {
        return nickName;
    }

    public String getPassword() {
        return password;
    }

    public Integer getUDPPort() {
        return UDPPort;
    }

    public Integer getScores() {
        return scores;
    }

    public String getWord() { return word; }

    public List<RankingListItem> getRankingList() {
        return rankingList;
    }

    public String getFriend() {
        return friend;
    }

    public Set<String> getFriends() {
        return friends;
    }

    public String getMessage() {
        return message;
    }

    public Integer getTtl() {
        return ttl;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public boolean isRequest() {
        return this.responseCode == null;
    }

    public boolean isResponse() {
        return this.responseCode != null;
    }

    public boolean isSuccessfullResponse() {
        return ResponseCode.OK.equals(this.responseCode);
    }

    public boolean isErrorResponse() {
        return ResponseCode.ERROR.equals(this.responseCode);
    }

    public boolean isExplainedErrorResponse() {
        return this.isErrorResponse() && this.message != null;
    }

    /**
     * Checks parameters for each type of request packet.
     * @return true if the packet is a well formed request packet.
     */
    public boolean isWellFormedRequestPacket() {
        if (!this.isRequest()) return false;
        switch (this.getOperationCode()) {
            case LOGIN:
                // User, password, udpPort
                return this.nickName != null
                        && this.password != null
                        && this.UDPPort != null;
            case FORWARD_CHALLENGE:
                // player1 name.
            case ADD_FRIEND:
                // user to be added as friend
            case REQUEST_CHALLENGE:
                // player2 name
                return this.friend != null;
            case ASK_WORD:
                // word to be asked
                return this.word != null;
            case LOGOUT:
            case GET_FRIENDS:
            case GET_SCORE:
            case GET_RANKING:
            default:
                // No params
                return true;
        }
    }

    private PacketPojo setFriend(String friend) {
        this.friend = friend;
        return this;
    }

    private PacketPojo setFriends(Set<String> friends) {
        this.friends = friends;
        return this;
    }

    private PacketPojo setScore(Integer scores) {
        this.scores = scores;
        return this;
    }

    private PacketPojo setRanking(List<RankingListItem> list) {
        this.rankingList = list;
        return this;
    }

    private PacketPojo setWord(String word) {
        this.word = word;
        return this;
    }

    private PacketPojo setNickName(String nickName) {
        this.nickName = nickName;
        return this;
    }

    private PacketPojo setPassword(String password) {
        this.password = password;
        return this;
    }

    private PacketPojo setUdpPort(Integer udpPort) {
        this.UDPPort = udpPort;
        return this;
    }

    private PacketPojo setTtl(Integer ttl) {
        this.ttl = ttl;
        return this;
    }
}


