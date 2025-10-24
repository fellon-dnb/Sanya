package com.sanya.client.service.audio;

import com.sanya.client.ApplicationContext;
import com.sanya.events.system.SystemMessageEvent;
import com.sanya.events.voice.VoiceMessageReadyEvent;
import com.sanya.events.voice.VoiceRecordingEvent;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * VoiceService — основной сервис для работы с голосовыми сообщениями.
 * Управляет записью, воспроизведением и отправкой аудиоданных.
 *
 * Назначение:
 *  - Контролировать жизненный цикл записи через {@link VoiceRecorder}.
 *  - Воспроизводить локальные записи через {@link VoicePlayer}.
 *  - Отправлять готовые голосовые сообщения на сервер.
 *  - Предотвращать повторную отправку во время активной операции.
 *
 * Использование:
 *  VoiceService voice = ctx.services().voice();
 *  voice.startRecording();
 *  voice.stopRecording();
 *  voice.sendVoice(data);
 */
public final class VoiceService {

    private static final Logger log = Logger.getLogger(VoiceService.class.getName());

    private final ApplicationContext ctx;
    private VoiceRecorder recorder;
    private boolean recording;
    private boolean sending;

    public VoiceService(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    /** Запускает запись с микрофона, если она ещё не активна. */
    public void startRecording() {
        if (recording) return;

        recording = true;
        sending = false;

        if (recorder == null || !recorder.isRunning()) {
            recorder = new VoiceRecorder(ctx);
            recorder.start();
            log.info("VoiceRecorder started");
        }

        ctx.getEventBus().publish(new VoiceRecordingEvent(ctx.getUserSettings().getName(), true));
        log.fine("Voice recording started");
    }

    /** Останавливает запись с микрофона. */
    public void stopRecording() {
        if (!recording) return;
        recording = false;

        if (recorder != null) {
            recorder.stop();
            log.info("VoiceRecorder stopped");
        }

        ctx.getEventBus().publish(new VoiceRecordingEvent(ctx.getUserSettings().getName(), false));
        log.fine("Voice recording stopped");
    }

    /** Воспроизводит локально записанное голосовое сообщение. */
    public void playTemp(byte[] data) {
        new Thread(() -> {
            log.info("Playing temporary voice message");
            new VoicePlayer(data).play();
        }, "VoicePlayerTemp").start();
    }

    /** Отправляет готовое голосовое сообщение через чат-сервис. */
    public void sendVoice(byte[] data) {
        if (sending) return;
        sending = true;

        try {
            var username = ctx.getUserSettings().getName();
            ctx.services().chat().sendObject(new VoiceMessageReadyEvent(username, data));
            log.info("Voice message sent by user: " + username);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to send voice message", e);
            ctx.getEventBus().publish(new SystemMessageEvent(
                    "[ERROR] Ошибка при отправке голосового сообщения: " + e.getMessage()
            ));
        } finally {
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                sending = false;
            }, "VoiceSendReset").start();
        }
    }

    /** Проверяет, выполняется ли сейчас отправка. */
    public boolean isSending() {
        return sending;
    }
}
