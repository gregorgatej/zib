package com.zib;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.zib.parser.ZibDocument;
import com.zib.parser.ZibParseException;
import com.zib.parser.ZibParser;
import com.zib.validation.ZibValidationException;
import com.zib.validation.ZibValidator;

public final class ZibApp {
    private ZibApp() {
    }

    public static void main(String[] args) {
        int exitCode = run(args, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        ValidationResult validation = CliArguments.validate(args);
        if (!validation.isValid()) {
            err.println("ERROR: " + validation.errorMessage().orElse("invalid command line arguments"));
            return 1;
        }

        Path inputFile = validation.inputFile().orElseThrow();
        try {
            String source = Files.readString(inputFile);
            ZibDocument document = new ZibParser().parse(source);
            new ZibValidator().validate(document, inputFile);

            out.println("Input accepted: " + inputFile);
            out.println("Parser and sound marker validation completed.");
            out.println("TTS and audio playback are not implemented yet.");
            return 0;
        } catch (ZibParseException | ZibValidationException exception) {
            err.println("ERROR: " + exception.getMessage());
            return 1;
        } catch (IOException exception) {
            err.println("ERROR: failed to read file: " + inputFile);
            return 1;
        }
    }
}
