package com.sanya.client.ui.dialog;

import com.sanya.client.service.audio.VoiceService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * {@code ChatVoiceDialog} — модальное диалоговое окно для управления голосовыми сообщениями.
 * Пользователь может прослушать записанное сообщение, отправить его или отменить действие.
 *
 * <p>Диалог служит связующим звеном между пользовательским интерфейсом и {@link VoiceService},
 * предоставляя безопасный способ подтверждения перед отправкой аудиофайла.</p>
 *
 * <h3>Функции:</h3>
 * <ul>
 *     <li>Отображение состояния записи ("готово к отправке").</li>
 *     <li>Возможность локального прослушивания перед отправкой.</li>
 *     <li>Асинхронная отправка аудиосообщения через {@link VoiceService#sendVoice(byte[])}.</li>
 *     <li>Безопасное закрытие и очистка состояния.</li>
 * </ul>
 *
 * <h3>Потоковая модель:</h3>
 * <ul>
 *     <li>Все операции с UI выполняются в EDT (Event Dispatch Thread).</li>
 *     <li>Отправка аудиоданных выполняется в отдельном потоке ("VoiceSender").</li>
 * </ul>
 *
 * <h3>Пример использования:</h3>
 * <pre>{@code
 * byte[] voiceData = ...;
 * VoiceService voiceService = ctx.services().voice();
 * ChatVoiceDialog dialog = new ChatVoiceDialog(parentFrame, voiceData, voiceService);
 * dialog.setVisible(true);
 * }</pre>
 */
public final class ChatVoiceDialog extends JDialog {

    /** Массив байт голосового сообщения, подготовленного к отправке. */
    private final byte[] data;

    /** Сервис, управляющий воспроизведением и отправкой голосовых сообщений. */
    private final VoiceService service;

    /** Кнопка для отправки голосового сообщения. */
    private JButton sendButton;

    /** Флаг, предотвращающий повторную отправку. */
    private boolean sent = false;

    /**
     * Создаёт диалог для предварительного просмотра и отправки голосового сообщения.
     *
     * @param parent  родительское окно, относительно которого центрируется диалог
     * @param data    массив байт аудиоданных
     * @param service сервис для отправки и воспроизведения голосовых сообщений
     */
    public ChatVoiceDialog(JFrame parent, byte[] data, VoiceService service) {
        super(parent, "Голосовое сообщение", true);
        this.data = data;
        this.service = service;

        initializeUI();
        setupWindowListener();
    }

    /**
     * Инициализирует компоненты пользовательского интерфейса диалога:
     * <ul>
     *     <li>Кнопки: "Прослушать", "Отправить", "Удалить".</li>
     *     <li>Информационное сообщение о готовности к отправке.</li>
     *     <li>Центрирование и базовые параметры окна.</li>
     * </ul>
     */
    private void initializeUI() {
        JButton playButton = new JButton("Прослушать");
        sendButton = new JButton("Отправить");
        JButton cancelButton = new JButton("Удалить");

        playButton.addActionListener(new PlayButtonListener());
        sendButton.addActionListener(new SendButtonListener());
        cancelButton.addActionListener(new CancelButtonListener());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.add(playButton);
        buttonPanel.add(sendButton);
        buttonPanel.add(cancelButton);

        JLabel infoLabel = new JLabel("Голосовое сообщение готово к отправке");
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);

        setLayout(new BorderLayout(10, 10));
        add(infoLabel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(getParent());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
    }

    /**
     * Настраивает слушатель для обработки закрытия окна.
     * При закрытии вызывается {@link #cleanup()} для блокировки кнопок
     * и предотвращения повторной отправки.
     */
    private void setupWindowListener() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanup();
            }
        });
    }

    /**
     * Выполняет очистку состояния при закрытии диалога:
     * <ul>
     *     <li>Отключает кнопку отправки.</li>
     *     <li>Помечает сообщение как уже отправленное или удалённое.</li>
     * </ul>
     */
    private void cleanup() {
        sent = true;
        if (sendButton != null) {
            sendButton.setEnabled(false);
        }
    }

    /**
     * Обработчик кнопки «Прослушать».
     * Воспроизводит записанное голосовое сообщение с помощью {@link VoiceService#playTemp(byte[])}.
     */
    private class PlayButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            service.playTemp(data);
        }
    }

    /**
     * Обработчик кнопки «Отправить».
     * <ul>
     *     <li>Блокирует кнопку отправки и меняет текст на «Отправка...». </li>
     *     <li>Асинхронно вызывает {@link VoiceService#sendVoice(byte[])}.</li>
     *     <li>Закрывает окно после успешной отправки.</li>
     * </ul>
     */
    private class SendButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (sent || service.isSending()) {
                return;
            }

            sent = true;
            sendButton.setEnabled(false);
            sendButton.setText("Отправка...");

            new Thread(() -> {
                service.sendVoice(data);
                SwingUtilities.invokeLater(ChatVoiceDialog.this::dispose);
            }, "VoiceSender").start();
        }
    }

    /**
     * Обработчик кнопки «Удалить».
     * Закрывает диалог без отправки сообщения.
     */
    private class CancelButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            dispose();
        }
    }
}
