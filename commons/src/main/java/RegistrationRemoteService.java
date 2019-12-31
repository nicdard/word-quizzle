import java.rmi.Remote;

/**
 * Interface of the Registration Service
 */
public interface RegistrationRemoteService extends Remote {

    String REMOTE_OBJECT_NAME = "RegistrationRemoteService";

    RegistrationResponseStatusCode addUser(String name, String password);

    boolean isUserAlreadyRegistered(String name);
}
