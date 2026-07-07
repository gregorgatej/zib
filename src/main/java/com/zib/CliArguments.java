package com.zib;

import java.nio.file.Files;
import java.nio.file.Path;

final class CliArguments {
    private CliArguments() {
    }

    static ValidationResult validate(String[] args) {
        if (args.length != 1) {
            return ValidationResult.invalid("exactly one .zib file argument is required");
        }

        Path inputFile = Path.of(args[0]);
        if (!Files.exists(inputFile)) {
            return ValidationResult.invalid("file does not exist: " + inputFile);
        }

        if (!Files.isRegularFile(inputFile)) {
            return ValidationResult.invalid("path is not a regular file: " + inputFile);
        }

        return ValidationResult.valid(inputFile);
    }
}
