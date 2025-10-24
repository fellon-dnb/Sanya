package com.sanya.client.service.audio;

import javax.sound.sampled.AudioFormat;

/**
 * AudioConfig — набор параметров аудиозахвата и воспроизведения.
 * Определяет единый формат звука для записи и передачи голосовых сообщений.
 *
 * Назначение:
 *  - Обеспечить согласованность формата аудио между клиентами.
 *  - Использоваться в {@link javax.sound.sampled.TargetDataLine} и {@link javax.sound.sampled.SourceDataLine}.
 *
 * Формат:
 *  - 16 бит
 *  - 1 канал (моно)
 *  - 44.1 кГц
 *  - PCM_SIGNED (малоэндиановый)
 */
public final class AudioConfig {

    private AudioConfig() {}

    /**
     * Возвращает стандартный аудиоформат для голосовой связи.
     *
     * @return объект {@link AudioFormat} (PCM_SIGNED, 16 бит, 44.1 кГц, mono)
     */
    public static AudioFormat getFormat() {
        return new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                44100.0f,   // частота дискретизации
                16,         // глубина (бит на сэмпл)
                1,          // количество каналов
                2,          // размер кадра (байт)
                44100.0f,   // частота кадров
                false       // порядок байтов — little endian
        );
    }
}
