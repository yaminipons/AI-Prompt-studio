package com.promptstudio.exception;

import com.promptstudio.dto.CommonDTOs.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralized exception handler applied across every controller.
 * Converts all recognized exception types into a consistent
 * {@link ApiResponse} JSON envelope with an appropriate HTTP status,
 * so the frontend never has to handle multiple differently-shaped
 * error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles application-level business errors thrown deliberately by
     * the service layer via {@link ApiException}, using the status code
     * carried on the exception itself.
     *
     * @param ex the caught ApiException
     * @return a ResponseEntity with the exception's status and message
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Object>> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles Bean Validation failures on {@code @Valid @RequestBody}
     * DTOs, collecting every field error into a single readable
     * message rather than exposing Spring's verbose default format.
     *
     * @param ex the caught MethodArgumentNotValidException
     * @return a 400 BAD_REQUEST response listing all validation failures
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        String combinedMessage = String.join("; ", fieldErrors.values());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Validation failed: " + combinedMessage));
    }

    /**
     * Handles authentication failures that escape the normal login flow
     * (rare, since AuthService already catches these, but covered here
     * as a safety net for any other authentication entry point).
     *
     * @param ex the caught BadCredentialsException
     * @return a 401 UNAUTHORIZED response
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid credentials"));
    }

    /**
     * Handles authorization failures when an authenticated user
     * attempts to access a resource their role does not permit
     * (e.g., a non-admin hitting an admin-only endpoint).
     *
     * @param ex the caught AccessDeniedException
     * @return a 403 FORBIDDEN response
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("You do not have permission to perform this action"));
    }

    /**
     * Catch-all handler for any exception not explicitly handled above,
     * preventing raw stack traces from ever reaching the client while
     * still returning a usable JSON error response.
     *
     * @param ex the caught unhandled exception
     * @return a 500 INTERNAL_SERVER_ERROR response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred. Please try again."));
    }
}