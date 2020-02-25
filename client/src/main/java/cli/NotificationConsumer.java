package cli;

import connection.UDPReader;
import protocol.OperationCode;
import protocol.json.PacketPojo;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class NotificationConsumer implements Runnable {

    private volatile boolean exit = false;
    private int counter = 0;
    /** Maps index -> sender */
    private ConcurrentMap<Integer, String> senders = new ConcurrentHashMap<>(5);

    @Override
    public void run() {
        while (!exit) {
            try {
                PacketPojo packetPojo = UDPReader.getInstance().consumePacket();
                if (OperationCode.FORWARD_CHALLENGE.equals(packetPojo.getOperationCode())) {
                    // Checks TTL.
                    if (System.currentTimeMillis() - packetPojo.getTimestamp() < packetPojo.getTtl()) {
                        if (!exit) {
                            this.senders.put(++this.counter, packetPojo.getFriend());
                            System.out.printf("- %d %s\n", this.counter, packetPojo.getFriend());
                        } else {
                            // In case the thread was already stopped republish the packet
                            UDPReader.getInstance().publishPacket(packetPojo);
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Stops the thread.
     */
    public void stop() {
        this.exit = true;
    }

    /**
     * @return the sender of a challenge request by its printed index.
     * if none returns null.
     */
    public String getSender(int index) {
        return this.senders.getOrDefault(index, null);
    }
}
