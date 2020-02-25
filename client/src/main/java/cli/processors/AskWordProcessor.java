package cli.processors;

import cli.CliManager;

import java.io.IOException;

public class AskWordProcessor extends BaseInputProcessor {

    String word;

    AskWordProcessor(String word) {
        this.word = word;
        this.expectedParameters = 1;
    }

    @Override
    public void process(String input) throws InputProcessorException, IOException {
        if (this.validate(input)) {
            System.out.println("Grazie");
        } else {
            super.process(input);
        }
    }
}
