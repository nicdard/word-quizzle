package cli.processors;

import cli.CliManager;
import cli.CliState;
import cli.Prompt;
import connection.TCPHandler;
import protocol.OperationCode;
import protocol.WQPacket;
import protocol.json.PacketPojo;

import java.io.IOException;

public class ShowFriendsProcessor extends BaseInputProcessor {

    ShowFriendsProcessor() {
        this.commandName = "show-friends";
        this.expectedParameters = 1;
    }

    @Override
    public void process(String input) throws IOException {
        PacketPojo response = TCPHandler.getInstance().handle(new WQPacket(
                new PacketPojo(OperationCode.GET_FRIENDS)
        ));
        // Pretty print
        System.out.println("You have " + response.getFriends().size() + " friends already:");
        response.getFriends().stream()
                .sorted()
                .forEach(System.out::println);
        CliManager.getInstance().setNext(Prompt.MAIN_PROMPT);
    }
}
