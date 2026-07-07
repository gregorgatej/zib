package com.zib.runtime;

import java.util.List;

import com.zib.audio.AudioPlayer;

public final class PlaybackOrchestrator {
    private final AudioPlayer audioPlayer;

    public PlaybackOrchestrator(AudioPlayer audioPlayer) {
        this.audioPlayer = audioPlayer;
    }

    public void play(List<PlaybackEvent> playbackEvents) {
        try {
            for (PlaybackEvent event : playbackEvents) {
                if (event instanceof PlaybackEvent.Speech speech) {
                    audioPlayer.playBlocking(speech.wavFile());
                } else if (event instanceof PlaybackEvent.Sound sound) {
                    audioPlayer.playInBackground(sound.wavFile());
                }
            }
        } finally {
            audioPlayer.close();
        }
    }
}
