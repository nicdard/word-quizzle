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
        this.commandName = "logout";
        this.expectedParameters = 1;
    }

    @Override
    public void process(String input) throws IOException {
        PacketPojo response = TCPHandler.getInstance().handle(new WQPacket(
                new PacketPojo(OperationCode.LOGOUT)
        ));
        this.validateOrPrettyPrintErrorResponse(response);
        if (response.isSuccessfullResponse()) Prompt.setPrompt(null);
        CliManager.getInstance().setNext(Prompt.MAIN_PROMPT);
    }

    @Override
    protected boolean validateOrPrettyPrintErrorResponse(PacketPojo response) {
        if (super.validateOrPrettyPrintErrorResponse(response)) System.out.println("You successfully logged-out. Come back soon!");
        return true;
    }
}

