package configurations;

import storage.Policy;

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
     * Default: IMMEDIATELY
     * Accepted values for this option:
     *  - 0: IMMEDIATELY
     *  - 1: WRITE_ON_READ
     *  - 2: ON_SESSION_CLOSE
     */
    private Policy storageAccessPolicy = Policy.IMMEDIATELY;
    /**
     * Configures the path to be used for the storage. Use relative paths.
     * Default: ${MODULE_WORKING_DIR}/internal
     */
    private String storagePath = "internal";

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
               /* case "-storageThreads":
                    int n = Integer.parseInt(rawValue);
                    storageThreads = n > 1 ? n : storageThreads;
                    break;*/
                case "-useStoragePolicy":
                    int p = Integer.parseInt(rawValue);
                    if (p < 0 || p > 2) {
                        System.out.println("-useStoragePolicy: please use a value between 0 and 2");
                    } else {
                        storageAccessPolicy = Policy.values()[p];
                    }
                    break;
                case "-useStoragePath":
                    if (!rawValue.isEmpty()) {
                        storagePath = rawValue;
                    }
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
}
