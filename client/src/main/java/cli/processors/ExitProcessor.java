package cli.processors;

import cli.CliManager;
import cli.CliState;
import cli.Prompt;

public class ExitProcessor extends BaseInputProcessor {

    ExitProcessor() {
        this.commandName = "exit";
        this.expectedParameters = 1;
    }

    @Override
    public void process(String input) {
        CliManager.getInstance().setNext(new Prompt(
                Prompt.EXITING,
                null,
                CliState.EXIT
        ));
    }
}

