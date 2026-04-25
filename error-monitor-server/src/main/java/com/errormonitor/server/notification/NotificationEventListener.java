package com.errormonitor.server.notification;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificationEventListener {

    private final SlackNotificationService slackNotificationService;

    public NotificationEventListener(SlackNotificationService slackNotificationService) {
        this.slackNotificationService = slackNotificationService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationEvent(NotificationEvent event) {
        slackNotificationService.send(event);
    }
}
