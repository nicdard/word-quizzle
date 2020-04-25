package cli;

import java.io.IOException;

public class CliManager {

    private Thread consumer;
    private NotificationConsumer notifier;
    private Prompt next;
    private CliState state;

    private static CliManager instance = getInstance();
    private CliManager() {
        // Initial prompt.
        next = Prompt.MAIN_PROMPT;
        state = CliState.MAIN;
    }
    public static CliManager getInstance() {
        if (instance == null) {
            instance = new CliManager();
        }
        return instance;
    }

    public void setNext(Prompt prompt) {
        this.next = prompt;
    }

    /**
     * Manages the interaction with the user.
     */
    public void start() {
        while (!this.shouldShutdown()) {
            this.executeNext();
        }
    }

    /**
     * If set, executes the next prompt. Sets error state otherwise.
     */
    private void executeNext() {
        Prompt prompt = this.next;
        this.setNext(null);
        if (prompt != null && this.hasTransition(prompt.getExecutionState())) {
            if (prompt.getExecutionState() != CliState.WAIT_BATTLE) {
                // if not in the waiting room it should shutdown consumer thread if present.
                if (this.notifier != null) {
                    this.notifier.stop();
                    this.notifier = null;
                }
            } else {
                // if entering the waiting room it should start a notifier thread.
                this.startNotifier();
            }
            try {
                // Sets the new state.
                this.state = prompt.getExecutionState();
                // Prompts the user and executes next step.
                prompt.execute();
            } catch (NullPointerException | IOException e) {
                // Re-add the prompt.
                CliManager.getInstance().setNext(prompt);
            }
        } else {
            // The application will stop because an error occurred
            // and no more prompts for the user are available.
            System.out.println("[ERROR] Unexpected error!");
            this.state = CliState.ERROR;
        }
    }

    /**
     * @return true if the application should stop.
     */
    public boolean shouldShutdown() {
        return this.state == CliState.EXIT
                || this.state == CliState.ERROR;
    }

    /**
     * Naive FSM transition matrix implementation.
     * @param nextState
     * @return
     */
    private boolean hasTransition(CliState nextState) {
        switch (state) {
            case WAIT_BATTLE:
                return nextState == CliState.MAIN
                        || nextState == CliState.ONGOING_BATTLE
                        || nextState == CliState.ERROR
                        || nextState == CliState.WAIT_BATTLE;
            case ONGOING_BATTLE:
                return nextState == CliState.ONGOING_BATTLE
                        || nextState == CliState.MAIN
                        || nextState == CliState.ERROR;
            case MAIN:
                return nextState == CliState.MAIN
                        || nextState == CliState.ONGOING_BATTLE
                        || nextState == CliState.ERROR
                        || nextState == CliState.WAIT_BATTLE
                        || nextState == CliState.EXIT;
            case EXIT:
                return nextState == CliState.EXIT;
            default:
            case ERROR:
                return false;
        }
    }


    public NotificationConsumer getNotifier() {
        return notifier;
    }

    public void cleanUp() throws InterruptedException {
        Prompt.cleanUp();
        if (this.notifier != null) notifier.stop();
        if (this.consumer != null) consumer.join();
    }


    /**
     * Starts a new notifierThread if none is already running.
     */
    public void startNotifier() {
        if (this.notifier == null) {
            this.notifier = new NotificationConsumer();
            this.consumer = new Thread(notifier);
            this.consumer.start();
        }
    }
}
