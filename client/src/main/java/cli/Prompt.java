package cli;

import cli.processors.BaseInputProcessor;
import cli.processors.InputProcessor;
import cli.processors.InputProcessorException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Prompt {

    private static final String DEFAULT_PROMPT = "> ";
    private static String PROMPT = DEFAULT_PROMPT;
    public static final String READER = "";
    public static final String EXITING = "\n===========\nExiting...\nPlease press enter to complete.\n";
    public static final String HELP = "\nAvailable Commands:" +
            "\n - register <nickname> <password>: registers an user to WQ with the given credentials;" +
            "\n - login <nickname> <password>" +
            "\n - logout" +
            "\n - add-friend <nickFriend>: sets the friendship between you and nickFriend" +
            "\n - show-friends: lists all of your friends" +
            "\n - challenge: <nickFriend> requests a challenge to nickFriend" +
            "\n - show-score: gets total user score" +
            "\n - show-ranking-list: shows the ranking list including only you and your friends" +
            "\n - wait-challenge" +
            "\n - exit\n";
    private static final String WORD_MACRO = "${WORD}";
    private static final String ASK_WORD = WORD_MACRO + " " + DEFAULT_PROMPT;

    public static final Prompt MAIN_PROMPT = new Prompt(
            "\n[help]\n",
            BaseInputProcessor.getMainDispatcher(),
            CliState.MAIN
    );

    // The prompt reader instance.
    private static BufferedReader reader = new BufferedReader(
            new InputStreamReader(System.in)
    );

    private String prompt;
    private InputProcessor processor;

    private CliState executionState;
    private boolean shouldPrintPrompt;

    public Prompt(String prompt, InputProcessor processor, CliState state) {
        if (prompt == null || state == null)
            throw new IllegalArgumentException("Should not be null");
        this.prompt = prompt;
        this.processor = processor;
        this.executionState = state;
        this.shouldPrintPrompt = true;
    }

    public Prompt(String prompt, InputProcessor chain, CliState state, boolean shouldPrintPrompt) {
        this(prompt, chain, state);
        this.shouldPrintPrompt = shouldPrintPrompt;
    }

    public void printPrompt() {
        if (shouldPrintPrompt) System.out.print(prompt + PROMPT);
        else System.out.print(prompt);
    }

    public void execute() throws IOException {
        printPrompt();
        String input = reader.readLine().trim();
        if (processor != null) {
            try {
                // Validate and process the input.
                processor.validate(input).process(input);
            } catch (InputProcessorException e) {
                System.out.println(e.getMessage());
                CliManager.getInstance().setNext(MAIN_PROMPT);
            } catch (IOException e) {
                CliManager.getInstance().setNext(new Prompt(
                        "[ERROR] IOError, An unexpected communication error occurred\n" + Prompt.EXITING,
                        null,
                        CliState.ERROR
                ));
            }
        }
    }

    public CliState getExecutionState() {
        return executionState;
    }

    /**
     * Used to change the prompt displayed. It appends a blank space and the default prompt.
     * When a null value is provided it restores the default one.
     * @param prompt
     */
    public static void setPrompt(String prompt) {
        if (prompt == null) PROMPT = DEFAULT_PROMPT;
        else PROMPT = prompt + " " + DEFAULT_PROMPT;
    }

    public static String getAskWordStringPrompt(String word) {
        return ASK_WORD.replace(WORD_MACRO, word);
    }

    public static void cleanUp() {
        try {
            Prompt.reader.close();
        } catch (IOException e) {
            System.out.println(e);
        }
    }
}
