package cli.processors;

import cli.CliManager;
import cli.CliState;
import cli.Prompt;

import java.io.IOException;

public class ExitProcessor extends BaseInputProcessor {

    ExitProcessor() {
        this.expectedParameters = 1;
    }

    @Override
    public boolean validate(String input) {
        if (super.validate(input)) {
            return input.equalsIgnoreCase("exit");
        } else {
            return false;
        }
    }

    @Override
    public void process(String input) throws InputProcessorException, IOException {
        if (this.validate(input)) {
            CliManager.getInstance().setNext(new Prompt(
                    Prompt.EXITING,
                    null,
                    CliState.EXIT
            ));
        } else {
            super.process(input);
        }
    }
}

