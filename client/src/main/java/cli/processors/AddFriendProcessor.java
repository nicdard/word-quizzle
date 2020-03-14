package cli.processors;

import cli.CliManager;
import cli.CliState;
import cli.Prompt;
import connection.TCPHandler;
import protocol.WQPacket;
import protocol.json.PacketPojo;

import java.io.IOException;

public class AddFriendProcessor extends BaseInputProcessor {

    AddFriendProcessor() {
        this.commandName = "add-friend";
        this.expectedParameters = 2;
    }

    @Override
    public void process(String input) throws IOException {
        String[] params = input.split(" ");
        PacketPojo response = TCPHandler.getInstance().handle(new WQPacket(
                PacketPojo.buildAddFriendRequest(params[1])
        ));
        this.validateOrPrettyPrintErrorResponse(response);
        CliManager.getInstance().setNext(Prompt.MAIN_PROMPT);
    }

    @Override
    protected boolean validateOrPrettyPrintErrorResponse(PacketPojo response) {
        if (super.validateOrPrettyPrintErrorResponse(response))
            System.out.println("You now have a new friend! :)");
        return true;
    }
}
