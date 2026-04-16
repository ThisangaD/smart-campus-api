package com.smartcampus.exception;

/**
 * Thrown when a POST reading is attempted on a MAINTENANCE sensor.
 * Mapped to HTTP 403 Forbidden.
 */
public class SensorUnavailableException extends RuntimeException {
    public SensorUnavailableException(String message) {
        super(message);
    }
}
