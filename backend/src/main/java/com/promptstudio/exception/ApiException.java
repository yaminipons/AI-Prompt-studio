package com.promptstudio.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Generic application exception used across all service classes to
 * signal a business-rule or resource error along with the appropriate
 * HTTP status code to return. A single exception type is used instead
 * of many narrow subclasses (ResourceNotFoundException, DuplicateException,
 * etc.) since the message + status pair is sufficient information for
 * the GlobalExceptionHandler to build a correct API response.
 */
@Getter
public class ApiException extends RuntimeException {

    /** The HTTP status code that should be returned to the client. */
    private final HttpStatus status;

    /**
     * Creates a new API exception.
     *
     * @param message human-readable error message, shown to the client
     * @param status  the HTTP status code to respond with
     */
    public ApiException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}