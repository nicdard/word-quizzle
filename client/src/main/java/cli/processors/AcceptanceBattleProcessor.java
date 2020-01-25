package cli.processors;

import cli.CliManager;
import cli.CliState;
import cli.Prompt;
import connection.TCPHandler;
import protocol.OperationCode;
import protocol.ResponseCode;
import protocol.WQPacket;

import java.io.IOException;

public class AcceptanceBattleProcessor extends BaseInputProcessor {

    private String sender;

    public AcceptanceBattleProcessor(String sender) {
        this.sender = sender;
        // Yes or No.
        this.expectedParameters = 1;
    }

    @Override
    public void process(String input) throws InputProcessorException {
        if (this.validate(input)) {
            if (input.equalsIgnoreCase("Y") || input.equalsIgnoreCase("yes")) {
                // Notify user the application is processing.
                System.out.println("Trying to setup the challenge...");
                // Send acceptance packet.
                try {
                    WQPacket response = TCPHandler.getInstance().handle(
                        new WQPacket(OperationCode.FORWARD_CHALLENGE,
                                ResponseCode.ACCEPT.name() + " " + sender)
                    );

                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                // Send discard packet.
                try {
                    // Ignore response.
                    TCPHandler.getInstance().handle(new WQPacket(
                            OperationCode.FORWARD_CHALLENGE,
                            ResponseCode.DISCARD.name() + " " + sender
                    ));
                    // Push main prompt on the queue.
                    CliManager.getInstance().enqueue(new Prompt(
                            Prompt.MAIN_PROMPT,
                            BaseInputProcessor.getMainDispatcher(),
                            CliState.MAIN
                    ));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            super.process(input);
        }
    }
}
