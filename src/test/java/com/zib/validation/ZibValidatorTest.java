package com.zib.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.zib.parser.ZibDocument;
import com.zib.parser.ZibToken;

class ZibValidatorTest {
    private final ZibValidator validator = new ZibValidator();

    @TempDir
    Path tempDir;

    @Test
    void acceptsExistingWavInSameDirectoryAsZibFile() throws IOException {
        Path zibFile = Files.createFile(tempDir.resolve("demo.zib"));
        Files.createFile(tempDir.resolve("otroski_smeh.wav"));
        ZibDocument document = documentWithSound("otroski_smeh.wav");

        assertDoesNotThrow(() -> validator.validate(document, zibFile));
    }

    @Test
    void rejectsMissingWavFile() throws IOException {
        Path zibFile = Files.createFile(tempDir.resolve("demo.zib"));
        ZibDocument document = documentWithSound("missing.wav");

        ZibValidationException exception = assertValidationError(document, zibFile);

        assertEquals("Referenced sound file not found next to .zib file: missing.wav", exception.getMessage());
    }

    @Test
    void rejectsAbsolutePathMarker() throws IOException {
        Path zibFile = Files.createFile(tempDir.resolve("demo.zib"));
        ZibDocument document = documentWithSound("/tmp/sound.wav");

        ZibValidationException exception = assertValidationError(document, zibFile);

        assertEquals("Sound marker must not be an absolute path: /tmp/sound.wav", exception.getMessage());
    }

    @Test
    void rejectsParentTraversalMarker() throws IOException {
        Path zibFile = Files.createFile(tempDir.resolve("demo.zib"));
        ZibDocument document = documentWithSound("../sound.wav");

        ZibValidationException exception = assertValidationError(document, zibFile);

        assertEquals("Sound marker must not contain parent traversal: ../sound.wav", exception.getMessage());
    }

    @Test
    void rejectsNestedPathMarker() throws IOException {
        Path zibFile = Files.createFile(tempDir.resolve("demo.zib"));
        ZibDocument document = documentWithSound("sounds/x.wav");

        ZibValidationException exception = assertValidationError(document, zibFile);

        assertEquals("Sound marker must be a filename, not a path: sounds/x.wav", exception.getMessage());
    }

    @Test
    void rejectsNonRegularReferencedFile() throws IOException {
        Path zibFile = Files.createFile(tempDir.resolve("demo.zib"));
        Files.createDirectory(tempDir.resolve("effect.wav"));
        ZibDocument document = documentWithSound("effect.wav");

        ZibValidationException exception = assertValidationError(document, zibFile);

        assertEquals("Referenced sound file is not a regular file: effect.wav", exception.getMessage());
    }

    @Test
    void rejectsNonWavMarker() throws IOException {
        Path zibFile = Files.createFile(tempDir.resolve("demo.zib"));
        ZibDocument document = documentWithSound("sound.mp3");

        ZibValidationException exception = assertValidationError(document, zibFile);

        assertEquals("Sound marker must reference a .wav file: sound.mp3", exception.getMessage());
    }

    private ZibValidationException assertValidationError(ZibDocument document, Path zibFile) {
        return assertThrows(ZibValidationException.class, () -> validator.validate(document, zibFile));
    }

    private static ZibDocument documentWithSound(String marker) {
        return new ZibDocument(List.of(new ZibToken.Sound(marker)));
    }
}
