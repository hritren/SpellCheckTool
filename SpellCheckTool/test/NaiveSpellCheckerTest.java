import bg.sofia.uni.fmi.mjt.spellchecker.Metadata;
import bg.sofia.uni.fmi.mjt.spellchecker.NaiveSpellChecker;
import bg.sofia.uni.fmi.mjt.spellchecker.SpellChecker;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.io.StringWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class NaiveSpellCheckerTest {

    private static final String SAMPLE_DICTIONARY_INPUT = """
            cat
            name
            DoG
            love
            ell
            hello
            second
            recond
            line
            """;
    private static final String SAMPLE_STOPWORDS_INPUT = """
            my
            is
            no
            or
            a
            """;

    private static SpellChecker spellChecker;

    @Before
    public void setUp() throws IOException {
        Reader dictionary = new StringReader(SAMPLE_DICTIONARY_INPUT);
        Reader stopwords = new StringReader(SAMPLE_STOPWORDS_INPUT);
        spellChecker = new NaiveSpellChecker(dictionary, stopwords);
    }

    @Test
    public void testAnalyzeEmptyFile() {
        Reader emptyText = new StringReader("");
        String expexcted = "= = = Metadata = = =" + System.lineSeparator()
                + "0 characters, 0 words, 0 spelling issue(s) found" + System.lineSeparator()
                + "= = = Findings = = =";
        Writer outputWriter = new StringWriter();
        int suggestionCount = 3;
        spellChecker.analyze(emptyText, outputWriter, suggestionCount);
        assertEquals(expexcted, outputWriter.toString());
    }

    @Test
    public void testAnalyzeWithoutMistakes() {
        Reader text = new StringReader("Hello, my name is dog.");
        String expexcted = "Hello, my name is dog." + System.lineSeparator()
                + "= = = Metadata = = =" + System.lineSeparator()
                + "18 characters, 3 words, 0 spelling issue(s) found"
                + System.lineSeparator() + "= = = Findings = = =";
        Writer outputWriter = new StringWriter();
        int suggestionCount = 2;
        spellChecker.analyze(text, outputWriter, suggestionCount);
        assertEquals(expexcted, outputWriter.toString());
    }

    @Test
    public void testAnalyzeWithOneMistake() {
        Reader text = new StringReader("Helllo, my name is dog.");
        String expexcted = "Helllo, my name is dog." + System.lineSeparator()
                + "= = = Metadata = = =" + System.lineSeparator()
                + "19 characters, 3 words, 1 spelling issue(s) found"
                + System.lineSeparator() + "= = = Findings = = =" + System.lineSeparator()
                + "Line #1, {Helllo,} - Possible suggestions are {ell, hello}";
        Writer outputWriter = new StringWriter();
        int suggestionCount = 2;
        spellChecker.analyze(text, outputWriter, suggestionCount);
        assertEquals(expexcted, outputWriter.toString());
    }

    @Test
    public void testAnalyzeWithTwoLines() {
        Reader text = new StringReader("Helllo, my name is dog." + "\n" + "Seccond line.");
        String expexcted = "Helllo, my name is dog." + System.lineSeparator()
                + "Seccond line." + System.lineSeparator()
                + "= = = Metadata = = =" + System.lineSeparator()
                + "31 characters, 5 words, 2 spelling issue(s) found" + System.lineSeparator()
                + "= = = Findings = = =" + System.lineSeparator()
                + "Line #1, {Helllo,} - Possible suggestions are {ell, hello}" + System.lineSeparator()
                + "Line #2, {Seccond} - Possible suggestions are {recond, second}";
        Writer outputWriter = new StringWriter();
        int suggestionCount = 2;
        spellChecker.analyze(text, outputWriter, suggestionCount);
        assertEquals(expexcted, outputWriter.toString());
    }

    @Test
    public void testMetadataEmpty() {
        Reader text = new StringReader("");
        Metadata expexcted = new Metadata(0, 0, 0);
        Metadata result = spellChecker.metadata(text);
        assertEquals(expexcted.characters(), result.characters());
        assertEquals(expexcted.words(), result.words());
        assertEquals(expexcted.mistakes(), result.mistakes());
    }

    @Test
    public void testMetadataTwoLines() {
        Reader text = new StringReader("Helllo, my name is dog." + "\n" + "Seccond line.");
        Metadata expexcted = new Metadata(31, 5, 2);
        Metadata result = spellChecker.metadata(text);
        assertEquals(expexcted.characters(), result.characters());
        assertEquals(expexcted.words(), result.words());
        assertEquals(expexcted.mistakes(), result.mistakes());
    }

    @Test
    public void testFindClosestWordsEmpty() {
        List<String> expected = new ArrayList<>();
        List<String> result = spellChecker.findClosestWords("two", 0);
        assertEquals(expected, result);
    }

    @Test
    public void testFindClosestWordsOneWord() {
        List<String> expected = new ArrayList<>();
        expected.add("line");
        List<String> result = spellChecker.findClosestWords("line", 1);
        assertEquals(expected, result);
    }

    @Test
    public void testFindClosestWordsTwoWords() {
        List<String> expected = new ArrayList<>();
        expected.add("ell");
        expected.add("hello");
        List<String> result = spellChecker.findClosestWords("Helllo,", 2);
        assertEquals(expected, result);
    }
}
