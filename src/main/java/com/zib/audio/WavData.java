package com.zib.audio;

import javax.sound.sampled.AudioFormat;

record WavData(byte[] pcmBytes, AudioFormat format) {
}
