package com.zib.audio;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.zib.error.ZibException;

public final class JavaSoundAudioPlayer implements AudioPlayer {
    @Override
    public void playBlocking(Path wavFile) {
        CountDownLatch finished = new CountDownLatch(1);
        Clip clip = openClip(wavFile);
        clip.addLineListener(event -> {
            if (event.getType() == LineEvent.Type.STOP || event.getType() == LineEvent.Type.CLOSE) {
                finished.countDown();
            }
        });

        try {
            clip.start();
            finished.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ZibException("Interrupted while playing WAV file: " + wavFile, exception);
        } finally {
            clip.close();
        }
    }

    @Override
    public void playInBackground(Path wavFile) {
        Clip clip = openClip(wavFile);
        clip.addLineListener(event -> {
            if (event.getType() == LineEvent.Type.STOP) {
                clip.close();
            }
        });
        clip.start();
    }

    private static Clip openClip(Path wavFile) {
        try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(wavFile.toFile())) {
            Clip clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            return clip;
        } catch (UnsupportedAudioFileException exception) {
            throw new ZibException("Unsupported WAV file: " + wavFile, exception);
        } catch (LineUnavailableException exception) {
            throw new ZibException("Audio line unavailable for WAV file: " + wavFile, exception);
        } catch (IOException exception) {
            throw new ZibException("Failed to read WAV file: " + wavFile, exception);
        }
    }
}
