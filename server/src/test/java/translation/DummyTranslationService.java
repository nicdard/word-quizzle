package translation;

import java.util.*;

/**
 * A translationService mock to be used in tests.
 * It provides just 5 entry by default (from italian to english):
 *   ciao, nome, verbo, codice, numero.
 */
class DummyTranslationService extends BaseTranslationService {

    private Map<String, List<String>> t = new HashMap<>(5);

    DummyTranslationService() {
        t.put("ciao", Collections.singletonList("hello"));
        t.put("nome", Collections.singletonList("name"));
        t.put("verbo", Collections.singletonList("verb"));
        t.put("codice", Collections.singletonList("code"));
        t.put("numero", Collections.singletonList("number"));
    }

    @Override
    public List<String> translate(String word) throws UnavailableTranslationException, IllegalStateException {
        this.checkTranslateContract();
        if (this.t.containsKey(word)) {
            return this.t.get(word);
        } else {
            return super.translate(word);
        }
    }
}
