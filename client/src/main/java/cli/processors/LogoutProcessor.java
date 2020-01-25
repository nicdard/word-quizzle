package cli.processors;

import cli.CliManager;
import cli.CliState;
import cli.Prompt;
import connection.TCPHandler;
import protocol.OperationCode;
import protocol.WQPacket;

import java.io.IOException;

public class LogoutProcessor extends BaseInputProcessor {

    LogoutProcessor() {
        this.expectedParameters = 1;
    }

    @Override
    public boolean validate(String input) {
        if (super.validate(input)) {
            return input.equalsIgnoreCase("logout");
        } else {
            return false;
        }
    }

    @Override
    public void process(String input) throws InputProcessorException {
        if (this.validate(input)) {
            try {
                WQPacket response = TCPHandler.getInstance().handle(new WQPacket(
                        OperationCode.LOGOUT,
                        ""
                ));
                System.out.println(response.getOpCode() + " " + response.getBodyAsString());
                CliManager.getInstance().enqueue(new Prompt(
                        Prompt.MAIN_PROMPT,
                        BaseInputProcessor.getMainDispatcher(),
                        CliState.MAIN
                ));
            } catch (IOException e) {
                e.printStackTrace();
                super.process(input);
            }
        } else {
            super.process(input);
        }
    }
}

