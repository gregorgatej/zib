package com.zib.tts;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.zib.parser.ZibDocument;
import com.zib.parser.ZibParser;
import com.zib.parser.ZibToken;
import com.zib.runtime.PlaybackEvent;

class TtsSegmentGeneratorTest {
    @TempDir
    Path tempDir;

    @Test
    void generatesOneWavPerTextSegmentWithDeterministicNames() {
        RecordingTtsService ttsService = new RecordingTtsService();
        TtsSegmentGenerator generator = new TtsSegmentGenerator(ttsService);
        Path zibFile = tempDir.resolve("demo.zib");
        Path ttsDir = tempDir.resolve("zib-generated");
        ZibDocument document = new ZibDocument(List.of(
                new ZibToken.Text("Hello "),
                new ZibToken.Sound("effect.wav"),
                new ZibToken.Text("world")));

        List<PlaybackEvent> events = generator.generate(document, zibFile, ttsDir);

        assertEquals(List.of(
                new Generation("Hello ", ttsDir.resolve("segment-001.wav")),
                new Generation("world", ttsDir.resolve("segment-002.wav"))),
                ttsService.generations());
        assertEquals(List.of(
                new PlaybackEvent.Speech(ttsDir.resolve("segment-001.wav")),
                new PlaybackEvent.Sound(tempDir.resolve("effect.wav")),
                new PlaybackEvent.Speech(ttsDir.resolve("segment-002.wav"))),
                events);
    }

    @Test
    void skipsBlankTextTokensButPreservesSoundEvents() {
        RecordingTtsService ttsService = new RecordingTtsService();
        TtsSegmentGenerator generator = new TtsSegmentGenerator(ttsService);
        Path zibFile = tempDir.resolve("demo.zib");
        Path ttsDir = tempDir.resolve("zib-generated");
        ZibDocument document = new ZibDocument(List.of(
                new ZibToken.Text("   "),
                new ZibToken.Sound("effect.wav"),
                new ZibToken.Text("spoken")));

        List<PlaybackEvent> events = generator.generate(document, zibFile, ttsDir);

        assertEquals(List.of(new Generation("spoken", ttsDir.resolve("segment-001.wav"))), ttsService.generations());
        assertEquals(List.of(
                new PlaybackEvent.Sound(tempDir.resolve("effect.wav")),
                new PlaybackEvent.Speech(ttsDir.resolve("segment-001.wav"))),
                events);
    }

    @Test
    void convertsRequiredDemoDocumentToExpectedPlaybackEvents() {
        RecordingTtsService ttsService = new RecordingTtsService();
        TtsSegmentGenerator generator = new TtsSegmentGenerator(ttsService);
        Path zibFile = tempDir.resolve("demo.zib");
        Path ttsDir = tempDir.resolve("zib-generated");
        ZibDocument document = new ZibParser().parse("\"Today is a beautiful day. The children are playing outside ${children_laughing.wav} on the playground.\"");

        List<PlaybackEvent> events = generator.generate(document, zibFile, ttsDir);

        assertEquals(List.of(
                new Generation("Today is a beautiful day. The children are playing outside ", ttsDir.resolve("segment-001.wav")),
                new Generation(" on the playground.", ttsDir.resolve("segment-002.wav"))),
                ttsService.generations());
        assertEquals(List.of(
                new PlaybackEvent.Speech(ttsDir.resolve("segment-001.wav")),
                new PlaybackEvent.Sound(tempDir.resolve("children_laughing.wav")),
                new PlaybackEvent.Speech(ttsDir.resolve("segment-002.wav"))),
                events);
    }

    private record Generation(String text, Path outputFile) {
    }

    private static final class RecordingTtsService implements TtsService {
        private final List<Generation> generations = new ArrayList<>();

        @Override
        public void checkAvailability() {
        }

        @Override
        public void generateWav(String text, Path outputFile) {
            generations.add(new Generation(text, outputFile));
        }

        private List<Generation> generations() {
            return List.copyOf(generations);
        }
    }
}
