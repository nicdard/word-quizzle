package cli;

import java.util.concurrent.*;

/**
 * Singletons that manages reading from the std_input in either a blocking and
 * a non-blocking way.
 */
public class CliTemporizedReader {

    private final int timeout = 1;
    private final TimeUnit timeUnit = TimeUnit.SECONDS;

    // No lazy loading.
    private final static CliTemporizedReader instance = new CliTemporizedReader();
    private CliTemporizedReader() { }
    public static CliTemporizedReader getInstance() {
        return instance;
    }


    /**
     * Reads a single line from standard input in either blocking or non blocking way.
     * @param shouldBlock
     * @param timeout
     * @param timeUnit
     * @return
     * @throws InterruptedException
     */
    public String readLine(final boolean shouldBlock,
                           final int timeout,
                           final TimeUnit timeUnit) throws InterruptedException
    {
        ExecutorService ex = Executors.newSingleThreadExecutor();
        String input = null;
        try {
            Future<String> result = ex.submit(
                    new CliReadTask());
            try {
                if (shouldBlock) {
                    input = result.get();
                } else {
                    input = result.get(timeout, timeUnit);
                }
            } catch (ExecutionException e) {
                e.getCause().printStackTrace();
            } catch (TimeoutException e) {
                result.cancel(true);
            }
        } finally {
            ex.shutdownNow();
        }
        return input;
    }

    /**
     * If shouldBlock is false then it waits for a predefined time (1 second)
     * and returns null if any string is submitted by the user.
     * @param shouldBlock
     * @return
     * @throws InterruptedException
     */
    public String readLine(boolean shouldBlock) throws InterruptedException {
        return readLine(shouldBlock, this.timeout, this.timeUnit);
    }
}