package com.neepo.backend.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Email or password is incorrect");
    }

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
