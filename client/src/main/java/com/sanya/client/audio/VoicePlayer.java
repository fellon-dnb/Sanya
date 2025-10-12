package com.sanya.client.audio;

import javax.sound.sampled.*;

import static com.sanya.client.audio.AudioConfig.getFormat;

/**
 * Проигрывает записанные байты с корректным тембром и скоростью.
 */
public class VoicePlayer {

    private final byte[] data;

    public VoicePlayer(byte[] data) {
        this.data = data;
    }

    public void play() {
        AudioFormat format = getFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

        try (SourceDataLine speakers = (SourceDataLine) AudioSystem.getLine(info)) {
            speakers.open(format);
            speakers.start();

            speakers.write(data, 0, data.length);
            speakers.drain();
            speakers.stop();
            speakers.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
