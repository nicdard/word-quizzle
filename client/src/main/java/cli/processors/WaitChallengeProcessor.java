package cli.processors;

import cli.CliManager;
import cli.CliState;
import cli.Prompt;

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
    public void process(String input) throws InputProcessorException {
        if (this.validate(input)) {
            System.out.println("Press enter to leave at any time.");
            CliManager.getInstance().executeNext();
        } else {
            super.process(input);
        }
    }
}


