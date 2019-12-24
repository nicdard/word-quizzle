package server;

public class Main {

    private final static String USAGE_MSG = "Usage: ";
    private final static int DEFAULT_DICTIONARY_CHALLENGE_LENGTH = 10;
    private static DictionaryService dictionaryService;

    public static void main(String[] args) {
        // 0. Initialisation
        int dictionaryLength = DEFAULT_DICTIONARY_CHALLENGE_LENGTH;
        try {
            if (args.length > 0) {
                dictionaryLength = Integer.parseInt(args[0]);
                if (dictionaryLength < 0) dictionaryLength = DEFAULT_DICTIONARY_CHALLENGE_LENGTH;
            }
        } catch (Exception e) {
            System.out.println(USAGE_MSG);
            return;
        }
        Main.dictionaryService = new DictionaryService(dictionaryLength);
    }
}
