package com.neepo.backend.exception;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(UUID userId) {
        super("User not found: " + userId);
    }

    public static UserNotFoundException forEmail() {
        return new UserNotFoundException("User not found for the supplied email");
    }

    private UserNotFoundException(String message) {
        super(message);
    }
}
