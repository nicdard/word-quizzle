package translation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * https://mymemory.translated.net/doc/spec.php
 * A singleton which implements MyMemory Translation service API.
 * NOTE: In multithreaded application don't lazy load this class or,
 * if needed, make getInstance synchronized
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

    /**
     * Source and language pair, separated by the | symbol. Use ISO standard names or RFC3066. Mandatory.
     * */
    private final static String LANGUAGE_ATTRUBUTE = "langpair";
    /**
     * The separator of the language pair values
     */
    private final static String LANGUAGE_VALUE_SEPARATOR = "|";
    /**
     * The sentence you want to translate.
     * Use UTF-8.
     * Max 500 bytes.
     * Mandatory.
     */
    private final static String QUESTION_ATTRIBUTE = "q";
    /**
     * The separator for an attribute and its value.
     */
    private final static String KEY_VALUE_SEPARATOR = "=";
    /**
     * Attributes separator.
     */
    private final static String SEPARATOR = "&";
    /**
     * The base url of the service
     */
    private final static String SERVICE_URL = "https://api.mymemory.translated.net/get?";

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<String> translate(String word) throws UnavailableTranslationException, IllegalStateException {
        this.checkTranslateContract();
        try {
            URL url = new URL(MyMemoryAPI.SERVICE_URL
                    + MyMemoryAPI.QUESTION_ATTRIBUTE + MyMemoryAPI.KEY_VALUE_SEPARATOR + word
                    + MyMemoryAPI.SEPARATOR
                    + MyMemoryAPI.LANGUAGE_ATTRUBUTE + MyMemoryAPI.KEY_VALUE_SEPARATOR
                        + this.getISOSourceLanguage()
                        + MyMemoryAPI.LANGUAGE_VALUE_SEPARATOR
                        + this.getISODestinationLanguage()
            );
            MyMemoryResponse response = objectMapper.readValue(url, MyMemoryResponse.class);
            if (response.responseData.match != null && response.matches != null && response.matches.length > 0) {
                return Arrays.stream(response.matches)
                        .map(match -> match.translation)
                        .collect(Collectors.toList());
            } else {
                super.translate(word);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return super.translate(word);
        }
        return null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class MyMemoryResponse {
        private static class ResponseData {
            @JsonProperty("translatedText") String translatedText;
            @JsonProperty("match") Double match;
        }
        @JsonIgnoreProperties(ignoreUnknown = true)
        private static class Match {
            @JsonProperty("translation") String translation;
            @JsonProperty("subject") String subject;
            @JsonProperty("match") double matchRate;
        }
        @JsonProperty("responseData") ResponseData responseData;
        @JsonProperty("responseStatus") Integer responseStatusCode;
        @JsonProperty("matches") Match[] matches;
    }
}
