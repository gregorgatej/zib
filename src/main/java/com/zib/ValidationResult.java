package com.zib;

import java.nio.file.Path;
import java.util.Optional;

final class ValidationResult {
    private final Path inputFile;
    private final String errorMessage;

    private ValidationResult(Path inputFile, String errorMessage) {
        this.inputFile = inputFile;
        this.errorMessage = errorMessage;
    }

    static ValidationResult valid(Path inputFile) {
        return new ValidationResult(inputFile, null);
    }

    static ValidationResult invalid(String errorMessage) {
        return new ValidationResult(null, errorMessage);
    }

    boolean isValid() {
        return errorMessage == null;
    }

    Optional<Path> inputFile() {
        return Optional.ofNullable(inputFile);
    }

    Optional<String> errorMessage() {
        return Optional.ofNullable(errorMessage);
    }
}
