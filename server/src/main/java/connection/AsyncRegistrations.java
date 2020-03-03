package connection;

import java.nio.channels.SelectionKey;
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
     * Pushes a registration operation in the asyncRegistrations and unblock selector from select.
     * @param key
     * @param operation
     */
    public void register(final SelectionKey key, final int operation) {
        this.registrationQueue.offer(new Registration(key, operation));
        key.selector().wakeup();
    }

    /**
     * Executes and removes the first command in the queue.
     * @return
     */
    private boolean call() {
        Registration command = this.registrationQueue.poll();
        if (command != null) {
            command.getKey().interestOps(command.getInterestOp());
            return true;
        } else return false;
    }

    /**
     * Executes all commands in the queue.
     */
    public void callAll() {
        while (call()) { }
    }

    /**
     * Captures a registrationAction that will performed by the invoker of selector.select().
     */
    private static class Registration implements Comparable<Registration> {

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
    }

}

