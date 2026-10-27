package com.neepo.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class DisruptionNotificationEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(DisruptionNotificationEventListener.class);

    private final NotificationAlertService alertService;

    public DisruptionNotificationEventListener(NotificationAlertService alertService) {
        this.alertService = alertService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onDisruptionReported(DisruptionReportedEvent event) {
        try {
            alertService.dispatchDisruptionAlerts(event.companyId(), event.disruption());
        } catch (Exception exception) {
            LOGGER.error("Failed to dispatch disruption notifications for companyId={} disruptionId={}",
                    event.companyId(), event.disruption().id(), exception);
        }
    }
}
