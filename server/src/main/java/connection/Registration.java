package connection;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Captures a registrationAction to be performed by the invoker of selector.select().
 */
public class Registration implements Comparable<Registration> {

    private SocketChannel client;
    private int selectionKey;
    private ClientState connectionState;

    public Registration(SocketChannel client, int selectionKey, ClientState connectionState) {
        if (selectionKey != SelectionKey.OP_READ
            && selectionKey != SelectionKey.OP_WRITE
            && selectionKey != SelectionKey.OP_CONNECT
            && selectionKey != SelectionKey.OP_ACCEPT
        ) {
            throw new IllegalArgumentException("Should be a SelectionKey valid value");
        }
        this.client = client;
        this.selectionKey = selectionKey;
        this.connectionState = connectionState;
    }

    public SocketChannel getClient() {
        return client;
    }

    public int getSelectionKey() {
        return selectionKey;
    }

    public ClientState getConnectionState() {
        return connectionState;
    }

    @Override
    public int compareTo(Registration registration) {
        return this.selectionKey - registration.selectionKey;
    }
}
