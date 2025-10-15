package com.sanya.client.service.audio;

import javax.sound.sampled.AudioFormat;

public final class AudioConfig {

    private AudioConfig() {}

    public static AudioFormat getFormat() {
        // 16 бит, 1 канал, 44.1 кГц, PCM_SIGNED — стандартное качество
        return new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                44100.0f,   // sample rate
                16,         // sample size in bits
                1,          // channels (mono)
                2,          // frame size (2 bytes = 16 бит)
                44100.0f,   // frame rate
                false       // little endian
        );
    }
}
