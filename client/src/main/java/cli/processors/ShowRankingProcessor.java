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
import protocol.json.RankingListItem;

import java.io.IOException;
import java.util.List;

public class ShowRankingProcessor extends BaseInputProcessor {

    ShowRankingProcessor() {
        this.expectedParameters = 1;
    }

    @Override
    public boolean validate(String input) {
        if (super.validate(input)) {
            String[] params = input.split(" ");
            return params[0].equalsIgnoreCase("show-ranking-list");
        } else {
            return false;
        }
    }

    @Override
    public void process(String input) throws InputProcessorException, IOException {
        if (this.validate(input)) {
            PacketPojo response = TCPHandler.getInstance().handle(new WQPacket(
                    new PacketPojo(OperationCode.GET_RANKING)
            ));
            // Pretty print
            response.getRankingList().stream()
                    .sorted()
                    .forEach((it -> System.out.println(it.name + " " + it.score)));
            // Next interface prompt
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
