package cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;

@Deprecated
public class CliReadTask implements Callable<String> {

    public String call() throws IOException {
        BufferedReader br = new BufferedReader(
                new InputStreamReader(System.in));
        String input;
        try {
            // wait until we have data to complete a readLine()
            while (!br.ready()) {
                Thread.sleep(200);
            }
            input = br.readLine();
        } catch (InterruptedException e) {
            System.out.println("[DEBUG] CliReadTask() cancelled");
            return null;
            }
        System.out.println("[DEBUG] Thank You for providing input!");
        return input;
    }

}

