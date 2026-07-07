package com.zib.tts;

import java.nio.file.Path;

public interface TtsService {
    void checkAvailability();

    void generateWav(String text, Path outputFile);
}
