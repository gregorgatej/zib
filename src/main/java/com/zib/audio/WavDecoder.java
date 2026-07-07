package com.zib.audio;

import java.nio.file.Path;

interface WavDecoder {
    WavData decode(Path wavFile);
}
