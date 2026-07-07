package com.zib.tts;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.zib.parser.ZibDocument;
import com.zib.parser.ZibToken;
import com.zib.runtime.PlaybackEvent;

public final class TtsSegmentGenerator {
    private final TtsService ttsService;

    public TtsSegmentGenerator(TtsService ttsService) {
        this.ttsService = ttsService;
    }

    public List<PlaybackEvent> generate(ZibDocument document, Path zibFile, Path tempDirectory) {
        List<PlaybackEvent> events = new ArrayList<>();
        Path zibDirectory = zibFile.toAbsolutePath().normalize().getParent();
        int segmentNumber = 1;

        for (ZibToken token : document.tokens()) {
            if (token instanceof ZibToken.Text text) {
                if (!text.value().isBlank()) {
                    Path speechFile = tempDirectory.resolve("segment-%03d.wav".formatted(segmentNumber));
                    ttsService.generateWav(text.value(), speechFile);
                    events.add(new PlaybackEvent.Speech(speechFile));
                    segmentNumber++;
                }
            } else if (token instanceof ZibToken.Sound sound) {
                events.add(new PlaybackEvent.Sound(zibDirectory.resolve(sound.filename())));
            }
        }

        return List.copyOf(events);
    }
}
