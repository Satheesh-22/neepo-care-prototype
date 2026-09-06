package com.neepo.backend.api;

import java.time.Instant;

public record ApiResponse<T>(
        boolean success,
        T data,
        Instant timestamp,
        String traceId
) {
    public static <T> ApiResponse<T> success(T data, String traceId) {
        return new ApiResponse<>(true, data, Instant.now(), traceId);
    }
}
