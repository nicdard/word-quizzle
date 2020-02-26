package cli.processors;

import cli.CliManager;
import cli.CliState;
import cli.Prompt;
import connection.TCPHandler;
import protocol.OperationCode;
import protocol.WQPacket;
import protocol.json.PacketPojo;

import java.io.IOException;

public class AskWordProcessor extends BaseInputProcessor {

    String word;

    AskWordProcessor(String word) {
        this.word = word;
    }

    @Override
    public boolean validate(String input) {
        // All inputs from the user are considered translation, the empty string
        // is also valid and it's useful to permit it so the user can skip a word
        // he doesn't know.
        return true;
    }

    @Override
    public void process(String input) throws InputProcessorException, IOException {
        if (this.validate(input)) {
            TCPHandler.getInstance().send(new WQPacket(
                    PacketPojo.buildAskWordResponse(this.word, input)
            ));
            PacketPojo packetPojo = TCPHandler.getInstance().receive();
            if (packetPojo.isWellFormedRequestPacket()
                && OperationCode.ASK_WORD.equals(packetPojo.getOperationCode())
            ) {
                CliManager.getInstance().setNext(new Prompt(
                        Prompt.getAskWordStringPrompt(packetPojo.getWord()),
                        new AskWordProcessor(packetPojo.getWord()),
                        CliState.ONGOING_BATTLE,
                        false
                ));
            } else {
                System.out.println("EHI that's " + packetPojo.getOperationCode().name());
                CliManager.getInstance().setNext(new Prompt(
                        Prompt.EXITING,
                        null,
                        CliState.ERROR
                ));
            }
        } else {
            super.process(input);
        }
    }
}
