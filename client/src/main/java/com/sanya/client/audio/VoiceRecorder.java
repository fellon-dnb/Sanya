package com.sanya.client.audio;

import com.sanya.client.ApplicationContext;
import com.sanya.events.VoiceRecordingStoppedEvent;
import com.sanya.events.EventBus;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class VoiceRecorder implements Runnable {

    private final ApplicationContext ctx;
    private final EventBus eventBus;
    private volatile boolean running = true;

    public VoiceRecorder(ApplicationContext ctx) {
        this.ctx = ctx;
        this.eventBus = ctx.getEventBus();
    }

    @Override
    public void run() {
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            System.out.println("Mixer: " + info.getName() + " - " + info.getDescription());
        }
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        System.out.println("=== AVAILABLE AUDIO MIXERS ===");
        for (Mixer.Info info : mixers) {
            System.out.println("Mixer: " + info.getName() + " - " + info.getDescription());
        }

        AudioFormat format = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                44100.0f,
                16,
                2,
                4,
                44100.0f,
                false
        );

        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            System.err.println("Аудиолиния не поддерживается!");
            return;
        }

        try (TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info)) {
            line.open(format);
            line.start();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];

            System.out.println("🎤 Recording started...");

            while (running) {
                int count = line.read(buffer, 0, buffer.length);
                if (count > 0) out.write(buffer, 0, count);
            }

            line.stop();
            line.close();

            byte[] audioData = out.toByteArray();
            eventBus.publish(new VoiceRecordingStoppedEvent(ctx.getUsername(), audioData));

            System.out.println("🎤 Recording stopped, bytes: " + audioData.length);

        } catch (LineUnavailableException e) {
            System.err.println("Не удалось открыть микрофон: " + e.getMessage());
        }
    }

    public void stop() {
        running = false;
    }
}
