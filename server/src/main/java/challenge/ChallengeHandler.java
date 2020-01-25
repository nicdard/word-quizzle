package challenge;

import configurations.Config;
import connection.State;
import translation.DictionaryService;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ChallengeHandler implements Supplier<String> {

    private static final String OPPONENT_MACRO = "${opponent}";
    private static String challengeRules = "You and your opponent " + OPPONENT_MACRO + " will have " + Config.getInstance().getChallengeTime() + " seconds to translate " + Config.getInstance().getWordsForChallenge() + " words.";

    private String player1;
    private String player2;

    private Map<String, List<String>> dictionary;

    /** start time of the challenge */
    private long start;

    private long player1Delta;
    private long player2Delta;

    public ChallengeHandler(String player1, String player2) {
        this.player2 = player2;
        this.player1 = player1;
        // Builds a dictionary for the challenge.
        this.dictionary = DictionaryService.getInstance().getDictionary(
                Config.getInstance().getWordsForChallenge()
        );
    }

    public static String getChallengeRules() {
        return challengeRules;
    }

    public static String getOpponentMacro() {
        return OPPONENT_MACRO;
    }

    @Override
    public String get() {
        start = System.currentTimeMillis() / 1000;
        State state1 = NotifierService.getInstance().getConnection(this.player1);
        State state2 = NotifierService.getInstance().getConnection(this.player2);


        player1Delta = System.currentTimeMillis();

        player2Delta = System.currentTimeMillis();

        String results = " ";
        return results;
    }

}
