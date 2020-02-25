package cli.processors;

import cli.CliManager;
import cli.CliState;
import cli.Prompt;
import com.fasterxml.jackson.core.type.TypeReference;
import connection.TCPHandler;
import protocol.OperationCode;
import protocol.WQPacket;
import protocol.json.JSONMapper;
import protocol.json.PacketPojo;

import java.io.IOException;
import java.util.List;

public class ShowFriendsProcessor extends BaseInputProcessor {
    ShowFriendsProcessor() {
        this.expectedParameters = 1;
    }

    @Override
    public boolean validate(String input) {
        if (super.validate(input)) {
            String[] params = input.split(" ");
            return params[0].equalsIgnoreCase("show-friends");
        } else {
            return false;
        }
    }

    @Override
    public void process(String input) throws InputProcessorException, IOException {
        if (this.validate(input)) {
            PacketPojo response = TCPHandler.getInstance().handle(new WQPacket(
                    new PacketPojo(OperationCode.GET_FRIENDS)
            ));
            // Pretty print
            System.out.println("You have " + response.getFriends().size() + " friends already:");
            response.getFriends().stream()
                    .sorted()
                    .forEach(System.out::println);
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
