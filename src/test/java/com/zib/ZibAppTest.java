package com.zib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ZibAppTest {
    @TempDir
    Path tempDir;

    @Test
    void writesUserFacingErrorForInvalidArguments() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = ZibApp.run(new String[0], printStream(out), printStream(err));

        assertEquals(1, exitCode);
        assertEquals("", out.toString(StandardCharsets.UTF_8));
        assertEquals("ERROR: exactly one .zib file argument is required%n".formatted(), err.toString(StandardCharsets.UTF_8));
    }

    @Test
    void acceptsRegularFileWithoutStartingParserOrAudio() throws IOException {
        Path inputFile = Files.createFile(tempDir.resolve("demo.zib"));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = ZibApp.run(new String[] { inputFile.toString() }, printStream(out), printStream(err));

        assertEquals(0, exitCode);
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("Input accepted: " + inputFile));
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("Parser and audio playback are not implemented yet."));
        assertEquals("", err.toString(StandardCharsets.UTF_8));
    }

    private static PrintStream printStream(ByteArrayOutputStream output) {
        return new PrintStream(output, true, StandardCharsets.UTF_8);
    }
}
