package com.promptstudio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Container for all AI Chat-related request and response DTOs.
 */
public final class ChatDTOs {

    private ChatDTOs() {
    }

    /**
     * Request payload for sending a new message within a chat session.
     */
    public record ChatMessageRequest(

            @NotBlank(message = "Message cannot be empty")
            @Size(max = 4000, message = "Message must not exceed 4000 characters")
            String message
    ) {
    }

    /**
     * Represents a single message within a chat session for API responses.
     */
    public record ChatMessageResponse(
            String role,
            String content,
            LocalDateTime timestamp
    ) {
    }

    /**
     * Full response representation of a chat session, including its
     * complete message history.
     */
    public record ChatSessionResponse(
            String id,
            String title,
            List<ChatMessageResponse> messages,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    /**
     * Lightweight summary of a chat session, excluding full message
     * history. Used for the sidebar session list.
     */
    public record ChatSessionSummary(
            String id,
            String title,
            int messageCount,
            LocalDateTime updatedAt
    ) {
    }
}