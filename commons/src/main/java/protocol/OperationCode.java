package protocol;

/**
 * Serialized in a byte only.
 */
public enum OperationCode {

    // Client requests
    LOGIN,
    LOGOUT,
    ADD_FRIEND,
    GET_FRIENDS,
    REQUEST_CHALLENGE,
    GET_SCORE,
    GET_RANKING,
    FORWARD_CHALLENGE,
    SETUP_CHALLENGE,
    ASK_WORD;

    private static final OperationCode[] operationCodes = OperationCode.values();

    public static OperationCode fromByte(byte i) {
        return operationCodes[i];
    }

    /**
     * WARNING: It takes only one byte from the integer representation.
     * @param operationCode
     * @return
     */
    public static byte toOneByte(OperationCode operationCode) {
        return new Integer(operationCode.ordinal()).byteValue();
    }
}
