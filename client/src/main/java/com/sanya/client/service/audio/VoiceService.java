package com.sanya.client.service.audio;

import com.sanya.client.ApplicationContext;
import com.sanya.client.audio.VoicePlayer;
import com.sanya.client.audio.VoiceRecorder;
import com.sanya.events.SystemMessageEvent;
import com.sanya.events.VoiceRecordingEvent;

public class VoiceService {
    private final ApplicationContext ctx;
    private VoiceRecorder recorder;
    private boolean recording;

    public VoiceService(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    public void startRecording() {
        if (recording) return;
        recording = true;
        recorder = new VoiceRecorder(ctx);
        new Thread(recorder, "VoiceRecorder").start();
        ctx.getEventBus().publish(new VoiceRecordingEvent(ctx.getUserSettings().getName(), true));
    }

    public void stopRecording() {
        if (!recording) return;
        recording = false;
        if (recorder != null) recorder.stop();
        ctx.getEventBus().publish(new VoiceRecordingEvent(ctx.getUserSettings().getName(), false));
    }

    public void playTemp(byte[] data) {
        new Thread(() -> new VoicePlayer(data).play()).start();
    }

    public void sendVoice(byte[] data) {
        try {
            var out = ctx.services().chat().getOutputStream();
            var req = new com.sanya.files.FileTransferRequest(ctx.getUserSettings().getName(), "voice", data.length);
            out.writeObject(req);
            out.writeObject(new com.sanya.files.FileChunk("voice", data, 0, true));
            out.flush();
        } catch (Exception e) {
            ctx.getEventBus().publish(
                    new SystemMessageEvent("[ERROR] Ошибка отправки голосового сообщения: " + e.getMessage())
            );
        }
    }
}
