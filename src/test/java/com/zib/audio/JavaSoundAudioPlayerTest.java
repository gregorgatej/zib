package com.zib.audio;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sound.sampled.AudioFormat;

import org.junit.jupiter.api.Test;

class JavaSoundAudioPlayerTest {
    private static final AudioFormat FORMAT = JavaSoundAudioPlayer.CANONICAL_FORMAT;

    @Test
    void speechOnlyWritesToOneOutputLineAndCloses() {
        FakeWavDecoder decoder = new FakeWavDecoder();
        FakeAudioOutputLine outputLine = new FakeAudioOutputLine();
        decoder.add("segment-001.wav", pcm(100, -100, 200, -200));
        JavaSoundAudioPlayer player = new JavaSoundAudioPlayer(decoder, outputLine, 1);

        player.playBlocking(Path.of("segment-001.wav"));
        player.close();

        assertEquals(1, outputLine.openCount());
        assertEquals(1, outputLine.closeCount());
        assertEquals(1, outputLine.drainCount());
        assertArrayEquals(pcm(100, -100, 200, -200), outputLine.allWrittenBytes());
    }

    @Test
    void backgroundBeforeSpeechReturnsWithoutWritingUntilSpeechStarts() {
        FakeWavDecoder decoder = new FakeWavDecoder();
        FakeAudioOutputLine outputLine = new FakeAudioOutputLine();
        decoder.add("effect.wav", pcm(10, 20));
        decoder.add("segment-001.wav", pcm(100, 200));
        JavaSoundAudioPlayer player = new JavaSoundAudioPlayer(decoder, outputLine, 1);

        player.playInBackground(Path.of("effect.wav"));

        assertEquals(0, outputLine.writeCount());

        player.playBlocking(Path.of("segment-001.wav"));
        player.close();

        assertArrayEquals(pcm(110, 220), outputLine.allWrittenBytes());
    }

    @Test
    void demoOrderMixesEffectIntoSecondSpeechThroughSingleOutputLine() {
        FakeWavDecoder decoder = new FakeWavDecoder();
        FakeAudioOutputLine outputLine = new FakeAudioOutputLine();
        decoder.add("segment-001.wav", pcm(100, 100));
        decoder.add("children_laughing.wav", pcm(10, 20));
        decoder.add("segment-002.wav", pcm(1000, 2000));
        JavaSoundAudioPlayer player = new JavaSoundAudioPlayer(decoder, outputLine, 1);

        player.playBlocking(Path.of("segment-001.wav"));
        player.playInBackground(Path.of("children_laughing.wav"));
        player.playBlocking(Path.of("segment-002.wav"));
        player.close();

        assertEquals(1, outputLine.openCount());
        assertArrayEquals(pcm(100, 100, 1010, 2020), outputLine.allWrittenBytes());
    }

    @Test
    void backgroundLongerThanFinalSpeechIsCutOff() {
        FakeWavDecoder decoder = new FakeWavDecoder();
        FakeAudioOutputLine outputLine = new FakeAudioOutputLine();
        decoder.add("effect.wav", pcm(10, 20, 30, 40));
        decoder.add("segment-001.wav", pcm(100, 200));
        JavaSoundAudioPlayer player = new JavaSoundAudioPlayer(decoder, outputLine, 1);

        player.playInBackground(Path.of("effect.wav"));
        player.playBlocking(Path.of("segment-001.wav"));
        player.close();

        assertArrayEquals(pcm(110, 220), outputLine.allWrittenBytes());
    }

    @Test
    void mixingClampsToSixteenBitRange() {
        FakeWavDecoder decoder = new FakeWavDecoder();
        FakeAudioOutputLine outputLine = new FakeAudioOutputLine();
        decoder.add("effect.wav", pcm(1000, -1000));
        decoder.add("segment-001.wav", pcm(32_000, -32_000));
        JavaSoundAudioPlayer player = new JavaSoundAudioPlayer(decoder, outputLine, 1);

        player.playInBackground(Path.of("effect.wav"));
        player.playBlocking(Path.of("segment-001.wav"));
        player.close();

        assertArrayEquals(pcm(Short.MAX_VALUE, Short.MIN_VALUE), outputLine.allWrittenBytes());
    }

    private static byte[] pcm(int... samples) {
        byte[] bytes = new byte[samples.length * 2];
        for (int index = 0; index < samples.length; index++) {
            int sample = samples[index];
            bytes[index * 2] = (byte) (sample & 0xff);
            bytes[index * 2 + 1] = (byte) ((sample >>> 8) & 0xff);
        }
        return bytes;
    }

    private static final class FakeWavDecoder implements WavDecoder {
        private final Map<Path, byte[]> files = new HashMap<>();

        private void add(String path, byte[] pcmBytes) {
            files.put(Path.of(path), pcmBytes);
        }

        @Override
        public WavData decode(Path wavFile) {
            return new WavData(files.get(wavFile), FORMAT);
        }
    }

    private static final class FakeAudioOutputLine implements AudioOutputLine {
        private final List<byte[]> writes = new ArrayList<>();
        private int openCount;
        private int drainCount;
        private int closeCount;

        @Override
        public void open(AudioFormat format) {
            assertEquals(FORMAT, format);
            openCount++;
        }

        @Override
        public void write(byte[] bytes, int offset, int length) {
            byte[] copy = new byte[length];
            System.arraycopy(bytes, offset, copy, 0, length);
            writes.add(copy);
        }

        @Override
        public void drain() {
            drainCount++;
        }

        @Override
        public void close() {
            closeCount++;
        }

        private int openCount() {
            return openCount;
        }

        private int writeCount() {
            return writes.size();
        }

        private int drainCount() {
            return drainCount;
        }

        private int closeCount() {
            return closeCount;
        }

        private byte[] allWrittenBytes() {
            int length = writes.stream().mapToInt(bytes -> bytes.length).sum();
            byte[] allBytes = new byte[length];
            int offset = 0;
            for (byte[] write : writes) {
                System.arraycopy(write, 0, allBytes, offset, write.length);
                offset += write.length;
            }
            return allBytes;
        }
    }
}
