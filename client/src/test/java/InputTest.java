
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.concurrent.CompletableFuture;

public class InputTest {

    private static final Reader r = new InputStreamReader(System.in);
    private static final BufferedReader reader = new BufferedReader(r);
    private static final BufferedReader reader2 = new BufferedReader(r);

    public static void main(String[] args) {
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Hai 5 richieste, quale vuoi [1-5]");
            String line2 = null;
            /*try {
                reader.close();
            } catch (IOException e) {
                System.out.println("asdaa");
                e.printStackTrace();
            }
            */
            try {
                line2 = reader2.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("\nline2 " + line2);
        });
        try {
            System.out.println("Premi invio per uscire");
            String line = reader.readLine();
            if (line.equals("")) {
                System.out.println("Bravo");
                Thread.sleep(1000);
                return;
            }
            System.out.println(line);
            Thread.sleep(1000);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
