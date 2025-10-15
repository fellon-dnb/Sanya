package com.sanya.client.service.audio;

import com.sanya.client.ApplicationContext;
import com.sanya.events.system.SystemMessageEvent;
import com.sanya.events.voice.VoiceLevelEvent;
import com.sanya.events.voice.VoiceRecordingStoppedEvent;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.sanya.client.service.audio.AudioConfig.getFormat;

/**
 * Захват звука с микрофона, публикация уровня сигнала (VU meter)
 * и события об окончании записи. С защитой от повторного запуска и лимитом по времени.
 */
public class VoiceRecorder implements Runnable {

    private static final long MAX_DURATION_MS = 60000; // максимум 1 минута
    private static final int BUFFER_SIZE = 4096;

    private final ApplicationContext ctx;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread thread;

    public VoiceRecorder(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    /** Запустить запись (игнорирует повторные вызовы) */
    public void start() {
        if (running.get()) {
            System.out.println("[VoiceRecorder] Already running — ignored start");
            return;
        }
        running.set(true);
        thread = new Thread(this, "VoiceRecorder");
        thread.start();
    }

    /** Остановить запись */
    public void stop() {
        if (!running.get()) return;
        running.set(false);
    }

    /** Проверка состояния */
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public void run() {
        AudioFormat format = getFormat();
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            ctx.getEventBus().publish(new SystemMessageEvent(
                    "[ERROR] Аудио вход не поддерживается: " + format));
            return;
        }

        try (TargetDataLine mic = (TargetDataLine) AudioSystem.getLine(info);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            mic.open(format);
            mic.start();

            byte[] buf = new byte[BUFFER_SIZE];
            long startTime = System.currentTimeMillis();
            long lastLevelUpdate = 0;

            System.out.println("[VoiceRecorder] Recording started");

            while (running.get()) {
                int bytes = mic.read(buf, 0, buf.length);
                if (bytes <= 0) continue;

                baos.write(buf, 0, bytes);

                long now = System.currentTimeMillis();
                if (now - lastLevelUpdate > 100) {
                    double rms = calcRMS(buf, bytes);
                    ctx.getEventBus().publish(new VoiceLevelEvent(rms));
                    lastLevelUpdate = now;
                }

                if (now - startTime > MAX_DURATION_MS) {
                    System.out.println("[VoiceRecorder] Max duration reached");
                    break;
                }
            }

            mic.stop();
            running.set(false);

            byte[] audio = baos.toByteArray();
            ctx.getEventBus().publish(
                    new VoiceRecordingStoppedEvent(ctx.getUserSettings().getName(), audio)
            );

            System.out.println("[VoiceRecorder] Recording finished, bytes: " + audio.length);

        } catch (LineUnavailableException e) {
            ctx.getEventBus().publish(new SystemMessageEvent("[ERROR] Микрофон недоступен: " + e.getMessage()));
        } catch (Exception e) {
            ctx.getEventBus().publish(new SystemMessageEvent("[ERROR] Ошибка записи звука: " + e.getMessage()));
            e.printStackTrace();
        } finally {
            running.set(false);
        }
    }

    /** RMS (Root Mean Square) вычисление для визуализации громкости */
    private double calcRMS(byte[] data, int len) {
        long sum = 0;
        for (int i = 0; i < len - 1; i += 2) {
            short sample = (short) ((data[i + 1] << 8) | (data[i] & 0xFF));
            sum += sample * sample;
        }
        double mean = sum / (len / 2.0);
        return Math.min(Math.sqrt(mean) / 32768.0, 1.0);
    }
}
