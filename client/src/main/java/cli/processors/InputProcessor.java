package cli.processors;

import java.io.IOException;

public interface InputProcessor {

    /**
     * @return true if this packetProcessor can be applied
     * and the parameters are valid.
     */
    boolean validate(String input);

    /**
     * Process the given input if possible.
     * @param input
     * @throws InputProcessorException
     */
    void process(String input) throws InputProcessorException, IOException;

    /**
     * Sets the next processor to be applied on the input.
     * @param next
     */
    InputProcessor setNext(InputProcessor next);
}
