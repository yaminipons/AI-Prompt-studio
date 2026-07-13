package com.promptstudio.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Container for generic, reusable response wrapper DTOs shared across
 * all controllers.
 */
public final class CommonDTOs {

    private CommonDTOs() {
    }

    /**
     * Standard envelope for every API response in the system.
     */
    public record ApiResponse<T>(
            boolean success,
            String message,
            T data,
            LocalDateTime timestamp
    ) {
        public static <T> ApiResponse<T> success(String message, T data) {
            return new ApiResponse<>(true, message, data, LocalDateTime.now());
        }

        public static <T> ApiResponse<T> success(String message) {
            return new ApiResponse<>(true, message, null, LocalDateTime.now());
        }

        public static <T> ApiResponse<T> error(String message) {
            return new ApiResponse<>(false, message, null, LocalDateTime.now());
        }
    }

    /**
     * Wraps a paginated list of results along with pagination metadata.
     *
     * @param content       the items on the current page
     * @param pageNumber    the current page number (zero-based)
     * @param pageSize      the number of items requested per page
     * @param totalElements the total number of items across all pages
     * @param totalPages    the total number of pages available
     * @param <T>           the type of item in the page
     */
    public record PageResponse<T>(
            List<T> content,
            int pageNumber,
            int pageSize,
            long totalElements,
            int totalPages
    ) {
    }
}