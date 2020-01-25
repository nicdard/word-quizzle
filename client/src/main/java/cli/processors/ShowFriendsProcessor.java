package cli.processors;

import cli.CliManager;
import cli.CliState;
import cli.Prompt;
import com.fasterxml.jackson.core.type.TypeReference;
import connection.TCPHandler;
import protocol.OperationCode;
import protocol.WQPacket;
import protocol.json.JSONMapper;

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
    public void process(String input) throws InputProcessorException {
        if (this.validate(input)) {
            try {
                WQPacket response = TCPHandler.getInstance().handle(new WQPacket(
                        OperationCode.GET_FRIENDS,
                        ""
                ));
                // Pretty print
                List<String> listItems =
                        JSONMapper.objectMapper.readValue(
                                response.getBody(),
                                new TypeReference<List<String>>() { }
                        );
                System.out.println("You have " + listItems.size() + " friends already:");
                listItems.stream()
                        .sorted()
                        .forEach(System.out::println);
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
