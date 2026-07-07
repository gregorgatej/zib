package com.zib.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TempDirectoryManagerTest {
    @TempDir
    Path tempDir;

    @Test
    void createsTempDirectoryWithZibPrefix() {
        TempDirectoryManager manager = new TempDirectoryManager(tempDir);

        Path created = manager.createTempDirectory();

        assertTrue(Files.isDirectory(created));
        assertTrue(created.getFileName().toString().startsWith("zib-"));
    }

    @Test
    void deletesTempDirectoryAfterSuccess() throws IOException {
        TempDirectoryManager manager = new TempDirectoryManager(tempDir);
        Path created = manager.createTempDirectory();
        Files.createFile(created.resolve("segment-001.wav"));

        manager.deleteAfterSuccess(created);

        assertFalse(Files.exists(created));
    }

    @Test
    void leavesTempDirectoryAfterFailure() throws IOException {
        TempDirectoryManager manager = new TempDirectoryManager(tempDir);
        Path created = manager.createTempDirectory();
        Files.createFile(created.resolve("segment-001.wav"));

        manager.keepAfterFailure(created);

        assertTrue(Files.isDirectory(created));
        assertTrue(Files.isRegularFile(created.resolve("segment-001.wav")));
    }

    @Test
    void recursiveDeleteHandlesGeneratedFiles() throws IOException {
        TempDirectoryManager manager = new TempDirectoryManager(tempDir);
        Path created = manager.createTempDirectory();
        Path nested = Files.createDirectory(created.resolve("nested"));
        Files.createFile(nested.resolve("segment-001.wav"));

        manager.deleteAfterSuccess(created);

        assertFalse(Files.exists(created));
    }
}
