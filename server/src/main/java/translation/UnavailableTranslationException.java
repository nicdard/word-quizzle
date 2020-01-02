package translation;

/**
 * Exception thrown by any translation service when it wasn't possible to get
 * any translation for the given word in the given destination language
 */
public class UnavailableTranslationException extends Exception {

    /** The source language (the language of the requested word) */
    private String sourceLanguage;
    /** The language of the translation */
    private String destinationLanguage;
    /** The word to translate */
    private String requestedWord;

    UnavailableTranslationException(
            String sourceLanguage,
            String destinationLanguage,
            String requestedWord
    ) {
        this.sourceLanguage = sourceLanguage;
        this.destinationLanguage = destinationLanguage;
        this.requestedWord = requestedWord;
    }

    /**
     * Extended constructor which provides also a message error
     * @param sourceLanguage
     * @param destinationLanguage
     * @param requestedWord
     * @param msg
     */
    UnavailableTranslationException(
            String sourceLanguage,
            String destinationLanguage,
            String requestedWord,
            String msg
    ) {
        super(msg);
        this.sourceLanguage = sourceLanguage;
        this.destinationLanguage = destinationLanguage;
        this.requestedWord = requestedWord;
    }

    /**
     * Suppress stackTrace for a lightweight exception
     * @return this
     */
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

    @Override
    public String toString() {
        return this.sourceLanguage + " "
                + this.destinationLanguage + " "
                + this.requestedWord + " "
                + this.getMessage();
    }
}
