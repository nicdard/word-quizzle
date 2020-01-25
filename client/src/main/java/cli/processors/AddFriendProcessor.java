package cli.processors;

import cli.CliManager;
import cli.CliState;
import cli.Prompt;
import connection.TCPHandler;
import protocol.OperationCode;
import protocol.WQPacket;

import java.io.IOException;

public class AddFriendProcessor extends BaseInputProcessor {

    AddFriendProcessor() {
        this.expectedParameters = 2;
    }

    @Override
    public boolean validate(String input) {
        if (super.validate(input)) {
            String[] params = input.split(" ");
            return params[0].equalsIgnoreCase("add-friend");
        } else {
            return false;
        }
    }

    @Override
    public void process(String input) throws InputProcessorException {
        if (this.validate(input)) {
            try {
                String[] params = input.split(" ");
                WQPacket response = TCPHandler.getInstance().handle(new WQPacket(
                        OperationCode.ADD_FRIEND,
                        params[1]
                ));
                System.out.println(response.getOpCode() + " " + response.getBodyAsString());
                CliManager.getInstance().enqueue(new Prompt(
                        Prompt.MAIN_PROMPT,
                        BaseInputProcessor.getMainDispatcher(),
                        CliState.MAIN
                ));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            super.process(input);
        }
    }
}
