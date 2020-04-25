package cli.processors;

import cli.CliManager;
import cli.CliState;
import cli.Prompt;

public class WaitChallengeProcessor extends BaseInputProcessor {

    WaitChallengeProcessor() {
        this.commandName = "wait-challenge";
        this.expectedParameters = 1;
    }

    @Override
    public void process(String input) {
        System.out.println("You entered the waiting room, when a request of challenge from a friend of yours arrive it will be displayed here." +
                "\nYou can select one of the available typing the number associated to it." +
                "\nJust press enter to leave at any time or type exit.");
        Prompt reader = new Prompt(
                Prompt.READER,
                new AcceptanceBattleProcessor(),
                CliState.WAIT_BATTLE,
                false
        );
        CliManager.getInstance().setNext(reader);
    }
}


