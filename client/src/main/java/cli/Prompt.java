package cli;

import cli.processors.BaseInputProcessor;
import cli.processors.InputProcessor;
import cli.processors.InputProcessorException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Prompt implements Comparable<Prompt> {


    public static final String MAIN_PROMPT = "\n[help]\n> ";
    public static final String NAME_MACRO = "${name}";
    public static final String ACCEPTANCE_ALERT = "\nUser "+ NAME_MACRO + " wants to challenge you!\nDo you want to accept? [Y/n] \n> ";
    public static final String EXITING = "\n===========\nExiting...\nPlease press enter to complete.";
    public static final String HELP = "\nAvailable Commands:" +
            "\n - register <nickname> <password>: registers the user to WQ with the " +
            "given credentials;\n - login <nickname> <password>\n - logout" +
            "\n - add-friend <nickFriend>: sets a friendship between this user and nickFriend" +
            "\n - list-friends: lists all friends of this user\n " +
            "- challenge: <nickFriend> requests a challenge to nickFriend\n " +
            "- show-score: gets total user score\n " +
            "- show-ranking-list: shows the ranking list including only this " +
            "user and his/her friends\n- wait-challenge\n - exit\n> ";
    public static final String CHECKED_ERROR = "\nInput Error: ";

    // The prompt reader instance.
    private static BufferedReader reader = new BufferedReader(
            new InputStreamReader(System.in)
    );

    private String prompt;
    private InputProcessor processorChain;

    private CliState executionState;

    public Prompt(String prompt, InputProcessor chain, CliState state) {
        if (prompt == null || state == null)
            throw new IllegalArgumentException("Should not be null");
        this.prompt = prompt;
        this.processorChain = chain;
        this.executionState = state;
    }

    public void execute() throws IOException {
        System.out.print(prompt);
        String input = reader.readLine();
        if (processorChain != null) {
            try {
                processorChain.process(input);
            } catch (InputProcessorException e) {
                // TODO pretty print error message.
                e.printStackTrace();
                CliManager.getInstance().enqueue(
                        new Prompt(MAIN_PROMPT,
                                BaseInputProcessor.getMainDispatcher(),
                                CliState.MAIN
                        )
                );
            }
        }
    }

    public CliState getExecutionState() {
        return executionState;
    }

    @Override
    public int compareTo(Prompt prompt) {
        if (prompt == null) return 1;
        return this.getExecutionState().compareTo(prompt.getExecutionState());
    }
}
