package com.neepo.backend.dto;

public record DisruptionRootCauseDto(
        String cause,
        int count,
        double percentage,
        double averageResolutionMinutes
) {}
