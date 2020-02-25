package cli.processors;

import cli.CliManager;
import cli.CliState;
import cli.Prompt;
import connection.TCPHandler;
import protocol.OperationCode;
import protocol.WQPacket;
import protocol.json.PacketPojo;

import java.io.IOException;

public class ScoreProcessor extends BaseInputProcessor {

    ScoreProcessor() {
        this.expectedParameters = 1;
    }

    @Override
    public boolean validate(String input) {
        if (super.validate(input)) {
            return input.equalsIgnoreCase("show-score");
        } else {
            return false;
        }
    }

    @Override
    public void process(String input) throws InputProcessorException, IOException {
        if (this.validate(input)) {
            PacketPojo response = TCPHandler.getInstance().handle(new WQPacket(
                    new PacketPojo(OperationCode.GET_SCORE)
            ));
            if (response.isSuccessfullResponse()) {
                System.out.println(response.getScores());
            }
            CliManager.getInstance().setNext(new Prompt(
                    Prompt.MAIN_PROMPT,
                    BaseInputProcessor.getMainDispatcher(),
                    CliState.MAIN
            ));
        } else {
            super.process(input);
        }
    }
}
