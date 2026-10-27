package com.neepo.backend.service;

public record WebhookDispatchResult(
        boolean success,
        int statusCode,
        String message
) {
    public static WebhookDispatchResult ok(int statusCode, String message) {
        return new WebhookDispatchResult(true, statusCode, message);
    }

    public static WebhookDispatchResult failure(int statusCode, String message) {
        return new WebhookDispatchResult(false, statusCode, message);
    }
}
