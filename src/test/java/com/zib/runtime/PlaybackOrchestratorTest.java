package com.zib.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.zib.audio.AudioPlayer;

class PlaybackOrchestratorTest {
    @Test
    void speechOnlyDocumentCallsPlayBlockingForEachSegmentInOrder() {
        RecordingAudioPlayer audioPlayer = new RecordingAudioPlayer();
        PlaybackOrchestrator orchestrator = new PlaybackOrchestrator(audioPlayer);

        orchestrator.play(List.of(
                new PlaybackEvent.Speech(Path.of("segment-001.wav")),
                new PlaybackEvent.Speech(Path.of("segment-002.wav"))));

        assertEquals(List.of(
                "blocking:segment-001.wav",
                "blocking:segment-002.wav"),
                audioPlayer.events());
    }

    @Test
    void soundMarkersCallPlayInBackground() {
        RecordingAudioPlayer audioPlayer = new RecordingAudioPlayer();
        PlaybackOrchestrator orchestrator = new PlaybackOrchestrator(audioPlayer);

        orchestrator.play(List.of(new PlaybackEvent.Sound(Path.of("effect.wav"))));

        assertEquals(List.of("background:effect.wav"), audioPlayer.events());
    }

    @Test
    void soundMarkerDoesNotBlockNextSpeech() {
        RecordingAudioPlayer audioPlayer = new RecordingAudioPlayer();
        PlaybackOrchestrator orchestrator = new PlaybackOrchestrator(audioPlayer);

        orchestrator.play(List.of(
                new PlaybackEvent.Sound(Path.of("effect.wav")),
                new PlaybackEvent.Speech(Path.of("segment-001.wav"))));

        assertEquals(List.of(
                "background:effect.wav",
                "blocking:segment-001.wav"),
                audioPlayer.events());
        assertFalse(audioPlayer.backgroundWaited());
    }

    @Test
    void orchestratorDoesNotWaitForBackgroundSoundsAfterFinalSpeech() {
        RecordingAudioPlayer audioPlayer = new RecordingAudioPlayer();
        PlaybackOrchestrator orchestrator = new PlaybackOrchestrator(audioPlayer);

        orchestrator.play(List.of(
                new PlaybackEvent.Speech(Path.of("segment-001.wav")),
                new PlaybackEvent.Sound(Path.of("effect.wav"))));

        assertEquals(List.of(
                "blocking:segment-001.wav",
                "background:effect.wav"),
                audioPlayer.events());
        assertTrue(audioPlayer.returnedFromBackgroundStart());
    }

    @Test
    void multipleSoundsAndSpeechSegmentsPreserveDemoEventOrder() {
        RecordingAudioPlayer audioPlayer = new RecordingAudioPlayer();
        PlaybackOrchestrator orchestrator = new PlaybackOrchestrator(audioPlayer);

        orchestrator.play(List.of(
                new PlaybackEvent.Speech(Path.of("segment-001.wav")),
                new PlaybackEvent.Sound(Path.of("otroski_smeh.wav")),
                new PlaybackEvent.Speech(Path.of("segment-002.wav"))));

        assertEquals(List.of(
                "blocking:segment-001.wav",
                "background:otroski_smeh.wav",
                "blocking:segment-002.wav"),
                audioPlayer.events());
    }

    private static final class RecordingAudioPlayer implements AudioPlayer {
        private final List<String> events = new ArrayList<>();
        private boolean backgroundWaited;
        private boolean returnedFromBackgroundStart;

        @Override
        public void playBlocking(Path wavFile) {
            events.add("blocking:" + wavFile);
        }

        @Override
        public void playInBackground(Path wavFile) {
            events.add("background:" + wavFile);
            returnedFromBackgroundStart = true;
        }

        private List<String> events() {
            return List.copyOf(events);
        }

        private boolean backgroundWaited() {
            return backgroundWaited;
        }

        private boolean returnedFromBackgroundStart() {
            return returnedFromBackgroundStart;
        }
    }
}
