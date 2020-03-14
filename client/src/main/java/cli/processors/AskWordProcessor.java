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
    public InputProcessor validate(String input) throws InputProcessorException {
        if (input == null) throw new InputProcessorException("Illegal argument.");
        // All inputs from the user are considered translation, the empty string
        // is also valid and it's useful to permit it so the user can skip a word
        // he doesn't know.
        return this;
    }

    @Override
    public void process(String input) throws IOException {
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
            if (this.validateOrPrettyPrintErrorResponse(packetPojo)) {
                System.out.println(packetPojo.getMessage());
                CliManager.getInstance().setNext(Prompt.MAIN_PROMPT);
            } else {
                System.out.println("Unfortunately an unexpected error occurred " +
                        "during the challenge, maybe your friend left."
                );
                CliManager.getInstance().setNext(Prompt.MAIN_PROMPT);
            }
        }
    }
}
