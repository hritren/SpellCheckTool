package bg.sofia.uni.fmi.mjt.spellchecker;

import java.io.Reader;
import java.io.BufferedReader;
import java.io.Writer;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;


public class NaiveSpellChecker implements SpellChecker {

    private final Set<String> stopwords;
    private final Set<String> dictionary;

    public NaiveSpellChecker(Reader dictionaryReader, Reader stopwordsReader) {
        try (var dr = new BufferedReader(dictionaryReader);
             var sr = new BufferedReader(stopwordsReader)) {
            dictionary = dr.lines().map(word -> word.toLowerCase())
                    .map(word -> word.trim())
                    .map(word -> word.replaceAll("^[^a-zA-Z0-9\\s]+|[^a-zA-Z0-9\\s]+$", ""))
                    .filter(word -> word.length() != 1)
                    .collect(Collectors.toSet());
            stopwords = sr.lines().map(word -> word.toLowerCase())
                    .map(word -> word.trim())
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new IllegalStateException("A problem occurred while reading form a file", e);
        }
    }

    private Map<String, Integer> toGrams(String word) {
        Map<String, Integer> result = new HashMap<>();
        for (int i = 0; i < word.length() - 1; i++) {
            String twoGram = word.substring(i, i + 2);
            int count = 0;
            int fromIndex = 0;
            while ((fromIndex = word.indexOf(twoGram, fromIndex)) != -1) {
                count++;
                fromIndex++;
            }
            result.put(twoGram, count);
        }
        return result;
    }

    private int squareSum(Map<String, Integer> vector) {
        int sum = 0;
        for (Integer i : vector.values()) {
            sum += i * i;
        }
        return sum;
    }

    public double cosineSimilarity(String word1, String word2) {
        Map<String, Integer> vector1 = toGrams(word1);
        Map<String, Integer> vector2 = toGrams(word2);

        double len1 = Math.sqrt(squareSum(vector1));
        double len2 = Math.sqrt(squareSum(vector2));

        double dotProduct = 0;
        for (String s : vector1.keySet()) {
            if (vector2.containsKey(s)) {
                dotProduct += vector1.get(s) * vector2.get(s);
            }
        }
        return dotProduct / (len1 * len2);
    }

    private boolean isMisspelled(String word) {
        String lowerTrimmed = word.toLowerCase().trim();
        String lowerTrimmedAlphaNum = lowerTrimmed.replaceAll("^[^a-zA-Z0-9\\s]+|[^a-zA-Z0-9\\s]+$", "");
        return !stopwords.contains(lowerTrimmed) && !dictionary.contains(lowerTrimmedAlphaNum);
    }

    private String findingsLine(String word, int suggestionsCount) {
        String result = ", {" + word + "} - Possible suggestions are {";
        List<String> suggestions = findClosestWords(word, suggestionsCount);
        boolean isFirstIteration = true;
        for (String s : suggestions) {
            if (isFirstIteration) {
                isFirstIteration = false;
                result += s;
            } else {
                result += ", " + s;
            }
        }
        result += "}";
        return result;
    }

    @Override
    public void analyze(Reader textReader, Writer output, int suggestionsCount) {
        if (suggestionsCount < 0) {
            throw new IllegalArgumentException("suggestionCount is negative");
        }
        try (var bw = new BufferedWriter(output);
             var br = new BufferedReader(textReader)) {
            String line;
            int characters = 0;
            int words = 0;
            int mistakes = 0;
            Set<String> misspelledWords = new LinkedHashSet<>();
            while ((line = br.readLine()) != null) {
                bw.write(line + System.lineSeparator());
                bw.flush();
                String[] tokens = line.split("\\s+");
                String mistakenWords = "";
                for (String s : tokens) {
                    Metadata lineData = lineMeta(s);
                    characters += lineData.characters();
                    words += lineData.words();
                    mistakes += lineData.mistakes();
                    if (isMisspelled(s)) {
                        mistakenWords += s + " ";
                    }
                }
                if (!mistakenWords.isEmpty()) {
                    misspelledWords.add(mistakenWords);
                }
            }

            bw.write("= = = Metadata = = =" + System.lineSeparator());
            bw.write(characters + " characters, " + words + " words, "
                    + mistakes + " spelling issue(s) found" + System.lineSeparator());
            bw.write("= = = Findings = = =");
            bw.flush();
            int lineNumber = 1;
            for (String s : misspelledWords) {
                String[] mistakenWords = s.split("\\s+");
                bw.write(System.lineSeparator() + "Line #" + lineNumber);
                for (String word : mistakenWords) {
                    bw.write(findingsLine(word, suggestionsCount));
                    bw.flush();
                    lineNumber++;
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("A problem occurred while writing form a file", e);
        }
    }

    private boolean hasAlphaNumericCharacter(String word) {
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if ((c >= 'A' && c <= 'Z')
                    || (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9')) {
                return true;
            }
        }
        return false;
    }

    private Metadata lineMeta(String line) {
        int words = 0;
        int characters = 0;
        int mistakes = 0;
        String[] tokens = line.split("\\s+");
        for (String s : tokens) {
            String lowerTrimmed = s.toLowerCase().trim();
            String lowerTrimmedAlphaNum = s.toLowerCase()
                    .trim()
                    .replaceAll("^[^a-zA-Z0-9\\s]+|[^a-zA-Z0-9\\s]+$", "");
            if (!stopwords.contains(lowerTrimmed) && hasAlphaNumericCharacter(lowerTrimmedAlphaNum)) {
                words++;
            }
            characters += s.length();
            if (!stopwords.contains(lowerTrimmed) && !dictionary.contains(lowerTrimmedAlphaNum)) {
                mistakes++;
            }
        }
        return new Metadata(characters, words, mistakes);
    }

    @Override
    public Metadata metadata(Reader textReader) {
        int characters = 0;
        int words = 0;
        int mistakes = 0;
        String line;
        try (var br = new BufferedReader(textReader)) {
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split("\\s+");
                for (String s : tokens) {
                    Metadata lineData = lineMeta(s);
                    characters += lineData.characters();
                    words += lineData.words();
                    mistakes += lineData.mistakes();
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("A problem occurred while reading form a file", e);
        }
        return new Metadata(characters, words, mistakes);
    }

    @Override
    public List<String> findClosestWords(String word, int n) {
        if (word == null) {
            throw new IllegalArgumentException("word is null!");
        }
        if (n < 0) {
            throw new IllegalArgumentException("n is negative");
        }
        List<String> result;
        List<Pair> cosineSimilarities = new ArrayList<>();
        for (String s : dictionary) {
            Pair pair = new Pair(s, cosineSimilarity(s, word));
            cosineSimilarities.add(pair);
        }
        result = cosineSimilarities.stream().sorted().map(pair -> pair.word()).collect(Collectors.toList());
        if (result.size() > n) {
            result = result.subList(0, n);
        }
        return result;
    }
}
