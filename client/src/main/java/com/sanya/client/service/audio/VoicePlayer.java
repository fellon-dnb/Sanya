package com.sanya.client.service.audio;

import javax.sound.sampled.*;

import static com.sanya.client.service.audio.AudioConfig.getFormat;

/**
 * VoicePlayer — проигрыватель голосовых сообщений.
 * Воспроизводит переданные аудиоданные в стандартном формате PCM.
 *
 * Назначение:
 *  - Воспроизводить записанные байты голосовых сообщений без искажений.
 *  - Использует параметры, заданные в {@link AudioConfig}.
 *
 * Использование:
 *  new VoicePlayer(data).play();
 */
public final class VoicePlayer {

    /** Массив байтов PCM-аудио. */
    private final byte[] data;

    /** Создаёт новый проигрыватель для заданных данных. */
    public VoicePlayer(byte[] data) {
        this.data = data;
    }

    /**
     * Воспроизводит переданный аудиопоток с корректным тембром и скоростью.
     */
    public void play() {
        AudioFormat format = getFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

        try (SourceDataLine speakers = (SourceDataLine) AudioSystem.getLine(info)) {
            speakers.open(format);
            speakers.start();

            speakers.write(data, 0, data.length);
            speakers.drain();
            speakers.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
