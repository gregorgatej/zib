package com.zib.audio;

import java.nio.file.Path;

public interface AudioPlayer {
    void playBlocking(Path wavFile);

    void playInBackground(Path wavFile);
}
