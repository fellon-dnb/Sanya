package com.sanya.client.audio;

import com.sanya.client.ApplicationContext;
import com.sanya.events.VoiceLevelEvent;
import com.sanya.events.VoiceRecordingStoppedEvent;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import static com.sanya.client.audio.AudioConfig.getFormat;

/**
 * Записывает голос, публикует громкость (VU meter) и событие об окончании записи.
 */
public class VoiceRecorder implements Runnable {

    private final ApplicationContext ctx;
    private volatile boolean running = true;

    public VoiceRecorder(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        AudioFormat format = getFormat();
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        try (TargetDataLine mic = (TargetDataLine) AudioSystem.getLine(info);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            mic.open(format);
            mic.start();

            byte[] buf = new byte[4096];
            long lastLevelUpdate = 0;

            while (running) {
                int bytes = mic.read(buf, 0, buf.length);
                if (bytes > 0) {
                    baos.write(buf, 0, bytes);

                    long now = System.currentTimeMillis();
                    if (now - lastLevelUpdate > 100) {
                        double rms = calcRMS(buf, bytes);
                        ctx.getEventBus().publish(new VoiceLevelEvent(rms));
                        lastLevelUpdate = now;
                    }
                }
            }

            mic.stop();
            mic.close();

            byte[] audio = baos.toByteArray();
            ctx.getEventBus().publish(
                    new VoiceRecordingStoppedEvent(ctx.getUserSettings().getName(), audio)
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private double calcRMS(byte[] data, int len) {
        long sum = 0;
        for (int i = 0; i < len; i += 2) {
            short sample = (short) ((data[i + 1] << 8) | (data[i] & 0xff));
            sum += sample * sample;
        }
        double mean = sum / (len / 2.0);
        return Math.sqrt(mean) / 32768.0; // нормализуем 0..1
    }
}
