package cli.processors;

import cli.CliManager;
import cli.CliState;
import cli.Prompt;
import connection.TCPHandler;
import protocol.OperationCode;
import protocol.ResponseCode;
import protocol.WQPacket;

import java.io.IOException;
import java.util.Arrays;

public class RequestChallengeProcessor extends BaseInputProcessor {

    RequestChallengeProcessor() {
        this.expectedParameters = 2;
    }

    @Override
    public boolean validate(String input) {
        if (super.validate(input)) {
            String[] params = input.split(" ");
            return params[0].equalsIgnoreCase("challenge");
        } else {
            return false;
        }
    }

    @Override
    public void process(String input) throws InputProcessorException {
        if (this.validate(input)) {
            try {
                String[] params = input.split(" ");
                WQPacket response = TCPHandler.getInstance().handle(new WQPacket(
                        OperationCode.REQUEST_CHALLENGE,
                        params[1] // friend name
                ));
                String[] parameters = response.getParameters();
                if (parameters.length < 2) {
                    System.out.println("An error occurred: MALFORMED PACKET "
                            + response.getBodyAsString()
                    );
                    CliManager.getInstance().enqueue(new Prompt(
                            Prompt.MAIN_PROMPT,
                            BaseInputProcessor.getMainDispatcher(),
                            CliState.MAIN
                    ));
                } else {
                    if (ResponseCode.ACCEPT.name().equals(parameters[0])) {
                        System.out.println("User "
                                + parameters[1]
                                + " accepted your challenge!"
                        );
                        if (parameters.length > 2) {
                            System.out.println(Arrays.toString(
                                    Arrays.copyOfRange(
                                            parameters,
                                            2,
                                            parameters.length
                                    )
                            ));
                        }
                        // TODO second TCP request for setup and starting.
                    } else if (ResponseCode.DISCARD.name().equals(parameters[0])) {
                        if (parameters.length > 2) {
                            System.out.println(Arrays.toString(
                                    Arrays.copyOfRange(
                                            parameters,
                                            2,
                                            parameters.length
                                    )
                            ));
                        }
                        CliManager.getInstance().enqueue(new Prompt(
                                Prompt.MAIN_PROMPT,
                                BaseInputProcessor.getMainDispatcher(),
                                CliState.MAIN
                        ));
                    } else {
                         throw new IllegalArgumentException("A protocol error occurred");
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            super.process(input);
        }
    }
}
