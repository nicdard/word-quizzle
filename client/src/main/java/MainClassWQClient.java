import cli.CliManager;
import cli.processors.LoginProcessor;
import connection.TCPHandler;
import connection.UDPReader;

import java.io.IOException;


public class MainClassWQClient {

    private UDPReader udpReader;
    private Thread udpReaderExecutor;

    MainClassWQClient() throws IOException {
        // Creates the instance and connect the socket.
        TCPHandler.getInstance();
        udpReader = UDPReader.getInstance();
        // UDP listener for battle requests.
        udpReaderExecutor = new Thread(udpReader);
        LoginProcessor.setUDPPort(udpReader.getPort());
        udpReaderExecutor.start();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Welcome to World Quizzle!");
        MainClassWQClient mainClassWQClient = new MainClassWQClient();
        mainClassWQClient.run();
    }

    private void run() throws IOException, InterruptedException {
        // Starts FSM prompt lifecycle.
        CliManager.getInstance().start();
        // Free resources.
        TCPHandler.getInstance().close();
        udpReader.stop();
        udpReaderExecutor.join();
        CliManager.getInstance().cleanUp();
    }
}

