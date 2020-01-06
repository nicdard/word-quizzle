package storage.models;

public interface UserViews {
    /** Identifier of user's registration only information */
    interface Registration {
        String FILE = "registrations.json";
    }
    /** Identifier of user's friendship and scores */
    interface Online {
        String FILE = "online-info.json";
    }
}
