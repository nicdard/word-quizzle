package cli.processors;

/**
 * Used to signal an errored command.
 */
public class InputProcessorException extends Exception {

    InputProcessorException(String input) {
        super(input);
    }

    /**
     * Suppress stackTrace for a lightweight exception
     * @return this
     */
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
