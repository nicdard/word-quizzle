package translation;

import java.util.List;

/**
 * Abstraction of a words translation service based upon ISO 639-1 languages codes.
 * It is used as a common interface either for third-party service, ram cache and
 * physical cache and whatever can act as a translation-service.
 * It is modelled as a chain of responsibility pattern, to allow multiple level of
 * translation service collaboration (such as cache, first-party and third party services).
 */
public interface TranslationService {

    /**
     * Requests the translation(s) of a given word to the service.
     * It throws an IllegalStateException if the Source and Destination languages
     * were not set before invoking this method.
     * It throws and UnavailableTranslationException if any translation was found
     * for the given word in the given destination Language.
     * @param word
     * @return a non empty list of available translations
     * @throws UnavailableTranslationException
     * @throws IllegalStateException
     */
    List<String> translate(String word) throws UnavailableTranslationException, IllegalStateException;

    /**
     * Sets the source language of the service. The language must be in ISO 639-1 format.
     * (it can be implemented Using Local.getISOLanguages).
     * If another service is chained then it should change also the child language.
     * @param sourceLanguageCode
     * @return false if a non valid code is provided, true otherwise.
     */
    boolean setISOSourceLanguage(String sourceLanguageCode);

    /**
     * Sets the destination language of the service.
     * If another service is chained then it should change also the child language.
     * The language must be in in ISO 639-1 format
     * @param destinationLanguageCode
     * @return false if a non valid code is provided, true otherwise.
     */
    boolean setISODestinationLanguage(String destinationLanguageCode);

    /**
     * @return the current source language ISO 639-1 code
     */
    String getISOSourceLanguage();

    /**
     * @return the current destination language ISO 639-1 code
     */
    String getISODestinationLanguage();

    /**
     * Sets the next service to be called to perform the task (translate)
     * if the current one can't retrieve any translation.
     * @param service
     */
    void setNext(TranslationService service);

}
