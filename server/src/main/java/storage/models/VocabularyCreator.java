package storage.models;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A simple helper class.
 * NOTE: do not include when submitting the project
 * Creates or appends words to the dictionary file(s)
 */
class VocabularyCreator {

    private static boolean u = false;
    private static String f = "src/main/resources/dictionary.txt";
    private static String m;
    private static Set<String> wl;
    private static boolean s = false;

    /**
     * options: -u=[boolean] eliminates duplicates when true
     *          -f=[filename] specifies a filename different from default
     *          -m=[filename2] merges filename2 with f (writes only uniques words)
     *          -wl=[;-separated words to append]
     *          -s=[boolean] splits words in files by first letter
     */
    public static void main(String[] args) throws IOException {

        Arrays.stream(args).forEach(arg -> {
            String[] keyValue = arg.split("=");
            String key = keyValue[0];
            String value = keyValue[1];
            switch (key) {
                case "-u":
                    u = Boolean.parseBoolean(value);
                    break;
                case "-f":
                    f = value;
                    break;
                case "-m":
                    m = value;
                    break;
                case "-wl":
                    wl = new HashSet<>(Arrays.asList(value.split(";")));
                    break;
                case "-s":
                    s = Boolean.parseBoolean(value);
                    break;
                default:
                    System.out.println("Unknown option");
            }
        });

        if (m != null) {
            merge(f, m);
        }
        if (m == null && u) {
            uniques(f);
        }
    }

    /**
     * Reads f and writes to f all words (length >= 3) without duplicates
     * @param fileName
     * @throws IOException
     */
    private static void uniques(String fileName) throws IOException {
        Set<String> set = Files.readAllLines(Paths.get(fileName))
                .stream()
                .filter(a -> a.length() >= 3)
                .collect(Collectors.toCollection(TreeSet::new));
        Files.write(Paths.get(fileName), set);
    }

    private static void merge(String f1, String f2) throws IOException {
        Set<String> s = Files.readAllLines(Paths.get(f1))
                .stream()
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(TreeSet::new));
        s.addAll(Files.readAllLines(Paths.get(f1))
                        .stream()
                        .map(String::toLowerCase)
                        .collect(Collectors.toList())
        );
        write(f1, s);
    }

    private static void write(String filename, Set<String> words) throws IOException {
        if (wl != null) {
            words.addAll(wl);
        }
        if (!s) {
            Files.write(Paths.get(filename), words);
        } else {
            // Splits by first character
            Map<Character, Set<String>> wordsByChar = new HashMap<>();
            words.forEach((a) -> {
                if (!wordsByChar.containsKey(a.charAt(0))) {
                    wordsByChar.put(a.charAt(0), new HashSet<>());
                }
                wordsByChar.get(a.charAt(0)).add(a);
            });
            String filenameRadix = filename.replace(".txt", "");
            wordsByChar.forEach((k, v) -> {
                try {
                    Files.write(
                            Paths.get(filenameRadix + "-" + k + ".txt"),
                            v
                    );
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
