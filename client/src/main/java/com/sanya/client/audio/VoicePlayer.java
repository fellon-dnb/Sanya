package com.sanya.client.audio;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;

public class VoicePlayer {

    private final byte[] audioData;

    public VoicePlayer(byte[] audioData) {
        this.audioData = audioData;
    }

    public void play() {
        try (AudioInputStream ais = new AudioInputStream(
                new ByteArrayInputStream(audioData),
                new AudioFormat(16000.0f, 16, 1, true, false),
                audioData.length / 2)) {

            DataLine.Info info = new DataLine.Info(SourceDataLine.class, ais.getFormat());
            try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
                line.open(ais.getFormat());
                line.start();

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = ais.read(buffer)) != -1) {
                    line.write(buffer, 0, bytesRead);
                }

                line.drain();
                line.stop();
            }

        } catch (Exception e) {
            System.err.println("Ошибка воспроизведения: " + e.getMessage());
        }
    }
}
