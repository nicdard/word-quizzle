package cli.processors;

import protocol.json.PacketPojo;

public abstract class BaseInputProcessor implements InputProcessor {

    protected int expectedParameters = 0;
    protected String commandName;

    private static InputProcessor mainDispatcher;

    /**
     * @param input
     * @return this if the input string contains the expected amount
     * of parameters. Finest checks are performed by the specialized
     * processors.
     */
    @Override
    public InputProcessor validate(String input) throws InputProcessorException {
        if (input == null) throw new InputProcessorException("Illegal argument.");
        String[] rawParameters = input.split(" ");
        if (rawParameters.length == this.expectedParameters
            && (rawParameters[0].equalsIgnoreCase(commandName)
                || commandName == null)
        ) {
            return this;
        } else throw new InputProcessorException("Some parameters are wrong or missing.");
    }

    public static InputProcessor getMainDispatcher() {
        if (mainDispatcher == null) {
            mainDispatcher = new MainDispatcherProcessor();
        }
        return mainDispatcher;
    }

    /**
     * Validates a response and if an error occurred prints an error message.
     * @param response
     * @return
     */
    protected boolean validateOrPrettyPrintErrorResponse(PacketPojo response) {
        if (response.isSuccessfullResponse()) {
            return true;
        } else {
            System.out.println("There was an error. ");
            if (response.isExplainedErrorResponse()) System.out.println(response.getMessage());
            return false;
        }
    }
}
