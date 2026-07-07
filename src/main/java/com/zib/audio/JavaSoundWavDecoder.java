package com.zib.audio;

import java.io.IOException;
import java.nio.file.Path;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.zib.error.ZibException;

final class JavaSoundWavDecoder implements WavDecoder {
    private final AudioFormat targetFormat;

    JavaSoundWavDecoder(AudioFormat targetFormat) {
        this.targetFormat = targetFormat;
    }

    @Override
    public WavData decode(Path wavFile) {
        try (AudioInputStream sourceStream = AudioSystem.getAudioInputStream(wavFile.toFile());
                AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormat, sourceStream)) {
            return new WavData(convertedStream.readAllBytes(), targetFormat);
        } catch (UnsupportedAudioFileException exception) {
            throw new ZibException("Unsupported WAV file: " + wavFile, exception);
        } catch (IllegalArgumentException exception) {
            throw new ZibException("Unsupported WAV conversion for file: " + wavFile, exception);
        } catch (IOException exception) {
            throw new ZibException("Failed to read WAV file: " + wavFile, exception);
        }
    }
}
