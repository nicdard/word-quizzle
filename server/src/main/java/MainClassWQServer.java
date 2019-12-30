public class MainClassWQServer {

    private final static String USAGE_MSG = "Usage: ";
    private final static int DEFAULT_DICTIONARY_CHALLENGE_LENGTH = 10;
    private static DictionaryService dictionaryService;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Starting server");
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
        MainClassWQServer.dictionaryService = new DictionaryService(dictionaryLength);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    System.out.println("Ciao");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
        System.out.println("Bye server");
    }
}
