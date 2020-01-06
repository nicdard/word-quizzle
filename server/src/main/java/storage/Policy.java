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
     * Use a buffer only in writing operations, flush it before reading from the storage.
     * NOTE: may loose data and inconsistencies may occur
     */
    WRITE_ON_READ,
    /**
     * Use a buffer for all the client login session, when a client logout
     * then write all information.
     */
    ON_SESSION_CLOSE
}
