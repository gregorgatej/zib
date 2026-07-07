package com.zib.audio;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.sound.sampled.AudioFormat;

public final class JavaSoundAudioPlayer implements AudioPlayer {
    static final AudioFormat CANONICAL_FORMAT = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            44_100,
            16,
            2,
            4,
            44_100,
            false);

    private static final int DEFAULT_CHUNK_FRAMES = 1024;

    private final WavDecoder wavDecoder;
    private final AudioOutputLine outputLine;
    private final int chunkFrames;
    private final List<ActiveBackground> activeBackgrounds = new ArrayList<>();
    private boolean outputLineOpen;

    public JavaSoundAudioPlayer() {
        this(new JavaSoundWavDecoder(CANONICAL_FORMAT), new SourceDataLineAudioOutputLine(), DEFAULT_CHUNK_FRAMES);
    }

    JavaSoundAudioPlayer(WavDecoder wavDecoder, AudioOutputLine outputLine, int chunkFrames) {
        this.wavDecoder = wavDecoder;
        this.outputLine = outputLine;
        this.chunkFrames = chunkFrames;
    }

    @Override
    public void playBlocking(Path wavFile) {
        WavData speech = wavDecoder.decode(wavFile);
        ensureOutputLineOpen(speech.format());

        int frameSize = speech.format().getFrameSize();
        int chunkBytes = chunkFrames * frameSize;
        int offset = 0;
        while (offset < speech.pcmBytes().length) {
            int length = Math.min(chunkBytes, speech.pcmBytes().length - offset);
            int frames = length / frameSize;
            byte[] mixed = mix(speech.pcmBytes(), offset, frames, frameSize);
            outputLine.write(mixed, 0, mixed.length);
            offset += mixed.length;
        }

        outputLine.drain();
    }

    @Override
    public void playInBackground(Path wavFile) {
        WavData effect = wavDecoder.decode(wavFile);
        activeBackgrounds.add(new ActiveBackground(effect.pcmBytes()));
    }

    @Override
    public void close() {
        activeBackgrounds.clear();
        if (outputLineOpen) {
            outputLine.close();
            outputLineOpen = false;
        }
    }

    private void ensureOutputLineOpen(AudioFormat format) {
        if (!outputLineOpen) {
            outputLine.open(format);
            outputLineOpen = true;
        }
    }

    private byte[] mix(byte[] speechBytes, int speechOffset, int frames, int frameSize) {
        byte[] mixed = new byte[frames * frameSize];
        for (int byteOffset = 0; byteOffset < mixed.length; byteOffset += 2) {
            int sample = readLittleEndian16(speechBytes, speechOffset + byteOffset);
            for (ActiveBackground background : activeBackgrounds) {
                if (background.hasBytes(byteOffset + 1)) {
                    sample += readLittleEndian16(background.bytes(), background.offset() + byteOffset);
                }
            }
            writeLittleEndian16(mixed, byteOffset, clampToInt16(sample));
        }

        advanceBackgrounds(mixed.length);
        return mixed;
    }

    private void advanceBackgrounds(int bytes) {
        Iterator<ActiveBackground> iterator = activeBackgrounds.iterator();
        while (iterator.hasNext()) {
            ActiveBackground background = iterator.next();
            background.advance(bytes);
            if (background.finished()) {
                iterator.remove();
            }
        }
    }

    private static int readLittleEndian16(byte[] bytes, int offset) {
        int low = bytes[offset] & 0xff;
        int high = bytes[offset + 1];
        return (short) ((high << 8) | low);
    }

    private static void writeLittleEndian16(byte[] bytes, int offset, int sample) {
        bytes[offset] = (byte) (sample & 0xff);
        bytes[offset + 1] = (byte) ((sample >>> 8) & 0xff);
    }

    private static int clampToInt16(int sample) {
        return Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, sample));
    }

    private static final class ActiveBackground {
        private final byte[] bytes;
        private int offset;

        private ActiveBackground(byte[] bytes) {
            this.bytes = bytes;
        }

        private byte[] bytes() {
            return bytes;
        }

        private int offset() {
            return offset;
        }

        private boolean hasBytes(int relativeOffset) {
            return offset + relativeOffset < bytes.length;
        }

        private void advance(int bytesWritten) {
            offset += bytesWritten;
        }

        private boolean finished() {
            return offset >= bytes.length;
        }
    }
}
