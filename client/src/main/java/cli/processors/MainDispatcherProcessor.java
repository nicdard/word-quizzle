package cli.processors;

import java.io.IOException;
import java.util.*;

/**
 * An helper class used to dispatch a request to the right processor for the main menu.
 */
public class MainDispatcherProcessor extends BaseInputProcessor {

    private String toDispatch;

    private static final Map<String, InputProcessor> COMMANDS = Collections.unmodifiableMap(commandMapGenerator());

    MainDispatcherProcessor() {
        this.expectedParameters = 1;
        this.toDispatch = "";
    }

    @Override
    public InputProcessor validate(String input) throws InputProcessorException {
        String[] rawChunks = input.split(" ");
        this.toDispatch = rawChunks[0];
        if (COMMANDS.containsKey(rawChunks[0].trim())) {
            return this;
        } else {
            throw new InputProcessorException("Unknown command! " + rawChunks[0]);
        }
    }

    @Override
    public void process(String input) throws InputProcessorException, IOException {
        // Calls the right processor if known.
        COMMANDS.get(this.toDispatch).validate(input).process(input);
    }

    private static Map<String, InputProcessor> commandMapGenerator() {
        Map<String, InputProcessor> ret = new HashMap<>();
        ret.put("help", new HelpInputProcessor());
        ret.put("exit", new ExitProcessor());
        ret.put("login", new LoginProcessor());
        ret.put("logout", new LogoutProcessor());
        ret.put("show-friends", new ShowFriendsProcessor());
        ret.put("show-score", new ScoreProcessor());
        ret.put("add-friend", new AddFriendProcessor());
        ret.put("show-ranking-list", new ShowRankingProcessor());
        ret.put("register", new RegistrationProcessor());
        ret.put("challenge", new RequestChallengeProcessor());
        ret.put("wait-challenge", new WaitChallengeProcessor());
        return ret;
    }
}
