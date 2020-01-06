package translation;

import java.util.List;

/**
 * A map from a word to its translations
 */
public class Translation {
    private String sourceWord;
    private List<String> translations;

    Translation(String sourceWord, List<String> translations) {
        this.sourceWord = sourceWord;
        this.translations = translations;
    }

    public String getSourceWord() {
        return sourceWord;
    }

    public List<String> getTranslations() {
        return translations;
    }
}
