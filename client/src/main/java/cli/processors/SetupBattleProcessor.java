package cli.processors;

import cli.CliManager;
import cli.CliState;
import cli.Prompt;
import connection.TCPHandler;
import protocol.OperationCode;
import protocol.ResponseCode;
import protocol.WQPacket;
import protocol.json.PacketPojo;

import java.io.IOException;

public abstract class SetupBattleProcessor extends BaseInputProcessor {

    protected void setupBattle(PacketPojo response) throws IOException {
        if (this.prettyPrint(response)) {
            // Display information about the challenge.
            System.out.println(response.getMessage());
            TCPHandler.getInstance().send(new WQPacket(
                    // Synchronize the players in the server.
                    new PacketPojo(OperationCode.SETUP_CHALLENGE, ResponseCode.OK)
            ));
            PacketPojo packetPojo = TCPHandler.getInstance().receive();
            if (packetPojo.isErrorResponse()) {
                System.out.println("An unexpected error occurred");
                CliManager.getInstance().setNext(new Prompt(
                        Prompt.EXITING,
                        null,
                        CliState.ERROR
                ));
            } else {
                CliManager.getInstance().setNext(new Prompt(
                        Prompt.getAskWordStringPrompt(packetPojo.getWord()),
                        new AskWordProcessor(packetPojo.getWord()),
                        CliState.ONGOING_BATTLE
                ));
            }
        } else {
            CliManager.getInstance().setNext(new Prompt(
                    Prompt.MAIN_PROMPT,
                    BaseInputProcessor.getMainDispatcher(),
                    CliState.MAIN
            ));
        }
    }
}
