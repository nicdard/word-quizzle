package translation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

/**
 * Simple unit test against MyMemory.
 * It assures that calling translate returns a notEmpty list.
 */
class MyMemoryApiTest {

    private final static TranslationService service = MyMemoryAPI.getInstance();

    @BeforeAll
    static void setServiceConfigurations() {
        service.setISOSourceLanguage("it");
        service.setISODestinationLanguage("en");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "teatro",
            "spettacolo",
            "interessante",
            "bottiglia",
            "acqua"
    })
    void testTranslate(String word) throws UnavailableTranslationException {
        try {
            List<String> translations = service.translate(word);
            Assertions.assertNotNull(translations);
            Assertions.assertFalse(translations.isEmpty());
        } catch (IllegalStateException e) {
            e.printStackTrace();
            System.out.println("Please review your test");
        }
    }
}
