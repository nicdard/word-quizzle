package cli;

import cli.processors.BaseInputProcessor;

import java.io.IOException;

public class CliManager {

    private Thread consumer;
    private NotificationConsumer notifier;
    private Prompt next;
    private CliState state;

    private static CliManager instance = getInstance();
    private CliManager() {
        // Initial prompt.
        next = new Prompt(
                Prompt.MAIN_PROMPT,
                BaseInputProcessor.getMainDispatcher(),
                CliState.MAIN
        );
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

    public void executeNext() {
        Prompt prompt = this.next;
        this.setNext(null);
        if (prompt != null) {
            // if not waiting room should shutdown consumer thread if present.
            if (prompt.getExecutionState() != CliState.WAIT_BATTLE) {
                if (this.notifier != null) this.notifier.stop();
            }
            if (this.hasTransition(prompt.getExecutionState())) {
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
                //this.onReject();
            }
        } else {
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
     * Verifies that the
     * @param nextState
     * @return
     */
    private boolean hasTransition(CliState nextState) {
        switch (state) {
            case WAIT_BATTLE:
                return nextState == CliState.MAIN
                        || nextState == CliState.ONGOING_BATTLE
                        || nextState == CliState.ERROR;
            case ONGOING_BATTLE:
                return nextState == CliState.ONGOING_BATTLE
                        || nextState == CliState.AFTER_BATTLE
                        || nextState == CliState.ERROR;
            case AFTER_BATTLE:
                return nextState == CliState.MAIN
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

    // TODO
    private void onReject() {

    }

    public void setConsumer(Thread consumer) {
        this.consumer = consumer;
    }

    public NotificationConsumer getNotifier() {
        return notifier;
    }

    public void cleanUpNofier() throws InterruptedException {
        if (this.notifier != null) notifier.stop();
        if (this.consumer != null) consumer.join();
    }

    public void setNotifier(NotificationConsumer notifier) {
        this.notifier = notifier;
    }
}
