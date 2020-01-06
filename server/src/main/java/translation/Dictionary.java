package translation;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

// FIXME TODO it can be just a function
public class Dictionary {

    private Set<String> words;

    public Dictionary(Set<String> words) {
        if (words != null) {
            this.words = words;
        }
    }

    public Map<String, List<String>> getDictionary() {
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
        try {
            Map<String, List<String>> dictionary = new ConcurrentHashMap<>();
            translationsMapping.get().forEach(t ->
                    dictionary.put(t.getSourceWord(), t.getTranslations())
            );
            return dictionary;
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        throw new RuntimeException("Impossible to get the words");
    }

}
