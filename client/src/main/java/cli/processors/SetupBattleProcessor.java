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
        if (this.validateOrPrettyPrintErrorResponse(response)) {
            // Display information about the challenge.
            System.out.println(response.getMessage());
            TCPHandler.getInstance().send(new WQPacket(
                    // Synchronize the players in the server.
                    new PacketPojo(OperationCode.SETUP_CHALLENGE, ResponseCode.OK)
            ));
            System.out.println("...Entering battle!");
            PacketPojo packetPojo = TCPHandler.getInstance().receive();
            if (packetPojo.isErrorResponse()) {
                System.out.println("Unfortunately an unexpected error occurred during battle setup.");
                CliManager.getInstance().setNext(Prompt.MAIN_PROMPT);
            } else {
                System.out.println("You can press enter to skip the word if you don't know the translation.");
                CliManager.getInstance().setNext(new Prompt(
                        Prompt.getAskWordStringPrompt(packetPojo.getWord()),
                        new AskWordProcessor(packetPojo.getWord()),
                        CliState.ONGOING_BATTLE,
                        false
                ));
            }
        } else {
            CliManager.getInstance().setNext(Prompt.MAIN_PROMPT);
        }
    }
}
