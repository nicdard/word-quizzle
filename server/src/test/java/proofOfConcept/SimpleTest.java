package proofOfConcept;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SimpleTest {

    @Test
    void completeMakesGetReturnTheNewValue() throws ExecutionException, InterruptedException {
        CompletableFuture<String> c = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "default";
        });
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            c.complete("newValue");
        });
        CompletableFuture.runAsync(() -> {
            String val = null;
            try {
                val = c.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            System.out.println("secondary thread: " + val);
            Assertions.assertEquals("newValue", val);
        });
        String val = c.get();
        System.out.println("main thread: " + val);
        Assertions.assertEquals("newValue", val);
    }
}
