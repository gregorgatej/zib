package com.zib.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import com.zib.error.ZibException;

final class SourceDataLineAudioOutputLine implements AudioOutputLine {
    private SourceDataLine sourceDataLine;

    @Override
    public void open(AudioFormat format) {
        try {
            sourceDataLine = AudioSystem.getSourceDataLine(format);
            sourceDataLine.open(format);
            sourceDataLine.start();
        } catch (LineUnavailableException exception) {
            throw new ZibException("Audio line unavailable for playback", exception);
        }
    }

    @Override
    public void write(byte[] bytes, int offset, int length) {
        sourceDataLine.write(bytes, offset, length);
    }

    @Override
    public void drain() {
        sourceDataLine.drain();
    }

    @Override
    public void close() {
        if (sourceDataLine != null) {
            sourceDataLine.stop();
            sourceDataLine.close();
            sourceDataLine = null;
        }
    }
}
