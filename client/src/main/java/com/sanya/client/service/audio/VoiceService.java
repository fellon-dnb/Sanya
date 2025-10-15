package com.sanya.client.service.audio;

import com.sanya.client.ApplicationContext;
import com.sanya.client.audio.VoicePlayer;
import com.sanya.client.audio.VoiceRecorder;
import com.sanya.events.SystemMessageEvent;
import com.sanya.events.VoiceMessageReadyEvent;
import com.sanya.events.VoiceRecordingEvent;

public class VoiceService {

    private final ApplicationContext ctx;
    private VoiceRecorder recorder;
    private boolean recording;
    private boolean sending; // Флаг для предотвращения множественной отправки

    public VoiceService(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    public void startRecording() {
        if (recording) return;
        recording = true;
        sending = false; // Сбрасываем флаг отправки
        recorder = new VoiceRecorder(ctx);
        ctx.getEventBus().publish(new VoiceRecordingEvent(ctx.getUserSettings().getName(), true));
        new Thread(recorder, "VoiceRecorder").start();
    }

    public void stopRecording() {
        if (!recording) return;
        recording = false;
        if (recorder != null) {
            recorder.stop();
        }
        ctx.getEventBus().publish(new VoiceRecordingEvent(ctx.getUserSettings().getName(), false));
    }

    public void playTemp(byte[] data) {
        new Thread(() -> new VoicePlayer(data).play()).start();
    }

    public void sendVoice(byte[] data) {
        // Защита от множественной отправки
        if (sending) {
            return;
        }
        sending = true;

        try {
            var username = ctx.getUserSettings().getName();

            // Только отправка на сервер, без локальных событий
            var out = ctx.services().chat().getOutputStream();
            synchronized (out) {
                out.writeObject(new VoiceMessageReadyEvent(username, data));
                out.flush();
            }
        } catch (Exception e) {
            ctx.getEventBus().publish(new SystemMessageEvent("[ERROR] Отправка голосового сообщения: " + e.getMessage()));
        } finally {
            // Сбрасываем флаг после небольшой задержки
            new Thread(() -> {
                try {
                    Thread.sleep(1000); // Задержка 1 секунда
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                sending = false;
            }).start();
        }
    }

    public boolean isSending() {
        return sending;
    }
}