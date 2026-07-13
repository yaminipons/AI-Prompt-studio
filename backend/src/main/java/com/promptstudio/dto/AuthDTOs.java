package com.promptstudio.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Container for all Authentication-related request and response DTOs.
 */
public final class AuthDTOs {

    private AuthDTOs() {
    }

    public record RegisterRequest(

            @NotBlank(message = "Full name is required")
            @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
            String fullName,

            @NotBlank(message = "Email is required")
            @Email(message = "Email must be valid")
            String email,

            @NotBlank(message = "Password is required")
            @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
            @Pattern(
                    regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                    message = "Password must contain at least one letter and one number"
            )
            String password
    ) {
    }

    public record LoginRequest(

            @NotBlank(message = "Email is required")
            @Email(message = "Email must be valid")
            String email,

            @NotBlank(message = "Password is required")
            String password
    ) {
    }

    /**
     * Response payload returned after successful login or registration.
     *
     * @param token     the signed JWT access token
     * @param tokenType the token type, always "Bearer"
     * @param userId    the authenticated user's ID
     * @param fullName  the authenticated user's display name
     * @param email     the authenticated user's email
     * @param role      the authenticated user's role, as a String
     */
    public record AuthResponse(
            String token,
            String tokenType,
            String userId,
            String fullName,
            String email,
            String role
    ) {
        /**
         * Convenience factory that always sets tokenType to "Bearer".
         *
         * @param token    the signed JWT
         * @param userId   the user's ID
         * @param fullName the user's display name
         * @param email    the user's email
         * @param role     the user's role name
         * @return the constructed AuthResponse
         */
        public static AuthResponse of(String token, String userId, String fullName, String email, String role) {
            return new AuthResponse(token, "Bearer", userId, fullName, email, role);
        }
    }
}