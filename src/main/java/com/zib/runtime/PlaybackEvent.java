package com.zib.runtime;

import java.nio.file.Path;

public sealed interface PlaybackEvent permits PlaybackEvent.Speech, PlaybackEvent.Sound {
    record Speech(Path wavFile) implements PlaybackEvent {
    }

    record Sound(Path wavFile) implements PlaybackEvent {
    }
}
