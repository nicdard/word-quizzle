package storage;

/**
 * Type to indicate the policy to be applied when reading/writing a storage
 * @link WARNING: Changes to this class are reflected in Config -useStoragePolicy option.
 */
public enum Policy {
    /**
     * Always access the storage to read/write (write on change)
     */
    IMMEDIATELY,
    /**
     * Use a buffer for all the client login session, when a client logs-out
     * then write all information.
     */
    ON_SESSION_CLOSE
}
