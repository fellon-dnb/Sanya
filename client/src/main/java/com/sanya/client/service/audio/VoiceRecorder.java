package com.sanya.client.service.audio;

import com.sanya.client.ApplicationContext;
import com.sanya.events.system.SystemMessageEvent;
import com.sanya.events.voice.VoiceLevelEvent;
import com.sanya.events.voice.VoiceRecordingStoppedEvent;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sanya.client.service.audio.AudioConfig.getFormat;

/**
 * VoiceRecorder — модуль записи голоса с микрофона.
 * Публикует события уровня сигнала (VU meter) и завершения записи.
 *
 * Назначение:
 *  - Захватывать аудиопоток с микрофона в формате PCM.
 *  - Передавать уровень громкости для визуализации в UI.
 *  - Отправлять готовые аудиоданные при остановке.
 *  - Ограничивать продолжительность записи (по умолчанию 1 минута).
 *
 * Использование:
 *  VoiceRecorder recorder = new VoiceRecorder(ctx);
 *  recorder.start(); // начало записи
 *  recorder.stop();  // завершение
 */
public final class VoiceRecorder implements Runnable {

    private static final Logger log = Logger.getLogger(VoiceRecorder.class.getName());
    private static final long MAX_DURATION_MS = 60000; // максимум 1 минута
    private static final int BUFFER_SIZE = 4096;

    private final ApplicationContext ctx;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread thread;

    public VoiceRecorder(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    /** Запускает запись, предотвращая повторные вызовы. */
    public void start() {
        if (running.get()) {
            log.warning("[VoiceRecorder] Already running — ignored start");
            return;
        }
        running.set(true);
        thread = new Thread(this, "VoiceRecorder");
        thread.start();
        log.info("[VoiceRecorder] Recording thread started");
    }

    /** Останавливает запись. */
    public void stop() {
        if (!running.get()) return;
        running.set(false);
        log.info("[VoiceRecorder] Stop requested");
    }

    /** Проверяет, активна ли запись. */
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
            log.severe("[VoiceRecorder] Audio input not supported: " + format);
            return;
        }

        try (TargetDataLine mic = (TargetDataLine) AudioSystem.getLine(info);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            mic.open(format);
            mic.start();

            byte[] buf = new byte[BUFFER_SIZE];
            long startTime = System.currentTimeMillis();
            long lastLevelUpdate = 0;

            log.info("[VoiceRecorder] Recording started");

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
                    log.warning("[VoiceRecorder] Max duration reached");
                    break;
                }
            }

            mic.stop();
            running.set(false);

            byte[] audio = baos.toByteArray();
            ctx.getEventBus().publish(
                    new VoiceRecordingStoppedEvent(ctx.getUserSettings().getName(), audio)
            );
            log.info("[VoiceRecorder] Recording finished, bytes: " + audio.length);

        } catch (LineUnavailableException e) {
            log.log(Level.SEVERE, "[VoiceRecorder] Audio line unavailable", e);
            ctx.getEventBus().publish(new SystemMessageEvent("[ERROR] Микрофон недоступен: " + e.getMessage()));
        } catch (Exception e) {
            log.log(Level.SEVERE, "[VoiceRecorder] Recording error", e);
            ctx.getEventBus().publish(new SystemMessageEvent("[ERROR] Ошибка записи звука: " + e.getMessage()));
        } finally {
            running.set(false);
        }
    }

    /**
     * Вычисляет RMS (Root Mean Square) значение для оценки громкости.
     *
     * @param data буфер с PCM-данными
     * @param len  количество байт для анализа
     * @return нормализованный уровень громкости от 0.0 до 1.0
     */
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
