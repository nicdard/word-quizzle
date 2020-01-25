package cli.processors;

import RMIRegistrationService.RegistrationRemoteService;
import RMIRegistrationService.RegistrationResponseStatusCode;
import cli.CliManager;
import cli.CliState;
import cli.Prompt;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RegistrationProcessor extends BaseInputProcessor {

    static RegistrationRemoteService service;

    private String nickName;
    private String password;

    RegistrationProcessor() {
        this.expectedParameters = 3; // One is the command name
    }

    /**
     * @param input
     * @return true if the provided password has at least 4 characters.
     * @modifies sets nickname and password
     */
    @Override
    public boolean validate(String input) {
        if (super.validate(input)) {
            String[] rawValues = input.split(" ");
            String command = rawValues[0];
            this.nickName = rawValues[1];
            this.password = rawValues[2];
            return command.equalsIgnoreCase("register")
                    && password.length() >= 4;
        }
        return false;
    }

    @Override
    public void process(String input) throws InputProcessorException {
        if (this.validate(input)) {
            try {
                RegistrationResponseStatusCode responseCode =
                    RegistrationProcessor.getRegistrationService()
                            .addUser(nickName, password);
                switch (responseCode) {
                    case OK:
                        System.out.println("You successfully complete the registration to WQ!");
                        break;
                    case INVALID_NICK_ERROR:
                        System.out.println("The provided nick is invalid");
                        break;
                    case INVALID_PASSWORD_ERROR:
                        System.out.println("The password must be at least 4 character long");
                        break;
                    case NICK_ALREADY_REGISTERED_ERROR:
                        System.out.println("The nickname is already registered to WQ :(");
                        break;
                    case INTERNAL_ERROR:
                    default:
                        System.out.println("The server has experienced an internal error");
                        break;
                }
                CliManager.getInstance().enqueue(new Prompt(
                        Prompt.MAIN_PROMPT,
                        BaseInputProcessor.getMainDispatcher(),
                        CliState.MAIN
                ));
            } catch (RemoteException | NotBoundException e) {
                e.printStackTrace();
                throw new InputProcessorException("Registration service is unreachable!");
            }
        } else {
            super.process(input);
        }
    }


    private static RegistrationRemoteService getRegistrationService()
            throws RemoteException, NotBoundException
    {
        if (service == null) {
            Registry registry = LocateRegistry.getRegistry(
                    RegistrationRemoteService.REGISTRY_HOST,
                    RegistrationRemoteService.REGISTRY_PORT);

            service = (RegistrationRemoteService)
                    registry.lookup(RegistrationRemoteService.REMOTE_OBJECT_NAME);
        }
        return service;
    }
}
