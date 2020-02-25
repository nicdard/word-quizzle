package cli.processors;

import cli.CliManager;
import cli.CliState;
import cli.Prompt;

import protocol.Config;
import connection.TCPHandler;
import protocol.OperationCode;
import protocol.WQPacket;
import protocol.json.PacketPojo;

import java.io.IOException;

public class LoginProcessor extends BaseInputProcessor {

    private static Integer UDPPort = Config.UDP_PORT;

    LoginProcessor() {
        expectedParameters = 3;
    }

    @Override
    public boolean validate(String input) {
        if (super.validate(input)) {
            String[] params = input.split(" ");
            return params[0].equalsIgnoreCase("login");
        } else {
            return false;
        }
    }

    @Override
    public void process(String input) throws InputProcessorException, IOException {
        if (this.validate(input)) {
            String[] params = input.split(" ");
            PacketPojo response = TCPHandler.getInstance().handle(new WQPacket(
                    PacketPojo.buildLoginRequest(
                            params[1],
                            params[2],
                            LoginProcessor.UDPPort
                    )
            ));
            this.prettyPrint(response);
            if (response.isSuccessfullResponse()) Prompt.setPrompt(params[1]);
            CliManager.getInstance().setNext(new Prompt(
                    Prompt.MAIN_PROMPT,
                    BaseInputProcessor.getMainDispatcher(),
                    CliState.MAIN
            ));
        } else {
            super.process(input);
        }
    }

    public static void setUDPPort(int newPort) {
        UDPPort = newPort;
    }

    @Override
    protected boolean prettyPrint(PacketPojo response) {
        if (super.prettyPrint(response)) System.out.println("You are now logged-in!");
        return true;
    }
}
