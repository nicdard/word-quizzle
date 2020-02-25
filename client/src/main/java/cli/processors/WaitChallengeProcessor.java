package cli.processors;

import cli.CliManager;
import cli.CliState;
import cli.NotificationConsumer;
import cli.Prompt;

import java.io.IOException;

public class WaitChallengeProcessor extends BaseInputProcessor {

    WaitChallengeProcessor() {
        this.expectedParameters = 1;
    }

    @Override
    public boolean validate(String input) {
        if (super.validate(input)) {
            return input.equalsIgnoreCase("wait-challenge");
        } else {
            return false;
        }
    }

    @Override
    public void process(String input) throws InputProcessorException, IOException {
        if (this.validate(input)) {
            System.out.println("You entered the waiting room, when a request of challenge from a friend of yours arrive it will be displayed here." +
                    "\nYou can select one of the available typing the number associated to it." +
                    "\nJust press enter to leave at any time or type exit.");

            NotificationConsumer notifier = new NotificationConsumer();
            Thread consumer = new Thread(notifier);
            consumer.start();
            CliManager.getInstance().setNotifier(notifier);
            CliManager.getInstance().setConsumer(consumer);
            Prompt reader = new Prompt(
                    Prompt.READER,
                    new AcceptanceBattleProcessor(),
                    CliState.WAIT_BATTLE,
                    false
            );
            CliManager.getInstance().setNext(reader);
        } else {
            super.process(input);
        }
    }
}


