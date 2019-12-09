package org.jawk.backend;

import org.jawk.ExitException;
import org.jawk.ext.JawkExtension;
import org.jawk.intermediate.AwkIntermediateCompiler;
import org.jawk.intermediate.AwkTuples;
import org.jawk.util.AwkSettings;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class AVMTest {

    /* --- Public Methods --- */

    @Test
    public void testDontPanic() throws Exception {

        List<String> lines = interpret("BEGIN { print \"Don\47t Panic!\" }", "");
        assertEquals(Arrays.asList("Don't Panic!"), lines);
    }

    @Test
    public void testMailListLiList() throws Exception {

        String input = readFile("/org/jawk/mail-list");
        List<String> lines = interpret("/li/ {print $0}", input);
        assertEquals(Arrays.asList(
                "Amelia       555-5553     amelia.zodiacusque@gmail.com    F",
                "Broderick    555-0542     broderick.aliquotiens@yahoo.com R",
                "Julie        555-6699     julie.perscrutabor@skeeve.com   F",
                "Samuel       555-3430     samuel.lanceolis@shu.edu        A"),
                lines);
    }

    @Test
    public void testTwoRules() throws Exception {

        StringBuffer input = new StringBuffer();
        input.append(readFile("/org/jawk/mail-list"));
        input.append(readFile("/org/jawk/inventory-shipped"));
        List<String> lines = interpret("/12/ {print $0} /21/ {print $0}", input.toString());
        assertEquals(Arrays.asList(
                "Anthony      555-3412     anthony.asserturo@hotmail.com   A",
                "Camilla      555-2912     camilla.infusarum@skynet.be     R",
                "Fabius       555-1234     fabius.undevicesimus@ucb.edu    F",
                "Jean-Paul    555-2127     jeanpaul.campanorum@nyu.edu     R",
                "Jean-Paul    555-2127     jeanpaul.campanorum@nyu.edu     R",
                "Jan  21  36  64 620",
                "Apr  21  70  74 514"),
                lines);
    }

    @Test
    public void testEmptyPattern() throws Exception {

        String input = readFile("/org/jawk/inventory-shipped");
        List<String> lines = interpret("//", input);
        assertEquals(
                Arrays.asList(
                        "Jan  13  25  15 115",
                        "Feb  15  32  24 226",
                        "Mar  15  24  34 228",
                        "Apr  31  52  63 420",
                        "May  16  34  29 208",
                        "Jun  31  42  75 492",
                        "Jul  24  34  67 436",
                        "Aug  15  34  47 316",
                        "Sep  13  55  37 277",
                        "Oct  29  54  68 525",
                        "Nov  20  87  82 577",
                        "Dec  17  35  61 401",
                        "",
                        "Jan  21  36  64 620",
                        "Feb  26  58  80 652",
                        "Mar  24  75  70 495",
                        "Apr  21  70  74 514"),
                lines);
    }

    @Test
    public void testUninitializedVarible() throws Exception {

        String input = readFile("/org/jawk/inventory-shipped");
        List<String> lines = interpret("//{ if (v == 0) {print \"uninitialize variable\"} else {print}}", input);
        assertEquals(Collections.nCopies(17, "uninitialize variable"),
                lines);
    }

    @Test
    public void testUninitializedVarible2() throws Exception {

        String input = readFile("/org/jawk/inventory-shipped");
        List<String> lines = interpret("//{ v = 1; if (v == 0) {print \"uninitialize variable\"} else {print}}", input);
        assertEquals(
                Arrays.asList(
                        "Jan  13  25  15 115",
                        "Feb  15  32  24 226",
                        "Mar  15  24  34 228",
                        "Apr  31  52  63 420",
                        "May  16  34  29 208",
                        "Jun  31  42  75 492",
                        "Jul  24  34  67 436",
                        "Aug  15  34  47 316",
                        "Sep  13  55  37 277",
                        "Oct  29  54  68 525",
                        "Nov  20  87  82 577",
                        "Dec  17  35  61 401",
                        "",
                        "Jan  21  36  64 620",
                        "Feb  26  58  80 652",
                        "Mar  24  75  70 495",
                        "Apr  21  70  74 514"),
                lines);
    }

    @Test
    public void testArrayStringKey() throws Exception {

        String input = readFile("/org/jawk/inventory-shipped");
        List<String> lines = interpret("//{i=1; j=\"1\"; v[i] = 100; print v[i] v[j];}", input);
        assertEquals(Collections.nCopies(17, "100100"), lines);
    }

    @Test
    public void testArrayStringKey2() throws Exception {

        String input = readFile("/org/jawk/inventory-shipped");
        List<String> lines = interpret("//{i=1; j=\"1\"; v[j] = 100; print v[i] v[j];}", input);
        assertEquals(Collections.nCopies(17, "100100"), lines);
    }

    /* --- Private Methods --- */

    private List<String> interpret(String sourceCode, String input) throws IOException, ExitException {

        return interpret(sourceCode, input, Collections.<String, JawkExtension>emptyMap());
    }

    private List<String> interpret(String sourceCode, String input, Map<String, JawkExtension> extensions) throws IOException, ExitException {

        AwkIntermediateCompiler intermediateCompiler = new AwkIntermediateCompiler();
        AwkTuples tuples = intermediateCompiler.compile(sourceCode);

        AwkSettings settings = new AwkSettings();
        ByteArrayInputStream inStream = new ByteArrayInputStream(
                input
                        .replaceAll("\r\n|\n|\r", System.lineSeparator())
                        .getBytes(StandardCharsets.UTF_8)
        );
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        settings.setInput(inStream);
        settings.setOutput(outStream);

        AVM avm = new AVM(settings, extensions);
        avm.interpret(tuples);
        String output = new String(outStream.toByteArray(), StandardCharsets.UTF_8);
        return Arrays.asList(output.split(System.lineSeparator()));
    }

    private String readFile(String relativePath) throws URISyntaxException, IOException {

        Path path = Paths.get(this.getClass().getResource(relativePath).toURI());
        byte[] bytes = Files.readAllBytes(path);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
