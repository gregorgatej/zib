package com.zib.runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import com.zib.error.ZibException;

public final class TempDirectoryManager {
    private final Path parentDirectory;

    public TempDirectoryManager() {
        this(Path.of(System.getProperty("java.io.tmpdir")));
    }

    public TempDirectoryManager(Path parentDirectory) {
        this.parentDirectory = parentDirectory;
    }

    public Path createTempDirectory() {
        try {
            return Files.createTempDirectory(parentDirectory, "zib-");
        } catch (IOException exception) {
            throw new ZibException("Failed to create temporary directory", exception);
        }
    }

    public void deleteAfterSuccess(Path tempDirectory) {
        try (Stream<Path> paths = Files.walk(tempDirectory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(this::delete);
        } catch (IOException exception) {
            throw new ZibException("Failed to delete temporary directory after success: " + tempDirectory, exception);
        }
    }

    public void keepAfterFailure(Path tempDirectory) {
        // Intentionally retained for debugging.
    }

    private void delete(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            throw new ZibException("Failed to delete temporary path: " + path, exception);
        }
    }
}
