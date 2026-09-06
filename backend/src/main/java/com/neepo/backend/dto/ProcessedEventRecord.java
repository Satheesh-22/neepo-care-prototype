package com.neepo.backend.dto;

import java.util.UUID;

public record ProcessedEventRecord(String eventType, String payloadHash, UUID resultReferenceId) {
}
