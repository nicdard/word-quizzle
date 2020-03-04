package translation;

import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

/**
 * A wrapper for TranslationService which enables concurrent translations.
 * This solves especially third-party service calls performance issues.
 */
public class TranslationSupplier implements Supplier<Translation> {

    private String word;

    public TranslationSupplier(String word) {
        this.word = word;
    }

    @Override
    public Translation get() {
        TranslationService chain = BaseTranslationService.getChain();
        try {
            return new Translation(word, chain.translate(word));
        } catch (UnavailableTranslationException e) {
            throw new RuntimeException(e);
        }
    }
}
