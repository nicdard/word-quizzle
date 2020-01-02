package translation;

import java.util.List;

/**
 * https://mymemory.translated.net/doc/spec.php
 * A singleton which implements MyMemory Translation service API.
 */
public class MyMemoryAPI extends BaseTranslationService {

    /** Singleton pattern */
    private static MyMemoryAPI instance;

    private MyMemoryAPI() {}

    static MyMemoryAPI getInstance() {
        if (instance == null) {
            instance = new MyMemoryAPI();
        }
        return instance;
    }

    @Override
    public List<String> translate(String word) throws UnavailableTranslationException, IllegalStateException {
        return null;
    }
}
