package connection;

import protocol.Config;
import protocol.OperationCode;
import protocol.WQPacket;
import protocol.json.PacketPojo;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class UDPReader implements Runnable {

    private volatile boolean exit;
    private DatagramSocket datagramSocket;
    /**
     * Stores the packets to be retrieved in the main thread
     * when the user access the waiting room.
     */
    private BlockingQueue<PacketPojo> packets;

    private static final int TIMEOUT = 1000;

    private static UDPReader instance;

    private UDPReader() throws SocketException, UnknownHostException {
        exit = false;
        datagramSocket = new DatagramSocket();
        datagramSocket.connect(InetAddress.getByName(Config.SERVER_NAME), Config.UDP_PORT);
        // Exit from blocking read every second to checks if the client is closing.
        datagramSocket.setSoTimeout(TIMEOUT);
        this.packets = new ArrayBlockingQueue<>(20);
    }

    public static UDPReader getInstance() throws IllegalStateException {
        if (instance == null) {
            try {
                instance = new UDPReader();
            } catch (SocketException | UnknownHostException e) {
                e.printStackTrace();
                throw new IllegalStateException("Error in UDPReader initialisation.");
            }
        }
        return instance;
    }

    @Override
    public void run() {
        while (!exit) {
            try {
                // UDP packet to receive
                DatagramPacket response = new DatagramPacket(new byte[512], 512, InetAddress.getByName(Config.SERVER_NAME), Config.UDP_PORT);
                // Try receive response from the ping
                datagramSocket.receive(response);
                PacketPojo wqPacket = WQPacket.fromBytes(ByteBuffer.wrap(
                        Arrays.copyOfRange(
                            response.getData(),
                            0,
                            response.getLength()
                        )
                ));
                if (OperationCode.FORWARD_CHALLENGE.equals(wqPacket.getOperationCode())) {
                    this.publishPacket(wqPacket);
                }
            } catch (IOException e) {
                // Do nothing, just check for exit condition and stops or receive again.
            }
        }
        this.datagramSocket.close();
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

    /**
     * Retrieves a packet, non blocking, waits up to one second.
     * @return
     * @throws InterruptedException
     */
    public PacketPojo consumePacket() throws InterruptedException {
        return packets.poll(1, TimeUnit.SECONDS);
    }


    public PacketPojo consumePacket(int ms) throws InterruptedException {
        return packets.poll(ms, TimeUnit.MILLISECONDS);
    }

    public void publishPacket(PacketPojo packetPojo) {
        if (!this.packets.offer(packetPojo)) {
            // The queue is full, discard the eldest packet
            try {
                // Discard the packet.
                this.consumePacket(0);
            } catch (InterruptedException e) {
                // Ignore, if not immediately available then we can retry
                // to add an element because the queue is empty.
            }
            // Retry the insertion.
            this.publishPacket(packetPojo);
        }
    }
}
