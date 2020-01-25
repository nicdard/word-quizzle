package cli.processors;

import cli.CliManager;
import cli.CliState;
import cli.Prompt;
import com.fasterxml.jackson.core.type.TypeReference;
import connection.TCPHandler;
import protocol.OperationCode;
import protocol.WQPacket;
import protocol.json.JSONMapper;
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
    public void process(String input) throws InputProcessorException {
        if (this.validate(input)) {
            try {
                WQPacket response = TCPHandler.getInstance().handle(new WQPacket(
                        OperationCode.GET_RANKING,
                        ""
                ));
                // Pretty print
                List<RankingListItem> listItems =
                        JSONMapper.objectMapper.readValue(
                                response.getBody(),
                                new TypeReference<List<RankingListItem>>() { }
                        );
                listItems.stream()
                        .sorted()
                        .forEach((it -> System.out.println(it.name + " " + it.score)));
                // Next interface prompt
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
