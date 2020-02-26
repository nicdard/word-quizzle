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
     * @param key
     * @param operation
     */
    public void register(final SelectionKey key, final int operation) {
        this.registrationQueue.offer(new Registration(key, operation));
        selector.wakeup();
    }

    /**
     * Pushes a registration operation in the asyncRegistrations and unblock selector from select.
     * This registration will crate a new selection key for the channel with the state attached.
     * @param channel
     * @param state
     * @param operation
     */
    public void register(final SocketChannel channel, final State state, final int operation) {
        this.registrationQueue.offer(new Registration(channel, state, operation));
        selector.wakeup();
    }

    /**
     * Executes and removes the first command in the queue.
     * @return
     */
    public boolean call() throws ClosedChannelException {
        Registration command = this.registrationQueue.poll();
        if (command != null) {
            if (command.shouldRegisterTheChannel()) {
                command.getSocketChannel().register(
                        this.selector,
                        command.getInterestOp(),
                        command.getState()
                );
            } else {
                command.getKey().interestOps(command.getInterestOp());
            }
            return true;
        } else return false;
    }

    /**
     * Executes all commands in the queue.
     */
    public void callAll() throws ClosedChannelException {
        while (call()) { }
    }

    /**
     * Captures a registrationAction that will performed by the invoker of selector.select().
     */
    private static class Registration implements Comparable<Registration> {

        // The socketChannel to be registered to create.
        private SocketChannel socketChannel;
        // The state
        private State state;

        // The key
        private SelectionKey key;
        // The requested operation
        private int interestOp;

        /**
         * Builds a new Registration command and checks if the interestOp is valid
         * (i.e. one of the SelectionKey exported constant).
         * @param key
         * @param interestOp
         */
        Registration(SelectionKey key, int interestOp) {
            if (interestOp != SelectionKey.OP_READ
                    && interestOp != SelectionKey.OP_WRITE
                    && interestOp != SelectionKey.OP_CONNECT
                    && interestOp != SelectionKey.OP_ACCEPT
            ) {
                throw new IllegalArgumentException("Should be a SelectionKey valid value");
            }
            this.key = key;
            this.interestOp = interestOp;
        }

        /**
         * Builds a new Registration command and checks if the interestOp is valid
         * (i.e. one of the SelectionKey exported constant).
         * @param client
         * @param interestOp
         */
        Registration(SocketChannel client, State state,  int interestOp) {
            if (interestOp != SelectionKey.OP_READ
                    && interestOp != SelectionKey.OP_WRITE
                    && interestOp != SelectionKey.OP_CONNECT
                    && interestOp != SelectionKey.OP_ACCEPT
            ) {
                throw new IllegalArgumentException("Should be a SelectionKey valid value");
            }
            this.interestOp = interestOp;
            this.state = state;
            this.socketChannel = client;
        }

        private boolean shouldRegisterTheChannel() {
            return this.socketChannel != null;
        }

        SocketChannel getSocketChannel() {
            return socketChannel;
        }

        /**
         * @return the key to register.
         */
        SelectionKey getKey() {
            return key;
        }

        /**
         * @return the requested interestOp.
         */
        int getInterestOp() {
            return interestOp;
        }

        /**
         * Used to prioritize the operation in the queue.
         * @param registration
         * @return
         */
        @Override
        public int compareTo(Registration registration) {
            return this.interestOp - registration.interestOp;
        }

        public State getState() {
            return state;
        }
    }

}

