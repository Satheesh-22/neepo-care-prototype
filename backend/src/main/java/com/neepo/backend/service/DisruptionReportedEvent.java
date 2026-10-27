package com.neepo.backend.service;

import com.neepo.contracts.DisruptionEvent;

import java.util.UUID;

public record DisruptionReportedEvent(
        UUID companyId,
        DisruptionEvent disruption
) {
}
