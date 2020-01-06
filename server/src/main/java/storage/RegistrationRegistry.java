package storage;

import RMIRegistrationService.RegistrationRemoteService;
import RMIRegistrationService.RegistrationResponseStatusCode;

import java.rmi.RemoteException;

public class RegistrationRegistry implements RegistrationRemoteService {

    private static UserStorage storage;

    private static RegistrationRegistry instance;
    private RegistrationRegistry() {
    }
    public static RegistrationRegistry getInstance() {
        if (instance == null) {
            instance = new RegistrationRegistry();
            storage = UserStorage.getInstance();
        }
        return instance;
    }

    /**
     * Thread safe implementation, it is required because RMI provides only
     * Client JVM synchronization. Every updates to the primary storage level
     * is maintained also in users.
     * @param nickName
     * @param password
     * @return
     * @throws RemoteException
     */
    @Override
    public synchronized RegistrationResponseStatusCode addUser(String nickName, String password) throws RemoteException {
        if (nickName == null || nickName.equals("")) {
            return RegistrationResponseStatusCode.INVALID_NICK_ERROR;
        }
        if (password == null || password.length() < 4) {
            return RegistrationResponseStatusCode.INVALID_PASSWORD_ERROR;
        }
        if (this.isAlreadyRegistered(nickName)) {
            return RegistrationResponseStatusCode.NICK_ALREADY_REGISTERED_ERROR;
        }
        if (storage.register(nickName, password)) {
            return RegistrationResponseStatusCode.OK;
        }
        return RegistrationResponseStatusCode.INTERNAL_ERROR;
    }

    @Override
    public synchronized boolean isAlreadyRegistered(String nickName) throws RemoteException {
        return storage.exists(nickName);
    }
}
