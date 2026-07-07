package com.zib;

import java.io.PrintStream;

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

        out.println("Input accepted: " + validation.inputFile().orElseThrow());
        out.println("Parser and audio playback are not implemented yet.");
        return 0;
    }
}
