package cli.processors;

import cli.CliManager;
import cli.CliState;
import cli.Prompt;

public class HelpInputProcessor extends BaseInputProcessor {

    HelpInputProcessor() {
        this.expectedParameters = 1;
    }

    @Override
    public boolean validate(String input) {
        if (super.validate(input)) {
            return input.equalsIgnoreCase("help");
        } else {
            return false;
        }
    }

    @Override
    public void process(String input) throws InputProcessorException {
        if (this.validate(input)) {
            CliManager.getInstance().enqueue(new Prompt(
                    Prompt.HELP,
                    BaseInputProcessor.getMainDispatcher(),
                    CliState.MAIN
            ));
        } else {
            super.process(input);
        }
    }
}
