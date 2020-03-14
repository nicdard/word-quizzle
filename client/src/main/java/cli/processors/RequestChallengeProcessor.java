package cli.processors;

import connection.TCPHandler;
import protocol.WQPacket;
import protocol.json.PacketPojo;

import java.io.IOException;

public class RequestChallengeProcessor extends SetupBattleProcessor {

    RequestChallengeProcessor() {
        this.commandName = "challenge";
        this.expectedParameters = 2;
    }

    @Override
    public void process(String input) throws IOException {
        String[] params = input.split(" ");
        System.out.println("Waiting " + params[1] + " response...");
        PacketPojo response = TCPHandler.getInstance().handle(new WQPacket(
                PacketPojo.buildChallengeRequest(params[1])
        ));
        this.setupBattle(response);
    }
}
