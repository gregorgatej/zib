package com.zib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.zib.audio.AudioPlayer;
import com.zib.error.ZibException;
import com.zib.runtime.TempDirectoryManager;
import com.zib.tts.TtsService;

class ZibAppTest {
    @TempDir
    Path tempDir;

    @Test
    void writesUserFacingErrorForInvalidArguments() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = ZibApp.run(new String[0], printStream(out), printStream(err), new FakeTtsService(), new TempDirectoryManager(tempDir), new FakeAudioPlayer());

        assertEquals(1, exitCode);
        assertEquals("", out.toString(StandardCharsets.UTF_8));
        assertEquals("ERROR: exactly one .zib file argument is required%n".formatted(), err.toString(StandardCharsets.UTF_8));
    }

    @Test
    void parsesValidatesGeneratesTtsAndRunsPlayback() throws IOException {
        Path inputFile = Files.createFile(tempDir.resolve("demo.zib"));
        Files.writeString(inputFile, "\"Hello ${effect.wav}\"", StandardCharsets.UTF_8);
        Files.createFile(tempDir.resolve("effect.wav"));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        FakeTtsService ttsService = new FakeTtsService();
        FakeAudioPlayer audioPlayer = new FakeAudioPlayer();

        int exitCode = ZibApp.run(new String[] { inputFile.toString() }, printStream(out), printStream(err), ttsService, new TempDirectoryManager(tempDir), audioPlayer);

        assertEquals(0, exitCode);
        assertTrue(ttsService.availabilityChecked());
        assertEquals(1, ttsService.generatedCount());
        assertEquals(List.of("blocking:segment-001.wav", "background:effect.wav"), audioPlayer.events());
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("Input accepted: " + inputFile));
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("Parser and sound marker validation completed."));
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("Generated and played events: 2"));
        assertEquals("", err.toString(StandardCharsets.UTF_8));
        assertFalse(hasZibTempDirectory());
    }

    @Test
    void writesUserFacingErrorForMissingReferencedSound() throws IOException {
        Path inputFile = Files.createFile(tempDir.resolve("demo.zib"));
        Files.writeString(inputFile, "\"Hello ${missing.wav}\"", StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = ZibApp.run(new String[] { inputFile.toString() }, printStream(out), printStream(err), new FakeTtsService(), new TempDirectoryManager(tempDir), new FakeAudioPlayer());

        assertEquals(1, exitCode);
        assertEquals("", out.toString(StandardCharsets.UTF_8));
        assertEquals("ERROR: Referenced sound file not found next to .zib file: missing.wav%n".formatted(),
                err.toString(StandardCharsets.UTF_8));
    }

    @Test
    void checksEspeakAvailabilityBeforeCreatingTempDirectory() throws IOException {
        Path inputFile = Files.createFile(tempDir.resolve("demo.zib"));
        Files.writeString(inputFile, "\"Hello\"", StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        FakeTtsService ttsService = new FakeTtsService();
        ttsService.failAvailabilityWith("espeak-ng was not found on PATH. Install it first, for example: sudo apt install espeak-ng");

        int exitCode = ZibApp.run(new String[] { inputFile.toString() }, printStream(out), printStream(err), ttsService, new TempDirectoryManager(tempDir), new FakeAudioPlayer());

        assertEquals(1, exitCode);
        assertEquals("", out.toString(StandardCharsets.UTF_8));
        assertEquals("ERROR: espeak-ng was not found on PATH. Install it first, for example: sudo apt install espeak-ng%n".formatted(),
                err.toString(StandardCharsets.UTF_8));
        assertFalse(hasZibTempDirectory());
    }

    @Test
    void keepsTempDirectoryAndReportsPathWhenTtsGenerationFails() throws IOException {
        Path inputFile = Files.createFile(tempDir.resolve("demo.zib"));
        Files.writeString(inputFile, "\"Hello\"", StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        FakeTtsService ttsService = new FakeTtsService();
        ttsService.failGenerationWith("TTS failed");

        int exitCode = ZibApp.run(new String[] { inputFile.toString() }, printStream(out), printStream(err), ttsService, new TempDirectoryManager(tempDir), new FakeAudioPlayer());

        assertEquals(1, exitCode);
        assertEquals("", out.toString(StandardCharsets.UTF_8));
        assertTrue(err.toString(StandardCharsets.UTF_8).contains("ERROR: TTS failed. Temporary files kept at: "));
        assertTrue(hasZibTempDirectory());
    }

    @Test
    void keepsTempDirectoryAndReportsPathWhenPlaybackFails() throws IOException {
        Path inputFile = Files.createFile(tempDir.resolve("demo.zib"));
        Files.writeString(inputFile, "\"Hello\"", StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        FakeAudioPlayer audioPlayer = new FakeAudioPlayer();
        audioPlayer.failBlockingWith("Playback failed");

        int exitCode = ZibApp.run(new String[] { inputFile.toString() }, printStream(out), printStream(err), new FakeTtsService(), new TempDirectoryManager(tempDir), audioPlayer);

        assertEquals(1, exitCode);
        assertEquals("", out.toString(StandardCharsets.UTF_8));
        assertTrue(err.toString(StandardCharsets.UTF_8).contains("ERROR: Playback failed. Temporary files kept at: "));
        assertTrue(hasZibTempDirectory());
    }

    private static PrintStream printStream(ByteArrayOutputStream output) {
        return new PrintStream(output, true, StandardCharsets.UTF_8);
    }

    private boolean hasZibTempDirectory() throws IOException {
        try (var paths = Files.list(tempDir)) {
            return paths.anyMatch(path -> Files.isDirectory(path) && path.getFileName().toString().startsWith("zib-"));
        }
    }

    private static final class FakeTtsService implements TtsService {
        private boolean availabilityChecked;
        private int generatedCount;
        private String availabilityFailure;
        private String generationFailure;

        @Override
        public void checkAvailability() {
            availabilityChecked = true;
            if (availabilityFailure != null) {
                throw new ZibException(availabilityFailure);
            }
        }

        @Override
        public void generateWav(String text, Path outputFile) {
            if (generationFailure != null) {
                throw new ZibException(generationFailure);
            }
            generatedCount++;
        }

        private boolean availabilityChecked() {
            return availabilityChecked;
        }

        private int generatedCount() {
            return generatedCount;
        }

        private void failAvailabilityWith(String message) {
            availabilityFailure = message;
        }

        private void failGenerationWith(String message) {
            generationFailure = message;
        }
    }

    private static final class FakeAudioPlayer implements AudioPlayer {
        private final List<String> events = new ArrayList<>();
        private String blockingFailure;

        @Override
        public void playBlocking(Path wavFile) {
            if (blockingFailure != null) {
                throw new ZibException(blockingFailure);
            }
            events.add("blocking:" + wavFile.getFileName());
        }

        @Override
        public void playInBackground(Path wavFile) {
            events.add("background:" + wavFile.getFileName());
        }

        private List<String> events() {
            return List.copyOf(events);
        }

        private void failBlockingWith(String message) {
            blockingFailure = message;
        }
    }
}
