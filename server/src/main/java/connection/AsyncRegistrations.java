package connection;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

public class AsyncRegistrations {

    /**
     * Registration queue.
     * It is needed to store a registration command and call it after
     * in the same thread were selector.select() is called.
     */
    private final Queue<Registration> registrationQueue = new PriorityBlockingQueue<>();

    /**
     * The selector to which the commands in the queue refer.
     */
    private Selector selector;

    public AsyncRegistrations(Selector selector) {
        this.selector = selector;
    }

    /**
     * Pushes a registration operation in the asyncRegistrations and unblock selector from select.
     * @param client
     * @param operation
     * @param state
     */
    public void register(SocketChannel client, int operation, final ClientState state) {
        this.registrationQueue.offer(new Registration(
                client,
                operation,
                state
        ));
        selector.wakeup();
    }

    /**
     * Executes and removes the first command in the queue.
     * @return
     * @throws ClosedChannelException
     */
    public boolean call() throws ClosedChannelException {
        Registration command = this.registrationQueue.poll();
        if (command != null) {
            command.getClient().register(
                    selector,
                    command.getSelectionKey(),
                    command.getConnectionState()
            );
            return true;
        } else return false;
    }

    /**
     * Executes all commands in the queue.
     * @throws ClosedChannelException
     */
    public void callAll() throws ClosedChannelException {
        while (call()) { }
    }


    /**
     * Captures a registrationAction that will performed by the invoker of selector.select().
     */
    private static class Registration implements Comparable<Registration> {

        // The client socket
        private SocketChannel client;
        // The requested operation
        private int selectionKey;
        // The new connection state for this client to be attached on register invocation.
        private ClientState connectionState;

        /**
         * Builds a new Registration command and checks if the selectionKey is valid
         * (i.e. one of the SelectionKey exported constant).
         * @param client
         * @param selectionKey
         * @param connectionState
         */
        Registration(SocketChannel client, int selectionKey, ClientState connectionState) {
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

        /**
         * @return the client socketChannel to register.
         */
        SocketChannel getClient() {
            return client;
        }

        /**
         * @return the requested selectionKey.
         */
        int getSelectionKey() {
            return selectionKey;
        }

        /**
         * @return the attachment.
         */
        ClientState getConnectionState() {
            return connectionState;
        }

        /**
         * Used to prioritize the operation in the queue.
         * @param registration
         * @return
         */
        @Override
        public int compareTo(Registration registration) {
            return this.selectionKey - registration.selectionKey;
        }
    }

}

