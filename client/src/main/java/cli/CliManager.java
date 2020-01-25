package cli;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

public class CliManager {

    private BlockingQueue<Prompt> prompts;
    private CliState state;

    private static CliManager instance = getInstance();
    private CliManager() {
        prompts = new PriorityBlockingQueue<>();
        state = CliState.MAIN;
    }
    public static CliManager getInstance() {
        if (instance == null) {
            instance = new CliManager();
        }
        return instance;
    }

    public void enqueue(Prompt prompt) {
        this.prompts.offer(prompt);
    }

    public void executeNext() {
        Prompt prompt = null;
        try {
            prompt = this.prompts.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (prompt != null) {
            if (this.hasTransition(prompt.getExecutionState())) {
                try {
                    // Sets the new state.
                    this.state = prompt.getExecutionState();
                    // Prompts the user and executes next step.
                    prompt.execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                // TODO Discard prompt and call onReject
            }
        } else {
            System.out.println("[WARNING] No more prompts!");
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
            case REQUEST_BATTLE:
                return nextState == CliState.MAIN
                        || nextState == CliState.ONGOING_BATTLE
                        || nextState == CliState.ERROR;
            case ONGOING_BATTLE:
                return nextState == CliState.ONGOING_BATTLE
                        || nextState == CliState.AFTER_BATTLE
                        || nextState == CliState.ERROR;
            case AFTER_BATTLE:
                return nextState == CliState.MAIN
                        || nextState == CliState.ACCEPT_BATTLE_ALERT
                        || nextState == CliState.ERROR;
            case ACCEPT_BATTLE_ALERT:
                return nextState == CliState.MAIN
                        || nextState == CliState.ONGOING_BATTLE
                        || nextState == CliState.ERROR;
            case MAIN:
                return nextState == CliState.MAIN
                        || nextState == CliState.REQUEST_BATTLE
                        || nextState == CliState.ACCEPT_BATTLE_ALERT
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
}
