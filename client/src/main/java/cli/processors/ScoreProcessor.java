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
        this.commandName = "show-score";
        this.expectedParameters = 1;
    }

    @Override
    public void process(String input) throws IOException {
        PacketPojo response = TCPHandler.getInstance().handle(new WQPacket(
                new PacketPojo(OperationCode.GET_SCORE)
        ));
        if (response.isSuccessfullResponse()) {
            System.out.println(response.getScores());
        }
        CliManager.getInstance().setNext(Prompt.MAIN_PROMPT);
    }
}
