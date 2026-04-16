package com.smartcampus.exception;

/**
 * Thrown when a POST sensor references a roomId that does not exist.
 * Mapped to HTTP 422 Unprocessable Entity.
 */
public class LinkedResourceNotFoundException extends RuntimeException {
    public LinkedResourceNotFoundException(String message) {
        super(message);
    }
}
