package com.sanya.client.service.audio;

import javax.sound.sampled.*;

public class SoundPlayer {

    public static void playMessageSound() {
        playBeep(700, 150);
    }

    public static void playSystemSound() {
        playBeep(500, 120);
    }

    public static void playErrorSound() {
        playBeep(200, 200);
    }

    private static void playBeep(int hz, int msecs) {
        float SAMPLE_RATE = 8000f;
        byte[] buf = new byte[(int) SAMPLE_RATE * msecs / 1000];
        for (int i = 0; i < buf.length; i++) {
            double angle = i / (SAMPLE_RATE / hz) * 2.0 * Math.PI;
            buf[i] = (byte)(Math.sin(angle) * 100);
        }

        try (SourceDataLine sdl = AudioSystem.getSourceDataLine(
                new AudioFormat(SAMPLE_RATE, 8, 1, true, false))) {
            sdl.open();
            sdl.start();
            sdl.write(buf, 0, buf.length);
            sdl.drain();
            sdl.stop();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }
}
