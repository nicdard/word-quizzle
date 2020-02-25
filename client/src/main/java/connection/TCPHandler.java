package connection;

import protocol.Config;
import protocol.WQPacket;
import protocol.json.PacketPojo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

public class TCPHandler {

    private Socket socket;
    private OutputStream out;
    private InputStream in;

    private static TCPHandler instance;

    private TCPHandler() throws IOException {
        socket = new Socket(InetAddress.getByName(Config.SERVER_NAME), Config.TCP_PORT);
        in = socket.getInputStream();
        out = socket.getOutputStream();
    }

    public static TCPHandler getInstance() throws IOException {
        if (instance == null) {
            instance = new TCPHandler();
        }
        return instance;
    }

    /**
     * Sends a packet to the server and waits for a response.
     * @param wqPacket
     * @return the response packet.
     * @throws IOException
     */
    public PacketPojo handle(WQPacket wqPacket) throws IOException {
        // send the request.
        this.send(wqPacket);
        return this.receive();
    }

    /**
     * Reads a packet from the server and unwraps it.
     * @return the received packet.
     * @throws IOException
     */
    public PacketPojo receive() throws IOException {
        // Read header.
        byte[] header = new byte[WQPacket.getHeaderByteNumber()];
        int still = WQPacket.getHeaderByteNumber();
        this.readn(header, 0, still);
        int length = WQPacket.getPacketLengthFromHeaderBytes(header);
        if (length == -1) {
            throw new IOException("Malformed header");
        }
        byte[] body = new byte[length - WQPacket.getHeaderByteNumber()];
        // Read body.
        readn(body, 0, length - WQPacket.getHeaderByteNumber());
        return WQPacket.fromBytes(
                ByteBuffer.wrap(header),
                ByteBuffer.wrap(body)
        );
    }

    /**
     * Sends a packet to the server.
     * @param wqPacket
     * @throws IOException
     */
    public void send(WQPacket wqPacket) throws IOException {
        out.write(wqPacket.toBytes());
        out.flush();
    }

    /**
     * Closes the socket and free resources.
     * @throws IOException
     */
    public void close() throws IOException {
        in.close();
        out.close();
        socket.close();
    }

    /**
     * Read length bytes from the socket writing them into buff starting from
     * the given pos. May produce ArrayOutOfBoundException.
     * @modifies buff
     * @param buff
     * @param pos
     * @param length
     * @throws IOException
     */
    private void readn(byte[] buff, int pos, int length) throws IOException {
        while (length != 0) {
            int read = in.read(buff, pos, length);
            if (read == -1) {
                // Server socket is closed.
                System.out.println("Server unreachable!");
                throw new IOException("Server is unreachable!");
            } else {
                length -= read;
                pos += read;
            }
        }
    }

}
