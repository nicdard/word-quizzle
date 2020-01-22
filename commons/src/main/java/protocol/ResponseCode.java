package protocol;

public enum ResponseCode {
    // Generic responses
    OK,
    ERROR,
    // For battle requests, both will have one field
    // that holds the nick of the friend.
    ACCEPT, // this response contains also the rules to be displayed
    DISCARD
}
