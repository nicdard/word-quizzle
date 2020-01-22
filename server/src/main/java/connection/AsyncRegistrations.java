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
    /**
     * Stores a default channel for registration command.
     */
    private SocketChannel channel;
    /**
     * Holds the connectionState associated to the default channel.
     */
    private State connectionState;

    public AsyncRegistrations(Selector selector) {
        this.selector = selector;
    }

    /**
     * Builds an asyncRegistrations instance with a default channel on which
     * call register.
     * @param selector
     * @param channel
     */
    public AsyncRegistrations(Selector selector, SocketChannel channel, State state) {
        this(selector);
        this.channel = channel;
        this.connectionState = state;
    }

    /**
     * Pushes a registration operation in the asyncRegistrations and unblock selector from select.
     * @param client
     * @param operation
     * @param state
     */
    public void register(SocketChannel client, int operation, final State state) {
        this.registrationQueue.offer(new Registration(
                client,
                operation,
                state
        ));
        selector.wakeup();
    }

    /**
     * Like the homologous method but uses the default channel stored
     * at construction time.
     * @param operation
     * @param state
     * @throws IllegalStateException if any default channel is provided.
     */
    public void register(int operation, final State state) throws IllegalStateException {
        if (channel == null) throw new IllegalStateException("Default channel missing!");
        this.registrationQueue.offer(new Registration(
                channel,
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
     * @return the state associated to the default channel.
     * @throws IllegalStateException if default channel is not provided.
     */
    public State getConnectionState() throws IllegalStateException {
        if (channel == null) throw new IllegalStateException("Default channel is not active!");
        return connectionState;
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
        private State connectionState;

        /**
         * Builds a new Registration command and checks if the selectionKey is valid
         * (i.e. one of the SelectionKey exported constant).
         * @param client
         * @param selectionKey
         * @param connectionState
         */
        Registration(SocketChannel client, int selectionKey, State connectionState) {
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
        State getConnectionState() {
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

