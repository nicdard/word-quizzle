import java.rmi.Remote;

/**
 * Registration Service Methods
 */
public interface RegistrationRemoteService extends Remote {

    String REMOTE_OBJECT_NAME = "RegistrationRemoteService";

    /**
     * Tries to register the user to WQ. A successful registration will be performed
     * only if the given nickName is not empty and not already registered to the service
     * and a valid password is provided (it must have at least 4 characters).
     * @param nickName
     * @param password
     * @return the status code of the operation indicating a successful operation
     * or an error cause
     */
    RegistrationResponseStatusCode addUser(String nickName, String password);

    /**
     * Checks whether nickName is a registered nickName of WQ.
     * @param nickName
     * @return true if nickName is found, false otherwise
     */
    boolean isNickAlreadyRegistered(String nickName);
}
