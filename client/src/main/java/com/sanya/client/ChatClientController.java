package com.sanya.client;

import com.sanya.client.ui.UIFacade;
import com.sanya.events.*;
import com.sanya.files.FileTransferEvent;

/**
 * Контроллер, связывающий события и фасад UI.
 * Принимает события от EventBus и передает их в UIFacade.
 */
public class ChatClientController {

    private final ApplicationContext context;
    private final UIFacade ui;

    public ChatClientController(ApplicationContext context) {
        this.context = context;
        this.ui = context.getUIFacade();

        EventBus eventBus = context.getEventBus();
        if (eventBus != null) {
            subscribeToEvents(eventBus);
        }
    }

    private void subscribeToEvents(EventBus eventBus) {
        eventBus.subscribe(MessageReceivedEvent.class, e -> ui.appendChatMessage(e.message().toString()));
        eventBus.subscribe(UserListUpdatedEvent.class, e -> ui.updateUserList(e.usernames()));
        eventBus.subscribe(ClearChatEvent.class, e -> ui.clearChat());

        eventBus.subscribe(UserConnectedEvent.class, e ->
                ui.appendSystemMessage("[SYSTEM] " + e.username() + " joined"));

        eventBus.subscribe(UserDisconnectedEvent.class, e ->
                ui.appendSystemMessage("[SYSTEM] " + e.username() + " left"));

        eventBus.subscribe(VoiceMessageReadyEvent.class, e -> ui.showVoiceMessage(e.username(), e.data()));
        eventBus.subscribe(VoiceRecordingEvent.class, e -> ui.showVoiceRecordingStatus(e.started()));

        eventBus.subscribe(FileTransferEvent.class, e -> {
            switch (e.type()) {
                case STARTED -> ui.showFileTransferProgress(e.filename(), 0, e.outgoing());
                case PROGRESS -> {
                    int percent = (int) ((100.0 * e.transferredBytes()) / e.totalBytes());
                    ui.showFileTransferProgress(e.filename(), percent, e.outgoing());
                }
                case COMPLETED -> ui.showFileTransferCompleted(e.filename(), e.outgoing());
                case FAILED -> ui.showError("Ошибка передачи файла: " + e.errorMessage());
            }
        });
    }

    public void sendMessage(String text) {
        if (text == null || text.isBlank()) return;
        context.getEventBus().publish(new MessageSendEvent(text));
        ui.appendChatMessage("Я: " + text);
    }
}
