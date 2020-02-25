package cli.processors;

import cli.CliManager;
import cli.CliState;
import cli.Prompt;
import connection.TCPHandler;
import protocol.OperationCode;
import protocol.WQPacket;
import protocol.json.PacketPojo;

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
    public void process(String input) throws InputProcessorException, IOException {
        if (this.validate(input)) {
            PacketPojo response = TCPHandler.getInstance().handle(new WQPacket(
                    new PacketPojo(OperationCode.LOGOUT)
            ));
            this.prettyPrint(response);
            if (response.isSuccessfullResponse()) Prompt.setPrompt(null);
            CliManager.getInstance().setNext(new Prompt(
                    Prompt.MAIN_PROMPT,
                    BaseInputProcessor.getMainDispatcher(),
                    CliState.MAIN
            ));
        } else {
            super.process(input);
        }
    }

    @Override
    protected boolean prettyPrint(PacketPojo response) {
        if (super.prettyPrint(response)) System.out.println("You successfully logged-out. Come back soon!");
        return true;
    }
}

