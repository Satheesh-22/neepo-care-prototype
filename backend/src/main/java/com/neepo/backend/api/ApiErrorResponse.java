package com.neepo.backend.api;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        boolean success,
        ErrorDetail error,
        Instant timestamp,
        String traceId
) {
    public static ApiErrorResponse of(
            String code,
            String message,
            List<FieldValidationError> fieldErrors,
            String traceId
    ) {
        return new ApiErrorResponse(
                false,
                new ErrorDetail(code, message, List.copyOf(fieldErrors)),
                Instant.now(),
                traceId
        );
    }

    public record ErrorDetail(
            String code,
            String message,
            List<FieldValidationError> fieldErrors
    ) {
    }

    public record FieldValidationError(String field, String message) {
    }
}
