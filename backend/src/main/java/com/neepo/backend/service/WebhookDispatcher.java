package com.neepo.backend.service;

public interface WebhookDispatcher {
    WebhookDispatchResult dispatch(String targetEndpoint, String signature, String payload);
}
