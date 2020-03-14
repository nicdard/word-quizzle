package cli.processors;

import cli.CliManager;
import cli.CliState;
import cli.Prompt;

public class HelpInputProcessor extends BaseInputProcessor {

    HelpInputProcessor() {
        this.commandName = "help";
        this.expectedParameters = 1;
    }

    @Override
    public void process(String input) {
        CliManager.getInstance().setNext(new Prompt(
                Prompt.HELP,
                BaseInputProcessor.getMainDispatcher(),
                CliState.MAIN
        ));
    }
}
