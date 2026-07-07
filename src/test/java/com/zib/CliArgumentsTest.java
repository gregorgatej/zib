package com.zib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CliArgumentsTest {
    @TempDir
    Path tempDir;

    @Test
    void rejectsMissingArgument() {
        ValidationResult result = CliArguments.validate(new String[0]);

        assertFalse(result.isValid());
        assertEquals("exactly one .zib file argument is required", result.errorMessage().orElseThrow());
    }

    @Test
    void rejectsExtraArguments() throws IOException {
        Path inputFile = Files.createFile(tempDir.resolve("demo.zib"));

        ValidationResult result = CliArguments.validate(new String[] { inputFile.toString(), "extra" });

        assertFalse(result.isValid());
        assertEquals("exactly one .zib file argument is required", result.errorMessage().orElseThrow());
    }

    @Test
    void rejectsMissingFile() {
        Path inputFile = tempDir.resolve("missing.zib");

        ValidationResult result = CliArguments.validate(new String[] { inputFile.toString() });

        assertFalse(result.isValid());
        assertEquals("file does not exist: " + inputFile, result.errorMessage().orElseThrow());
    }

    @Test
    void rejectsDirectory() throws IOException {
        Path inputPath = Files.createDirectory(tempDir.resolve("directory.zib"));

        ValidationResult result = CliArguments.validate(new String[] { inputPath.toString() });

        assertFalse(result.isValid());
        assertEquals("path is not a regular file: " + inputPath, result.errorMessage().orElseThrow());
    }

    @Test
    void acceptsExistingRegularFile() throws IOException {
        Path inputFile = Files.createFile(tempDir.resolve("demo.zib"));

        ValidationResult result = CliArguments.validate(new String[] { inputFile.toString() });

        assertTrue(result.isValid());
        assertEquals(inputFile, result.inputFile().orElseThrow());
    }
}
