package cli.processors;

import cli.CliManager;
import cli.CliState;
import cli.Prompt;
import connection.TCPHandler;
import protocol.OperationCode;
import protocol.ResponseCode;
import protocol.WQPacket;
import protocol.json.PacketPojo;

import java.io.IOException;
import java.util.Arrays;

public class RequestChallengeProcessor extends SetupBattleProcessor {

    RequestChallengeProcessor() {
        this.expectedParameters = 2;
    }

    @Override
    public boolean validate(String input) {
        if (super.validate(input)) {
            String[] params = input.split(" ");
            return params[0].equalsIgnoreCase("challenge");
        } else {
            return false;
        }
    }

    @Override
    public void process(String input) throws InputProcessorException, IOException {
        if (this.validate(input)) {
            String[] params = input.split(" ");
            PacketPojo response = TCPHandler.getInstance().handle(new WQPacket(
                    PacketPojo.buildChallengeRequest(params[1])
            ));
            this.setupBattle(response);
        } else {
            super.process(input);
        }
    }
}
