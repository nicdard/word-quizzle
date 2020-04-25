package translation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements a fixed maximum size pool of couples < word, List of translations >. To manage the size every couple has also oldness timestamp, which is reset on touch
 * It is a centralised cache for the various challenges dictionaries (sort of JVM String constant pool).
 * It uses a chain of responsibility pattern: when a translation is not available
 * it forward the request to a "lower level" TranslationService
 * (either cache or primary service), therefor it isn't meant
 * to be used as the primary TranslationService.
 *
 * It is not a singleton because there can be different
 */
public class TranslationsPool extends BaseTranslationService {

    /**
     * The maximum number of cacheables words.
     */
    private long maximumSize;
    /**
     * The pool were to store
     */
    private Map<String, ItemValue> pool;

    public TranslationsPool(long maximumSize) {
        this.maximumSize = maximumSize;
        this.pool = new ConcurrentHashMap<>();
    }

    @Override
    public List<String> translate(String word) throws UnavailableTranslationException, IllegalStateException {
        this.checkTranslateContract();
        // Avoid using contains key to assure thread-safety
        ItemValue itemValue = this.pool.get(word);
        if (itemValue != null) {
            return itemValue.getTranslations();
        } else {
            // Pass the request to the next handler.
            List<String> translations = super.translate(word);
            // Get the translation provided by the next handler
            // and save it into the cache.
            if (this.pool.size() >= this.maximumSize) {
                this.freeSpace();
            }
            // To be sure in a concurrent environment, a concurrent writer
            // may have already written the word in the map meanwhile.
            this.pool.putIfAbsent(word, new ItemValue(translations));
            return translations;
        }
    }

    @Override
    public boolean setISOSourceLanguage(String sourceLanguageCode) {
        // Invalidate the cache only when setting a different language
        if (!sourceLanguageCode.equals(this.getISOSourceLanguage())) {
            this.invalidateCache();
        }
        return super.setISOSourceLanguage(sourceLanguageCode);
    }

    @Override
    public boolean setISODestinationLanguage(String destinationLanguageCode) {
        if (!destinationLanguageCode.equals(this.getISODestinationLanguage())) {
            this.invalidateCache();
        }
        return super.setISODestinationLanguage(destinationLanguageCode);
    }

    /**
     * Replace the current pool with a new empty one.
     */
    private void invalidateCache() {
        this.pool.clear();
    }

    /**
     * Removes the oldest entry from a not empty pool.
     */
    private void freeSpace() {
        this.pool.entrySet().stream()
                .min(Comparator.comparing(Map.Entry::getValue))
                .ifPresent(entry -> this.pool.remove(entry.getKey()));
    }

    /**
     * A value of the map implementing the pool containing the translation of its key
     * and the last touch timestamp.
     * It traces also the touches (gets from pool) as a second comparing parameters
     */
    private class ItemValue implements Comparable<ItemValue> {
        private List<String> translations;
        private long lastTouchTimestamp;
        private long touches;

        ItemValue(List<String> translations) {
            if (translations == null || translations.isEmpty())
                throw new IllegalArgumentException("translations should be a valid translation list (not empty)");
            this.translations = translations;
            this.lastTouchTimestamp = System.currentTimeMillis();
            this.touches = 1;
        }

        /**
         * @modifies Reset timestamp and counts +1 touch.
         * @return the list of translations.
         */
        List<String> getTranslations() {
            this.touch();
            return translations;
        }

        long getLastTouchTimestamp() {
            return lastTouchTimestamp;
        }

        /**
         * Compares two elements upon their lastTouchTimestamp and touches fields.
         * @param itemValue the instance to be compared to this
         */
        @Override
        public int compareTo(ItemValue itemValue) {
            long val = this.getLastTouchTimestamp() - itemValue.getLastTouchTimestamp();
            if (val == 0)
                val = this.getTouches() - itemValue.getTouches();
            return val > 0
                    ? 1
                    : val < 0
                        ? -1
                        : 0;
        }

        /**
         * Updates touches (+1) and reset the timestamp (makes the item young again)
         */
        private void touch() {
            this.touches++;
            this.lastTouchTimestamp = System.currentTimeMillis();
        }

        long getTouches() {
            return touches;
        }
    }
}
