package com.zib.tts;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.zib.error.ZibException;

public final class EspeakNgService implements TtsService {
    private final ProcessRunner processRunner;

    public EspeakNgService() {
        this(new DefaultProcessRunner());
    }

    public EspeakNgService(ProcessRunner processRunner) {
        this.processRunner = processRunner;
    }

    @Override
    public void checkAvailability() {
        ProcessResult result = run(List.of("espeak-ng", "--version"), "espeak-ng was not found on PATH. Install it first, for example: sudo apt install espeak-ng");
        if (result.exitCode() != 0) {
            throw new ZibException(withStderr("espeak-ng availability check failed", result.stderr()));
        }
    }

    @Override
    public void generateWav(String text, Path outputFile) {
        List<String> command = List.of("espeak-ng", "-v", "en", "-w", outputFile.toString(), text);
        ProcessResult result = run(command, "Failed to execute espeak-ng while generating TTS segment: " + outputFile);
        if (result.exitCode() != 0) {
            throw new ZibException(withStderr("Failed to generate TTS segment: " + outputFile, result.stderr()));
        }
    }

    private ProcessResult run(List<String> command, String ioFailureMessage) {
        try {
            return processRunner.run(command);
        } catch (IOException exception) {
            throw new ZibException(ioFailureMessage, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ZibException("Interrupted while running espeak-ng", exception);
        }
    }

    private static String withStderr(String message, String stderr) {
        if (stderr == null || stderr.isBlank()) {
            return message;
        }
        return message + ": " + stderr.strip();
    }
}
