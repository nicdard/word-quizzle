package translation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class TranslationsPoolTest {

    private final TranslationsPool translationsPool = TranslationsPool.getInstance(3);

    @BeforeEach
    void setLanguage() {
        this.translationsPool.setISOSourceLanguage("it");
        this.translationsPool.setISODestinationLanguage("en");
    }

    @Test
    void testUnavailableTranslationException() {
        Assertions.assertThrows(UnavailableTranslationException.class, () ->
            translationsPool.translate("available")
        );
    }

    @Test
    void testFreeSpaceIllegalStateException() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            try {
                Method freeSpace = this.translationsPool.getClass().getDeclaredMethod("freeSpace");
                Method invalidateCache = this.translationsPool.getClass().getDeclaredMethod("invalidateCache");
                freeSpace.setAccessible(true);
                invalidateCache.setAccessible(true);
                invalidateCache.invoke(this.translationsPool);
                freeSpace.invoke(this.translationsPool);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        });
    }

    @Test
    void testFreeSpaceTimestampAlgorithm() {
        try {
            this.translationsPool.setNext(new DummyTranslationService());
            this.translationsPool.setISOSourceLanguage("it");
            this.translationsPool.setISODestinationLanguage("en");
            Assertions.assertEquals(
                    "hello",
                    this.translationsPool.translate("ciao").get(0)
            );
            // To be sure the first timestamp is the eldest
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {

            }
            Assertions.assertEquals(
                    "code",
                    this.translationsPool.translate("codice").get(0)
            );
            this.translationsPool.translate("numero");
            Field field = this.translationsPool.getClass().getDeclaredField("pool");
            field.setAccessible(true);
            Map pool = (Map) field.get(this.translationsPool);
            Assertions.assertEquals(3, pool.size());
            Set keySet = new HashSet(pool.keySet());
            this.translationsPool.translate("numero");
            Assertions.assertEquals(keySet, pool.keySet());
            this.translationsPool.translate("verbo");
            Assertions.assertEquals(3, pool.size());
            Assertions.assertNotEquals(keySet, pool.keySet());
            Assertions.assertEquals(
                    new HashSet<>(Arrays.asList("numero", "codice", "verbo")),
                    pool.keySet()
            );
        } catch (UnavailableTranslationException
                | NoSuchFieldException
                | IllegalAccessException e
        ) {
            e.printStackTrace();
            System.out.println("Please review your test implementation");
        }
    }
}
