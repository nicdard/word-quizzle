package translation;

import configurations.Config;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Implements the chain of responsibility base forwarding logic, exposes methods to manage the building process and common parameters settings.
 * Translation services are linkable only if source and destination languages of both parent and child are the same.
 * Here when a parent language is changed also the child language change consistently.
 */
public abstract class BaseTranslationService implements TranslationService {

    /** The next request handler */
    private TranslationService next;

    /**
     * The current ISO 639-1 code of the language in which the service
     * receives the words to be translated
     */
    private String sourceLanguageCode;
    /**
     * The current ISO 639-1 code of the language in which the service
     * translates the words.
     */
    private String destinationLanguageCode;

    /**
     * Builds and configures a chain of TranslationServices according to the global Config.
     * @return the head of the TranslationServices chain
     */
    public static TranslationService assemblyChain() {
        Config config = Config.getInstance();
        // This is the third-party service used to translate the words.
        TranslationService head = MyMemoryAPI.getInstance();
        // Chain assembler section.
        if (config.useTranslationCache()) {
            head.setNext(TranslationsPool.getInstance(config.getCacheMaxSize()));
        }
        // Configures languages only at the end of the chain assembly process
        // to propagate the settings.
        head.setISOSourceLanguage(config.getISOSourceLanguage());
        head.setISODestinationLanguage(config.getISODestinationLanguage());
        return head;
    }

    @Override
    public void setNext(TranslationService service) {
        this.next = service;
    }

    @Override
    public List<String> translate(String word) throws UnavailableTranslationException, IllegalStateException {
        if (this.next != null
            && this.next.getISOSourceLanguage().equals(this.getISOSourceLanguage())
            && this.next.getISODestinationLanguage().equals(this.getISODestinationLanguage())
        ) {
            return this.next.translate(word);
        }
        // If I can't translate the given word and I can't forward
        // the request to anyone I declare the failure and I signal it.
        throw new UnavailableTranslationException(
                this.getISOSourceLanguage(),
                this.getISODestinationLanguage(),
                word,
                "Unavailable translation"
        );
    }

    @Override
    public boolean setISOSourceLanguage(String sourceLanguageCode) {
        if (BaseTranslationService.isISOLanguage(sourceLanguageCode)) {
            this.sourceLanguageCode = sourceLanguageCode;
            // Propagate the changing to the whole chain
            if (this.next != null) {
                this.next.setISOSourceLanguage(sourceLanguageCode);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean setISODestinationLanguage(String destinationLanguageCode) {
        if (BaseTranslationService.isISOLanguage(destinationLanguageCode)) {
            this.destinationLanguageCode = destinationLanguageCode;
            // Propagate the change to the whole chain
            if (this.next != null) {
                this.next.setISODestinationLanguage(destinationLanguageCode);
            }
            return true;
        }
        return false;
    }

    @Override
    public String getISOSourceLanguage() {
        return this.sourceLanguageCode;
    }

    @Override
    public String getISODestinationLanguage() {
        return this.destinationLanguageCode;
    }

    /**
     * @param code the string representing a potential ISO 639-1 code
     * @return true if the provided code is an ISO 639-1 language code, false otherwise.
     */
    private static boolean isISOLanguage(String code) {
        return Arrays.asList(Locale.getISOLanguages()).contains(code);
    }
}
