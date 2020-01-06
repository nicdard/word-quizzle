package storage;
import java.util.Set;

/**
 * The type of a StorageSetService:
 * Expose methods to operates over a storage and setting the desired access policy
 */
interface StorageSetService<E> {

    /**
     * Sets access policy of the service
     * @param policy
     */
    void setAccessPolicy(Policy policy);

    /**
     * Retrieves all the information from the storage as a set of data
     * @return
     */
    Set<E> retrieve();

    /**
     * Puts an item into the storage
     * NOTE: the final save to file depends on the policy adopted
     * @param item
     * @return true if the element was added to the storage
     */
    boolean put(E item);

    /**
     * Adds all elements of collection to the storage.
     * @param collection
     * @return true if all are added
     */
    boolean putAll(Iterable<E> collection);

    /**
     * @param item
     * @return true if the item is in the storage
     * NOTE: it uses equals method internally to perfom comparisons
     */
    boolean hasItem(E item);
}
