package cli.processors;

import cli.CliManager;
import cli.CliState;
import cli.Prompt;
import connection.TCPHandler;
import protocol.ResponseCode;
import protocol.WQPacket;
import protocol.json.PacketPojo;

import java.io.IOException;

public class AcceptanceBattleProcessor extends SetupBattleProcessor {

    private static final int TTL = 1000;

    public AcceptanceBattleProcessor() {
        // The number of accepted the challenge request or an empty string to exit.
        this.expectedParameters = 1;
        // Do not set the command name, it is indeed not really a command.
    }

    @Override
    public InputProcessor validate(String input) throws InputProcessorException {
        if (input == null) throw new InputProcessorException("Illegal argument.");
        else if (input.isEmpty()) return this;
        else {
            try {
                Integer.parseInt(input);
                return this;
            } catch (NumberFormatException e) {
                throw new InputProcessorException("It should be a number indicating an user from the list.");
            }
        }
    }

    @Override
    public void process(String input) throws IOException {
        if (input.isEmpty() || input.equalsIgnoreCase("exit")) {
            System.out.println("Exiting waiting room...");
            CliManager.getInstance().setNext(Prompt.MAIN_PROMPT);
            return;
        } else {
            int senderIndex = Integer.parseInt(input);
            String challengeRequester = CliManager.getInstance()
                    .getNotifier()
                    .getSender(senderIndex);
            if (challengeRequester == null) {
                // Wrong input just wait for another one.
                System.out.println("The number you entered does not appear in the challenge list.");
                CliManager.getInstance().setNext(new Prompt(
                        Prompt.READER,
                        new AcceptanceBattleProcessor(),
                        CliState.WAIT_BATTLE,
                        false
                ));
                return;
            }
            PacketPojo response = TCPHandler.getInstance().handle(new WQPacket(
                    PacketPojo.buildForwardChallengeResponse(
                            ResponseCode.OK,
                            challengeRequester,
                            TTL
                    )));
            this.setupBattle(response);
        }
    }
}
