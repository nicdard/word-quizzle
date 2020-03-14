package translation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class DictionaryServiceTest {

    private static DictionaryService dictionaryService = DictionaryService.getInstance();

    Map<String, List<String>> requestWordsTranslation() {
        Set<String> words = new HashSet<>(Arrays.asList(
                "abate",
                "abbacchiato",
                "abbandonare",
                "abbastanza",
                "abbecedario",
                "abilmente",
                "abitanti",
                "abitare",
                "abito"
        ));
        Map<String, List<String>> dictionary = dictionaryService.getDictionary(words);
        boolean hasTranslatedAll = words.stream().allMatch(dictionary::containsKey);
        Assertions.assertTrue(hasTranslatedAll);
        dictionary.forEach((key, value) -> System.out.println(key + " " + value));
        return dictionary;
    }

    Map<String, List<String>> requestWordsTranslation(int n) {
        Map<String, List<String>> dictionary = dictionaryService.getDictionary(n);
        boolean hasTranslatedAll = dictionary.size() == n;
        Assertions.assertTrue(hasTranslatedAll);
        dictionary.forEach((key, value) -> System.out.println(key + " " + value));
        return dictionary;
    }

    @Test
    void parallelRequestsTest() throws ExecutionException, InterruptedException {
         // Executes in parallel
         CompletableFuture future1 = CompletableFuture
                .supplyAsync(this::requestWordsTranslation);
        CompletableFuture future2 = CompletableFuture
                .supplyAsync(this::requestWordsTranslation);
        CompletableFuture future3 = CompletableFuture
                .supplyAsync(() -> this.requestWordsTranslation(10));
        CompletableFuture.allOf(future1, future2, future3).get();
        // The three dictionaries should be equals
        Assertions.assertEquals(future1.get(), future2.get());
    }
}
