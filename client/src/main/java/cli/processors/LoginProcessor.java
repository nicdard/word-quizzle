package cli.processors;

import cli.CliManager;
import cli.CliState;
import cli.Prompt;

import protocol.Config;
import connection.TCPHandler;
import protocol.WQPacket;
import protocol.json.PacketPojo;

import java.io.IOException;

public class LoginProcessor extends BaseInputProcessor {

    private static Integer UDPPort = Config.UDP_PORT;

    LoginProcessor() {
        commandName = "login";
        expectedParameters = 3;
    }

    @Override
    public void process(String input) throws IOException {
        String[] params = input.split(" ");
        PacketPojo response = TCPHandler.getInstance().handle(new WQPacket(
                PacketPojo.buildLoginRequest(
                        params[1],
                        params[2],
                        LoginProcessor.UDPPort
                )
        ));
        this.validateOrPrettyPrintErrorResponse(response);
        if (response.isSuccessfullResponse()) Prompt.setPrompt(params[1]);
        CliManager.getInstance().setNext(Prompt.MAIN_PROMPT);
    }

    public static void setUDPPort(int newPort) {
        UDPPort = newPort;
    }

    @Override
    protected boolean validateOrPrettyPrintErrorResponse(PacketPojo response) {
        if (super.validateOrPrettyPrintErrorResponse(response)) System.out.println("You are now logged-in!");
        return true;
    }
}
