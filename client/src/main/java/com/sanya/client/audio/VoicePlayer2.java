package com.sanya.client.audio;

import javax.sound.sampled.*;

public class VoicePlayer2 {

    private static final AudioFormat FORMAT =
            new AudioFormat(16000.0f, 16, 1, true, false);

    public static void play(byte[] data) {
        try (SourceDataLine line = AudioSystem.getSourceDataLine(FORMAT)) {
            line.open(FORMAT);
            line.start();
            line.write(data, 0, data.length);
            line.drain();
        } catch (LineUnavailableException e) {
            System.err.println("[ERROR] Voice playback failed: " + e.getMessage());
        }
    }
}
