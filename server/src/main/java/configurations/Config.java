package configurations;

import protocol.WQPacket;
import storage.Policy;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * A singleton that holds all configurations of the server.
 */
public class Config {

    /** Singleton instance */
    private static Config configurations;
    private Config() { }

    /** Returns the instance of the class */
    public static Config getInstance() {
        if (configurations == null) {
            configurations = new Config();
        }
        return configurations;
    }

    /**
     * True: Logs Server info messages.
     * Default: false.
     */
    private boolean isDebug = false;
    /**
     * True: use a cache layer if possible to translate the words
     * Default: false
     */
    private boolean useTranslationCache = false;
    /**
     * @link useTranslationCache: This option is considered only if
     * useTranslationCache is set to true.
     * It indicates the maximum cache size.
     * Default: 1000 items.
     */
    private long cacheMaxSize = 1000;
    /**
     * Configures the language to be used as source language
     * Default: it
     */
    private String ISOSourceLanguage = "it";
    /**
     * Configures the language in which request translations
     * Default: en
     */
    private String ISODestinationLanguage = "en";

    /**
     * Configures the write/read access policy to the file system
     * only for those information that are not considered prioritised,
     * which are stored immediately (ex. registration info).
     * Default: ON_SESSION_CLOSE
     * Accepted values for this option:
     *  - IMMEDIATELY
     *  - ON_SESSION_CLOSE
     */
    private Policy storageAccessPolicy = Policy.ON_SESSION_CLOSE;
    /**
     * Configures the path to be used for the storage. Use relative paths.
     * Default: ${MODULE_WORKING_DIR}/internal
     */
    private String storagePath = "internal";

    /**
     * Configures dictionary file path.
     * Default: ${MODULE_WORKING_DIR}/src/main/resources/dictionary.txt
     */
    private String dictionaryFilePath = String.join(File.separator,
            "src", "main", "resources", "dictionary.txt"
    );

    /**
     * Configures the number of words to translate in a challenge.
     * Default: 20
     */
    private int wordsForChallenge = 10;
    /**
     * Configures the maximum time for a request in ms.
     * Default: 5s
     */
    private int challengeRequestTimeout = 5000;
    /**
     * Configures the time given to a user to complete a challenge in seconds.
     * Default: 50s -> with default config (10 words): 5s per word.
     */
    private int challengeTime = 50;
    /**
     * Configures the points gained by an user when a correct answer is provided.
     */
    private int wordBonus = 3;
    /**
     * Configures the points lost by an user when a wrong answer is provided.
     */
    private int wordMalus = 1;
    /**
     * Configures the additional points gained by an user when winning a challenge.
     */
    private int winnerExtraPoints = 5;

    /**
     * Parses the command line arguments and initialise the config fields.
     * @param args an array of command line values
     */
    public void parseCommandLineArguments(String[] args) {
        for (String arg : args) {
            String[] keyValue = arg.split("=");
            if (keyValue.length != 2) {
                System.out.println(
                        "[WARNING] Malformed option: "
                        + arg +
                        "\n-> this option will be ignored."
                );
            }
            String key = keyValue[0];
            String rawValue = keyValue[1];
            switch (key) {
                case "-setDebug":
                    isDebug = Boolean.parseBoolean(rawValue);
                    break;
                case "-useTranslationCache":
                    useTranslationCache = Boolean.parseBoolean(rawValue);
                    break;
                case "-cacheMaxSize":
                    cacheMaxSize = Long.parseLong(rawValue);
                    break;
                case "-useISOSourceLang":
                    ISOSourceLanguage = rawValue;
                    break;
                case "-useISODestinationLang":
                    ISODestinationLanguage = rawValue;
                    break;
                case "-useStoragePolicy":
                    storageAccessPolicy = Policy.valueOf(rawValue);
                    break;
                case "-useStoragePath":
                    if (!rawValue.isEmpty()) {
                        storagePath = rawValue;
                    }
                    break;
                case "-useDictionary":
                    this.dictionaryFilePath = rawValue;
                    break;
                case "-wordsForChallenge":
                    this.wordsForChallenge = Integer.parseInt(rawValue);
                    break;
                case "-challengeRequestTimeout":
                    this.challengeRequestTimeout = Integer.parseInt(rawValue);
                    break;
                case "-challengeTime":
                    this.challengeTime = Integer.parseInt(rawValue);
                    break;
                default:
                    System.out.println("[WARNING] Unrecognised option: " + key + "\n->this option will be ignored");
            }
        }
    }

    public boolean useTranslationCache() {
        return useTranslationCache;
    }

    public long getCacheMaxSize() {
        return cacheMaxSize;
    }

    public String getISOSourceLanguage() {
        return ISOSourceLanguage;
    }

    public String getISODestinationLanguage() {
        return ISODestinationLanguage;
    }

    public Policy getStorageAccessPolicy() {
        return storageAccessPolicy;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public String getDictionaryFilePath() {
        return dictionaryFilePath;
    }

    public int getWordsForChallenge() {
        return wordsForChallenge;
    }

    public int getChallengeRequestTimeout() {
        return challengeRequestTimeout;
    }

    public int getChallengeTime() {
        return challengeTime;
    }

    public int getWordBonus() {
        return wordBonus;
    }

    public int getWordMalus() {
        return wordMalus;
    }

    public int getWinnerExtraPoints() {
        return winnerExtraPoints;
    }

    public boolean isDebug() {
        return isDebug;
    }

    /**
     * Prints debug messages.
     * @param message
     */
    public void debugLogger(String message) {
        if (this.isDebug) {
            System.out.println("[SERVER DEBUG] " + message);
        }
    }

    /**
     * Prints an error stack trace information in debug mode.
     * @param t
     */
    public void debugLogger(Throwable t, String ...additionalInfo) {
        this.debugLogger(Arrays.stream(t.getStackTrace())
            .map(StackTraceElement::toString)
            .reduce((a, el) -> String.join("\n", a, el))
            .orElse("")
            .concat("\n" + Arrays.stream(additionalInfo)
                    .reduce((a, el) -> String.join("\n", a, el))
                    .orElse("")
            )
        );
    }

    /**
     * Tries to deserialize a packet from a byteBuffer and prints out the operation code.
     * @param byteBuffer
     */
    public void debugLogger(ByteBuffer byteBuffer) {
        try {
            this.debugLogger(WQPacket.fromBytes(byteBuffer).getOperationCode().name());
        } catch (Exception e) {
            this.debugLogger(e);
        } finally {
            byteBuffer.rewind();
        }
    }
}
