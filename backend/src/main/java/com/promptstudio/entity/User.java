package com.promptstudio.entity;

import com.promptstudio.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * Represents a registered user account in the AI Prompt Engineering Studio.
 * Maps to the "users" collection in MongoDB. Stores authentication
 * credentials, profile information, and role-based access data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    /** Unique MongoDB-generated identifier for this user. */
    @Id
    private String id;

    /** User's full display name. */
    @NotBlank(message = "Full name is required")
    @Field("full_name")
    private String fullName;

    /** User's email address. Used as the unique login identifier. */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Indexed(unique = true)
    @Field("email")
    private String email;

    /** BCrypt-hashed password. The raw password is never stored. */
    @Field("password_hash")
    private String passwordHash;

    /** The authorization role assigned to this user. Defaults to USER. */
    @Field("role")
    @Builder.Default
    private UserRole role = UserRole.USER;

    /** URL pointing to the user's profile picture, if uploaded. */
    @Field("profile_image_url")
    private String profileImageUrl;

    /** Short user-provided biography shown on their profile page. */
    @Field("bio")
    private String bio;

    /** Whether this account is active. Admins can deactivate without deleting. */
    @Field("is_active")
    @Builder.Default
    private boolean active = true;

    /** Timestamp of the user's most recent successful login. */
    @Field("last_login_at")
    private LocalDateTime lastLoginAt;

    /** Timestamp when this account was created (auto-populated). */
    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    /** Timestamp when this account was last updated (auto-populated). */
    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;
}