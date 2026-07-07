package com.zib.tts;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.zib.error.ZibException;

class EspeakNgServiceTest {
    @Test
    void availabilityCheckSucceedsWhenCommandReturnsZero() {
        RecordingProcessRunner runner = new RecordingProcessRunner(new ProcessResult(0, ""));
        EspeakNgService service = new EspeakNgService(runner);

        assertDoesNotThrow(service::checkAvailability);

        assertEquals(List.of(List.of("espeak-ng", "--version")), runner.commands());
    }

    @Test
    void availabilityCheckFailsWithClearMessageWhenCommandIsMissing() {
        RecordingProcessRunner runner = new RecordingProcessRunner(new IOException("not found"));
        EspeakNgService service = new EspeakNgService(runner);

        ZibException exception = assertThrows(ZibException.class, service::checkAvailability);

        assertEquals("espeak-ng was not found on PATH. Install it first, for example: sudo apt install espeak-ng", exception.getMessage());
    }

    @Test
    void generationBuildsExpectedArgumentList() {
        RecordingProcessRunner runner = new RecordingProcessRunner(new ProcessResult(0, ""));
        EspeakNgService service = new EspeakNgService(runner);
        Path outputFile = Path.of("/tmp/zib-test/segment-001.wav");

        service.generateWav("Hello world", outputFile);

        assertEquals(List.of(List.of("espeak-ng", "-v", "en", "-w", outputFile.toString(), "Hello world")), runner.commands());
    }

    @Test
    void nonZeroEspeakExitBecomesClearException() {
        RecordingProcessRunner runner = new RecordingProcessRunner(new ProcessResult(2, ""));
        EspeakNgService service = new EspeakNgService(runner);

        ZibException exception = assertThrows(ZibException.class,
                () -> service.generateWav("Hello", Path.of("/tmp/zib-test/segment-001.wav")));

        assertEquals("Failed to generate TTS segment: /tmp/zib-test/segment-001.wav", exception.getMessage());
    }

    @Test
    void stderrFromEspeakIsIncludedInErrorMessage() {
        RecordingProcessRunner runner = new RecordingProcessRunner(new ProcessResult(2, "bad voice\n"));
        EspeakNgService service = new EspeakNgService(runner);

        ZibException exception = assertThrows(ZibException.class,
                () -> service.generateWav("Hello", Path.of("/tmp/zib-test/segment-001.wav")));

        assertTrue(exception.getMessage().contains("Failed to generate TTS segment: /tmp/zib-test/segment-001.wav"));
        assertTrue(exception.getMessage().contains("bad voice"));
    }

    private static final class RecordingProcessRunner implements ProcessRunner {
        private final Object resultOrException;
        private final List<List<String>> commands = new ArrayList<>();

        private RecordingProcessRunner(Object resultOrException) {
            this.resultOrException = resultOrException;
        }

        @Override
        public ProcessResult run(List<String> command) throws IOException {
            commands.add(List.copyOf(command));
            if (resultOrException instanceof IOException exception) {
                throw exception;
            }
            return (ProcessResult) resultOrException;
        }

        private List<List<String>> commands() {
            return List.copyOf(commands);
        }
    }
}
