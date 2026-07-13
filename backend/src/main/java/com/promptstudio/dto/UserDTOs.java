package com.promptstudio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Container for User profile and Dashboard-related request and response DTOs.
 */
public final class UserDTOs {

    private UserDTOs() {
    }

    public record UserResponse(
            String id,
            String fullName,
            String email,
            String role,
            String profileImageUrl,
            String bio,
            boolean active,
            LocalDateTime lastLoginAt,
            LocalDateTime createdAt
    ) {
    }

    public record UpdateProfileRequest(

            @NotBlank(message = "Full name is required")
            @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
            String fullName,

            @Size(max = 500, message = "Bio must not exceed 500 characters")
            String bio,

            String profileImageUrl
    ) {
    }

    public record ChangePasswordRequest(

            @NotBlank(message = "Current password is required")
            String currentPassword,

            @NotBlank(message = "New password is required")
            @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
            @Pattern(
                    regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                    message = "Password must contain at least one letter and one number"
            )
            String newPassword
    ) {
    }

    /**
     * Response payload summarizing the authenticated user's activity,
     * displayed on the Dashboard home view.
     *
     * @param totalPrompts       total number of prompt records created
     * @param savedPrompts       number of prompts saved to the Library
     * @param favoritePrompts    number of favorited prompts
     * @param totalCollections   number of prompt collections created
     * @param totalChatSessions  number of AI Chat sessions started
     * @param actionBreakdown    count of prompts grouped by action type
     * @param averagePromptScore the average overall analyzer score across all analyzed prompts
     */
    public record DashboardStatsResponse(
            long totalPrompts,
            long savedPrompts,
            long favoritePrompts,
            long totalCollections,
            long totalChatSessions,
            Map<String, Long> actionBreakdown,
            double averagePromptScore
    ) {
    }
}