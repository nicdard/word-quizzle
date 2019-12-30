import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class DictionaryService implements DataService<DictionaryService.Dictionary<String>> {

    /** Default store path*/
    private final static String DEFAULT_STORE_PATH = "";
    /** Default retrieve path*/
    private final static String DEFAULT_RETRIEVE_PATH = "";

    /** The dictionary requested length when calling retrieve */
    private int len;
    /** The path where to store the dictionary */
    private String storePath = DEFAULT_STORE_PATH;
    /** The path to the italian words file */
    private String retrievePath = DEFAULT_RETRIEVE_PATH;

    DictionaryService(int len) {
        this.len = len;
    }

    DictionaryService(int len, String storePath) {
        this(len);
        this.storePath = storePath != null ? storePath : DEFAULT_STORE_PATH;
    }

    DictionaryService(int len, String storePath, String retrievePath) {
        this(len, storePath);
        this.retrievePath = retrievePath != null ? retrievePath : DEFAULT_RETRIEVE_PATH;
    }


    /**
     * Store a list of chosen italian words
     * @throws UnsupportedOperationException
     */
    public boolean store(Dictionary<String> dictionary) {
        return false;
    }

    /**
     * Randomly get a set of italian word of a predefined len
     * provided with a method to consume it
     * @return a new random italian Dictionary
     */
    public Dictionary<String> retrieve() {
        return new Dictionary<String>(this.getRandomWords());
    }

    public boolean remove(Dictionary<String> obj) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Changes the length of the dictionary requested when calling retrieve,
     * Important: It affects all future retrieve calls once called.
     * @param len new dictionary length when calling retrieve
     */
    public void setLen(int len) {
        this.len = len;
    }

    private Set<String> getRandomWords() {
        return new HashSet<String>(this.len);
    }

    private void checkPath() {
        // TODO
    }

    class Dictionary<T> {
        /** This is a copy of the actual available word set */
        private Set<T> dictionary;
        private Iterator<T> iterator;

        Dictionary(Set<T> dictionary) {
            this.dictionary = dictionary;
        }

        T getAndRemove() {
            if (this.iterator != null) {
                if (this.iterator.hasNext()) {
                    T next = this.iterator.next();
                    this.iterator.remove();
                    return next;
                } else {
                    throw new IllegalStateException("Empty dictionary");
                }
            } else {
                this.iterator = this.dictionary.iterator();
                return this.getAndRemove();
            }
        }
    }

}
