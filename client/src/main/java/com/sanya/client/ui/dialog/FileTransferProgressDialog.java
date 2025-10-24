package com.sanya.client.ui.dialog;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FileTransferProgressDialog — диалоговое окно, отображающее процесс передачи файла.
 * Применяется как при отправке, так и при загрузке данных.
 * Управляется через события FileTransferEvent и потокобезопасен для вызовов из разных потоков.
 *
 * Назначение:
 * Показывает текущий прогресс передачи в процентах и автоматически закрывается при достижении 100%.
 * Для каждого файла создаётся отдельное окно, зарегистрированное в статической коллекции activeDialogs.
 * Доступ к окнам осуществляется через методы open, updateGlobalProgress и close.
 *
 * Потоковая безопасность:
 * Все обновления интерфейса выполняются через SwingUtilities.invokeLater(...).
 *
 * Пример:
 * FileTransferProgressDialog.open(parent, "file.zip", true);
 * FileTransferProgressDialog.updateGlobalProgress("file.zip", 45);
 * FileTransferProgressDialog.updateGlobalProgress("file.zip", 100);
 */
public final class FileTransferProgressDialog extends JDialog {

    /** Активные окна прогресса, одно окно на каждый файл. */
    private static final ConcurrentHashMap<String, FileTransferProgressDialog> activeDialogs = new ConcurrentHashMap<>();

    /** Полоса прогресса. */
    private final JProgressBar progressBar = new JProgressBar(0, 100);

    /** Текст состояния передачи. */
    private final JLabel statusLabel = new JLabel();

    /** Имя файла, связанного с окном. */
    private final String filename;

    /**
     * Создаёт новое окно для наблюдения за прогрессом передачи файла.
     *
     * @param parent   родительское окно
     * @param filename имя файла
     * @param outgoing true — если отправка, false — если приём
     */
    public FileTransferProgressDialog(Frame parent, String filename, boolean outgoing) {
        super(parent, (outgoing ? "Отправка" : "Загрузка") + ": " + filename, false);
        this.filename = filename;

        setSize(400, 120);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        add(new JLabel((outgoing ? "Отправляется" : "Принимается") + " файл: " + filename), BorderLayout.NORTH);
        add(progressBar, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
        progressBar.setStringPainted(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    /**
     * Обновляет состояние прогресса для данного окна.
     * Устанавливает значение полосы и текст состояния.
     * При достижении 100% автоматически закрывает окно и удаляет его из карты активных.
     *
     * @param percent значение прогресса в процентах
     */
    public void updateProgress(int percent) {
        progressBar.setValue(percent);
        statusLabel.setText("Прогресс: " + percent + "%");

        if (percent >= 100) {
            dispose();
            activeDialogs.remove(filename);
        }
    }

    /**
     * Открывает окно прогресса, если оно ещё не создано.
     * Если окно уже существует, возвращает текущий экземпляр.
     *
     * @param parent   родительское окно
     * @param filename имя файла
     * @param outgoing направление передачи
     * @return экземпляр FileTransferProgressDialog
     */
    public static FileTransferProgressDialog open(Frame parent, String filename, boolean outgoing) {
        return activeDialogs.computeIfAbsent(filename, f -> {
            FileTransferProgressDialog dialog = new FileTransferProgressDialog(parent, f, outgoing);
            SwingUtilities.invokeLater(() -> dialog.setVisible(true));
            return dialog;
        });
    }

    /**
     * Выполняет обновление прогресса по имени файла.
     * Используется при получении события FileTransferEvent с типом PROGRESS.
     *
     * @param filename имя файла
     * @param percent  текущий прогресс
     */
    public static void updateGlobalProgress(String filename, int percent) {
        FileTransferProgressDialog dialog = activeDialogs.get(filename);
        if (dialog != null) {
            SwingUtilities.invokeLater(() -> dialog.updateProgress(percent));
        }
    }

    /**
     * Закрывает окно прогресса по имени файла и удаляет его из списка активных.
     *
     * @param filename имя файла
     */
    public static void close(String filename) {
        FileTransferProgressDialog dialog = activeDialogs.remove(filename);
        if (dialog != null) {
            SwingUtilities.invokeLater(dialog::dispose);
        }
    }
}
