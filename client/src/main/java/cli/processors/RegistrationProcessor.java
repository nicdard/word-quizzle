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

    RegistrationProcessor() {
        this.commandName = "register";
        this.expectedParameters = 3; // One is the command name
    }

    @Override
    public void process(String input) throws InputProcessorException {
        String[] rawValues = input.split(" ");
        try {
            RegistrationResponseStatusCode responseCode =
                RegistrationProcessor.getRegistrationService()
                        .addUser(rawValues[1], rawValues[2]);
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
            CliManager.getInstance().setNext(Prompt.MAIN_PROMPT);
        } catch (RemoteException | NotBoundException e) {
            throw new InputProcessorException("Registration service is unreachable!");
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
