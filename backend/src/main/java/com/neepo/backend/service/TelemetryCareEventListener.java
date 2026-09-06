package com.neepo.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TelemetryCareEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryCareEventListener.class);

    private final TelemetryCareProcessingService processing;

    public TelemetryCareEventListener(TelemetryCareProcessingService processing) {
        this.processing = processing;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterTelemetryCommit(TelemetryUpdatedEvent event) {
        try {
            processing.process(event);
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "CARE telemetry processing failed tripId={} eventId={}",
                    event.observation().tripId(), event.observation().eventId(), exception
            );
        }
    }
}
