package translation;

import configurations.Config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Dictionary service.
 * It loads the server's word list and expose a method
 * to get a dictionary of either a set of provided words
 * or n randomly chosen words.
 * NOTE: In multithreaded application don't lazy load this class or,
 * if needed, make getInstance synchronized
 */
public class DictionaryService {

    /**
     * The words known by the server.
     */
    private List<String> words;

    /** The singleton instance */
    private static DictionaryService instance = getInstance();

    private DictionaryService() throws IOException {
        this.words = new Vector<>();
        // Loads words from disk
        this.words.addAll(Files.readAllLines(
                Paths.get(Config.getInstance().getDictionaryFilePath())
        ));
    }

    public static DictionaryService getInstance() {
        if (instance == null) {
            try {
                instance = new DictionaryService();
            } catch (IOException e) {
                throw new RuntimeException("ERROR: words file not readable");
            }
        }
        return instance;
    }

    /**
     * Gets a dictionary of n randomly chosen words from the service known set of words.
     * @param n
     * @return a map from word to translations.
     * @throws NoSuchElementException
     */
    public Map<String, List<String>> getDictionary(int n) throws NoSuchElementException {
        if (n > this.words.size()) {
            throw new NoSuchElementException("This requested number can't be fulfilled with the actual word list!");
        }
        // Builds a random subset.
        Set<String> set = ThreadLocalRandom.current().ints(n, 0, this.words.size())
                .mapToObj(i -> this.words.get(i))
                .collect(Collectors.toSet());
        if (set.size() != n) {
            // Fills empty slots.
            Iterator<String> wordsGenerator = this.words.iterator();
            while (set.size() != n && wordsGenerator.hasNext()) {
                set.add(wordsGenerator.next());
            }
        }
        // Gets the translations.
        return this.getDictionary(set);
    }


    /**
     * Gets a dictionary from a given set of words.
     * @param words
     * @throws NoSuchElementException if any of the provided word is untranslatable.
     * @return a map from word to translations.
     */
    public Map<String, List<String>> getDictionary(Set<String> words) throws NoSuchElementException {
        try {
            List<CompletableFuture<Translation>> promises = words.stream()
                    .filter(word -> word != null && !word.isEmpty())
                    .map(word -> CompletableFuture.supplyAsync(new TranslationSupplier(word)))
                    .collect(Collectors.toList());
            CompletableFuture<List<Translation>> translationsMapping =
                    CompletableFuture.allOf(
                            promises.toArray(new CompletableFuture[0])
                    ).thenApply(v -> promises.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList())
                    );
            Map<String, List<String>> dictionary = new HashMap<>();
            translationsMapping.get().forEach(t ->
                    dictionary.put(t.getSourceWord(), t.getTranslations())
            );
            return dictionary;
        } catch (CompletionException | InterruptedException | ExecutionException e) {
            configurations.Config.getInstance().debugLogger(e);
            throw new NoSuchElementException("Impossible to get the words");
        }
    }

}
