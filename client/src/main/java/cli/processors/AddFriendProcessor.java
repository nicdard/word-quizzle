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
    public void process(String input) throws InputProcessorException, IOException {
        if (this.validate(input)) {
            String[] params = input.split(" ");
            PacketPojo response = TCPHandler.getInstance().handle(new WQPacket(
                    PacketPojo.buildAddFriendRequest(params[1])
            ));
            this.prettyPrint(response);
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
        if (super.prettyPrint(response))
            System.out.println("You now have a new friend! :)");
        return true;
    }
}
