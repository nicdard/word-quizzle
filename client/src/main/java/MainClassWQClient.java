import RMIRegistrationService.RegistrationRemoteService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class MainClassWQClient {
    private static int REGISTRY_PORT = 2020;
    private static String REGISTRY_HOST = "localhost";

    public static void main(String[] args) {

        System.out.println("[Client] Starting client");

        try {
            String command;
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            do {
                System.out.println("> [help]");
                command = br.readLine().toLowerCase();
                switch(command) {
                    case "help":
                        System.out.println("Available Commands:");
                        System.out.println("- register <nickname> <password>: registers the user to WQ with the given credentials");
                        System.out.println("- login <nickname> <password>");
                        System.out.println("- logout");
                        System.out.println("- add-friend <nickFriend>: sets a friendship between this user and nickFriend");
                        System.out.println("- list-friends: lists all friends of this user");
                        System.out.println("- challenge: <nickFriend> requests a challenge to nickFriend");
                        System.out.println("- score: gets total user score");
                        System.out.println("- show-ranking-list: shows the ranking list including only this user and his/her friends");
                        System.out.println("- exit");
                        System.out.println();
                        break;
                    case "register":
                        Registry registry = LocateRegistry.getRegistry(REGISTRY_HOST, REGISTRY_PORT);
                        RegistrationRemoteService registrationService = (RegistrationRemoteService) registry.lookup(RegistrationRemoteService.REMOTE_OBJECT_NAME);
                        System.out.println("Remote registration service found");
                        break;
                    case "login":
                        System.out.println("Login called");
                        break;
                    case "add":
                    case "add-friend":
                        System.out.println("Add called");
                        break;
                    case "list":
                    case "list-friends":
                        System.out.println("Called list");
                        break;
                    case "challenge":
                        System.out.println("Challenge called");
                        break;
                    case "score":
                        System.out.println(0);
                        break;
                    case "show":
                    case "show-rankings":
                    case "show-ranking-list":
                        System.out.println("Ranking list");
                        break;
                    case "logout":
                        System.out.println("Logging you out..");
                        break;
                    case "exit":
                        break;
                }
            } while (!command.equalsIgnoreCase("exit"));

        } catch (RemoteException e) {
            System.out.println("[Client] Error in client: " + e.getMessage());
        } catch (NotBoundException e) {
            System.out.println("[Client] Remote object not found: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("[Client] Client successfully closed.");
    }

}
