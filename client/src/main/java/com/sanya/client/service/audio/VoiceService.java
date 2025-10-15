package com.sanya.client.service.audio;

import com.sanya.client.ApplicationContext;
import com.sanya.events.system.SystemMessageEvent;
import com.sanya.events.voice.VoiceMessageReadyEvent;
import com.sanya.events.voice.VoiceRecordingEvent;

import java.util.logging.Level;
import java.util.logging.Logger;

public class VoiceService {

    private static final Logger log = Logger.getLogger(VoiceService.class.getName());

    private final ApplicationContext ctx;
    private VoiceRecorder recorder;
    private boolean recording;
    private boolean sending; // защита от множественной отправки

    public VoiceService(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    /** Запустить запись, если она не активна */
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

    /** Остановить запись */
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

    /** Воспроизвести временную запись (локально) */
    public void playTemp(byte[] data) {
        new Thread(() -> {
            log.info("Playing temporary voice message");
            new VoicePlayer(data).play();
        }, "VoicePlayerTemp").start();
    }

    /** Отправить голосовое сообщение на сервер */
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
                    "[ERROR] Отправка голосового сообщения: " + e.getMessage()
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

    public boolean isSending() {
        return sending;
    }
}
