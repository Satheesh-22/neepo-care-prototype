package com.neepo.backend.dto;

public record DisruptionTrendPointDto(
        String period,
        int disruptionCount,
        int resolvedCount,
        double costAvoidedUsd
) {}
