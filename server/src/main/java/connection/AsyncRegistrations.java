package connection;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.Selector;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

public class AsyncRegistrations {

    private final Queue<Registration> registrationQueue = new PriorityBlockingQueue<>();

    private Selector selector;

    public AsyncRegistrations(Selector selector) {
        this.selector = selector;
    }

    public void enqueue(Registration registration) {
        this.registrationQueue.offer(registration);
    }

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

    public void callAll() throws ClosedChannelException {
        while (call()) { }
    }
}

