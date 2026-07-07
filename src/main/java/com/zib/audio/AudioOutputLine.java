package com.zib.audio;

import javax.sound.sampled.AudioFormat;

interface AudioOutputLine {
    void open(AudioFormat format);

    void write(byte[] bytes, int offset, int length);

    void drain();

    void close();
}
