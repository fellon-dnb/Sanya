package com.sanya.client.service.audio;

import com.sanya.client.ApplicationContext;
import com.sanya.events.system.SystemMessageEvent;
import com.sanya.events.voice.VoiceMessageReadyEvent;
import com.sanya.events.voice.VoiceRecordingEvent;

public class VoiceService {

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
        sending = false; // сбрасываем флаг отправки

        // Проверяем, есть ли активный рекордер
        if (recorder == null || !recorder.isRunning()) {
            recorder = new VoiceRecorder(ctx);
            recorder.start();
        }

        ctx.getEventBus().publish(
                new VoiceRecordingEvent(ctx.getUserSettings().getName(), true)
        );
    }

    /** Остановить запись */
    public void stopRecording() {
        if (!recording) return;
        recording = false;

        if (recorder != null) {
            recorder.stop();
        }

        ctx.getEventBus().publish(
                new VoiceRecordingEvent(ctx.getUserSettings().getName(), false)
        );
    }

    /** Воспроизвести временную запись (локально) */
    public void playTemp(byte[] data) {
        new Thread(() -> new VoicePlayer(data).play(), "VoicePlayerTemp").start();
    }

    /** Отправить голосовое сообщение на сервер */
    public void sendVoice(byte[] data) {
        if (sending) return; // защита от повторной отправки
        sending = true;

        try {
            var username = ctx.getUserSettings().getName();

            // просто шлём объект через ChatService
            ctx.services().chat().sendObject(
                    new VoiceMessageReadyEvent(username, data)
            );

        } catch (Exception e) {
            ctx.getEventBus().publish(
                    new SystemMessageEvent("[ERROR] Отправка голосового сообщения: " + e.getMessage())
            );
        } finally {
            // сброс флага через 1 секунду
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
