package com.sanya.client.service.audio;

import javax.sound.sampled.*;

/**
 * SoundPlayer — утилита для воспроизведения простых системных звуков.
 * Используется для уведомлений о сообщениях, ошибках и системных событиях.
 *
 * Назначение:
 *  - Проигрывать короткие звуковые сигналы разной частоты.
 *  - Работает без внешних звуковых файлов, генерируя синусоидальный сигнал.
 *
 * Использование:
 *  SoundPlayer.playMessageSound(); // звук входящего сообщения
 *  SoundPlayer.playErrorSound();   // звук ошибки
 */
public final class SoundPlayer {

    private SoundPlayer() {}

    /** Воспроизводит звук входящего сообщения. */
    public static void playMessageSound() {
        playBeep(700, 150);
    }

    /** Воспроизводит системный звук уведомления. */
    public static void playSystemSound() {
        playBeep(500, 120);
    }

    /** Воспроизводит звук ошибки. */
    public static void playErrorSound() {
        playBeep(200, 200);
    }

    /**
     * Генерирует и воспроизводит короткий звуковой сигнал.
     *
     * @param hz    частота сигнала (Гц)
     * @param msecs длительность сигнала (мс)
     */
    private static void playBeep(int hz, int msecs) {
        final float SAMPLE_RATE = 8000f;
        byte[] buf = new byte[(int) SAMPLE_RATE * msecs / 1000];
        for (int i = 0; i < buf.length; i++) {
            double angle = i / (SAMPLE_RATE / hz) * 2.0 * Math.PI;
            buf[i] = (byte) (Math.sin(angle) * 100);
        }

        AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);

        try (SourceDataLine sdl = AudioSystem.getSourceDataLine(format)) {
            sdl.open(format);
            sdl.start();
            sdl.write(buf, 0, buf.length);
            sdl.drain();
            sdl.stop();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }
}
