package connection;

import cli.processors.AcceptanceBattleProcessor;
import cli.CliManager;
import cli.CliState;
import cli.Prompt;
import protocol.Config;
import protocol.OperationCode;
import protocol.WQPacket;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class UDPReader implements Runnable {

    private volatile boolean exit;
    private DatagramSocket datagramSocket;

    private static final int TIMEOUT = 1000;

    public UDPReader() throws SocketException, UnknownHostException {
        exit = false;
        datagramSocket = new DatagramSocket();
        datagramSocket.connect(InetAddress.getByName(Config.SERVER_NAME), Config.UDP_PORT);
        // Exit from blocking read every second to checks if the client is closing.
        datagramSocket.setSoTimeout(TIMEOUT);
    }

    @Override
    public void run() {
        while (!exit) {
            try {
                // UDP packet to receive
                DatagramPacket response = new DatagramPacket(new byte[512], 512, InetAddress.getByName(Config.SERVER_NAME), Config.UDP_PORT);
                // Try receive response from the ping
                datagramSocket.receive(response);
                WQPacket wqPacket = WQPacket.fromBytes(ByteBuffer.wrap(
                        Arrays.copyOfRange(
                            response.getData(),
                            0,
                            response.getLength()
                        )
                ));
                if (wqPacket.getOpCode() == OperationCode.FORWARD_CHALLENGE) {
                    // The nickName of the original challenge requester.
                    String nickname = wqPacket.getBodyAsString();
                    if (nickname != null && !nickname.isEmpty()) {
                        // Create an acceptance alert.
                        Prompt prompt = new Prompt(
                                Prompt.ACCEPTANCE_ALERT.replace(Prompt.NAME_MACRO, nickname),
                                new AcceptanceBattleProcessor(nickname),
                                CliState.ACCEPT_BATTLE_ALERT
                        );
                        // Put it in the queue.
                        CliManager.getInstance().enqueue(prompt);
                    }
                }
            } catch (IOException e) {
                // Do nothing, just check for exit condition and stops on receive again.
            }

        }
    }

    /**
     * Stops the thread and closes the socket.
     */
    public void stop() {
        this.exit = true;
    }

    /**
     * @return the udp port.
     */
    public int getPort() {
        if (datagramSocket.getLocalPort() > 0) {
            return datagramSocket.getLocalPort();
        } else throw new IllegalStateException("UDP socket not bound or closed!");
    }
}
