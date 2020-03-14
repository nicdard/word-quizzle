package cli.processors;

import java.io.IOException;

public interface InputProcessor {

    /**
     * @throws InputProcessorException if this packetProcessor cannot be applied
     * or the parameters are not valid.
     * @return InputProcessor to enable dot concatenation.
     */
    InputProcessor validate(String input) throws InputProcessorException;

    /**
     * Process the given input if possible.
     * @param input
     * @throws InputProcessorException
     */
    void process(String input) throws InputProcessorException, IOException;
}
