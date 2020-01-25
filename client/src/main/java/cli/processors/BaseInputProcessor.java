package cli.processors;

public abstract class BaseInputProcessor implements InputProcessor {

    protected int expectedParameters = 0;

    private InputProcessor next;

    private static InputProcessor mainDispatcher;

    /**
     * @param input
     * @return true if the input string contains the expected amount
     * of parameters. Finest checks are performed by the specialized
     * processors.
     */
    @Override
    public boolean validate(String input) {
        if (input == null || input.isEmpty()) return false;
        String[] rawParameters = input.split(" ");
        return rawParameters.length == this.expectedParameters;
    }

    @Override
    public void process(String input) throws InputProcessorException {
        if (this.next != null) {
            this.next.process(input);
        } else {
            // None an handle this input.
            throw new InputProcessorException(input);
        }
    }

    @Override
    public InputProcessor setNext(InputProcessor next) {
        this.next = next;
        return this;
    }

    public static InputProcessor getMainDispatcher() {
        if (mainDispatcher == null) {
            mainDispatcher = new MainDispatcherProcessor();
        }
        return mainDispatcher;
    }
}
