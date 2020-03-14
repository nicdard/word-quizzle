package cli.processors;

import cli.CliManager;
import cli.CliState;
import cli.Prompt;
import connection.TCPHandler;
import protocol.OperationCode;
import protocol.WQPacket;
import protocol.json.PacketPojo;

import java.io.IOException;

public class ShowRankingProcessor extends BaseInputProcessor {

    ShowRankingProcessor() {
        this.commandName = "show-ranking-list";
        this.expectedParameters = 1;
    }

    @Override
    public void process(String input) throws IOException {
        PacketPojo response = TCPHandler.getInstance().handle(new WQPacket(
                new PacketPojo(OperationCode.GET_RANKING)
        ));
        // Pretty print
        response.getRankingList().stream()
                .sorted()
                .forEach((it -> System.out.println(it.name + " " + it.score)));
        // Next interface prompt
        CliManager.getInstance().setNext(Prompt.MAIN_PROMPT);
    }
}
