package cli.processors;

import protocol.json.PacketPojo;

import java.io.IOException;

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
    public void process(String input) throws InputProcessorException, IOException {
        if (this.next != null) {
            this.next.process(input);
        } else {
            // None can handle this input.
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

    /**
     * Validates a response and if an error occurred it prints an error message.
     * @param response
     * @return
     */
    protected boolean prettyPrint(PacketPojo response) {
        if (response.isSuccessfullResponse()) {
            return true;
        } else {
            System.out.println("There was an error. ");
            if (response.isExplainedErrorResponse()) System.out.println(response.getMessage());
            return false;
        }
    }
}
