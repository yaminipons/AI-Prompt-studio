package com.promptstudio.service;

import com.promptstudio.dto.UserDTOs.ChangePasswordRequest;
import com.promptstudio.dto.UserDTOs.DashboardStatsResponse;
import com.promptstudio.dto.UserDTOs.UpdateProfileRequest;
import com.promptstudio.dto.UserDTOs.UserResponse;
import com.promptstudio.entity.Prompt;
import com.promptstudio.entity.User;
import com.promptstudio.enums.PromptAction;
import com.promptstudio.exception.ApiException;
import com.promptstudio.repository.ChatSessionRepository;
import com.promptstudio.repository.PromptCollectionRepository;
import com.promptstudio.repository.PromptRepository;
import com.promptstudio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service layer handling authenticated user profile management and
 * Dashboard statistics aggregation. Scoped strictly to the currently
 * authenticated user's own data - no cross-user access here (that is
 * AdminService's responsibility).
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PromptRepository promptRepository;
    private final PromptCollectionRepository promptCollectionRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Retrieves the profile information of the given user.
     *
     * @param userId the authenticated user's ID
     * @return the mapped UserResponse
     * @throws ApiException with 404 NOT_FOUND if no such user exists
     */
    public UserResponse getProfile(String userId) {
        User user = findUserOrThrow(userId);
        return mapToResponse(user);
    }

    /**
     * Updates the given user's editable profile fields (name, bio, avatar).
     * Email and password are intentionally not editable through this method.
     *
     * @param userId  the authenticated user's ID
     * @param request the updated profile fields
     * @return the updated UserResponse
     * @throws ApiException with 404 NOT_FOUND if no such user exists
     */
    public UserResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = findUserOrThrow(userId);

        user.setFullName(request.fullName().trim());
        user.setBio(request.bio());
        if (request.profileImageUrl() != null && !request.profileImageUrl().isBlank()) {
            user.setProfileImageUrl(request.profileImageUrl());
        }

        User saved = userRepository.save(user);
        return mapToResponse(saved);
    }

    /**
     * Changes the given user's password after verifying their current
     * password matches what is stored.
     *
     * @param userId  the authenticated user's ID
     * @param request the current and new password payload
     * @throws ApiException with 404 NOT_FOUND if no such user exists,
     *                       or 400 BAD_REQUEST if the current password is incorrect
     */
    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = findUserOrThrow(userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiException("Current password is incorrect", HttpStatus.BAD_REQUEST);
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    /**
     * Builds an aggregated statistics summary for the Dashboard home
     * view: total prompts, saved/favorite counts, collection and chat
     * counts, a per-feature usage breakdown, and the user's average
     * prompt quality score across all analyzed prompts.
     *
     * @param userId the authenticated user's ID
     * @return the aggregated DashboardStatsResponse
     */
    public DashboardStatsResponse getDashboardStats(String userId) {
        long totalPrompts = promptRepository.countByUserId(userId);
        long savedPrompts = promptRepository.countByUserIdAndSaved(userId, true);

        long favoritePrompts = promptRepository
                .findByUserIdAndFavoriteOrderByCreatedAtDesc(userId, true, PageRequest.of(0, 1))
                .getTotalElements();

        long totalCollections = promptCollectionRepository.countByUserId(userId);
        long totalChatSessions = chatSessionRepository.countByUserId(userId);

        Map<String, Long> actionBreakdown = new LinkedHashMap<>();
        for (PromptAction action : PromptAction.values()) {
            actionBreakdown.put(action.name(), promptRepository.countByUserIdAndAction(userId, action));
        }

        double averageScore = calculateAveragePromptScore(userId);

        return new DashboardStatsResponse(
                totalPrompts,
                savedPrompts,
                favoritePrompts,
                totalCollections,
                totalChatSessions,
                actionBreakdown,
                averageScore
        );
    }

    /**
     * Computes the average overall analyzer score across every prompt
     * record the user has run through the Prompt Analyzer feature.
     * Returns 0.0 if the user has never used the analyzer.
     *
     * @param userId the authenticated user's ID
     * @return the average overall score, or 0.0 if no analyzed prompts exist
     */
    private double calculateAveragePromptScore(String userId) {
        Page<Prompt> analyzed = promptRepository.findByUserIdAndActionOrderByCreatedAtDesc(
                userId, PromptAction.ANALYZE, Pageable.unpaged()
        );

        List<Prompt> analyzedPrompts = analyzed.getContent();

        if (analyzedPrompts.isEmpty()) {
            return 0.0;
        }

        double total = 0.0;
        int count = 0;
        for (Prompt prompt : analyzedPrompts) {
            if (prompt.getAnalysis() != null) {
                total += prompt.getAnalysis().getOverallScore();
                count++;
            }
        }

        return count == 0 ? 0.0 : Math.round((total / count) * 10.0) / 10.0;
    }

    /**
     * Looks up a user by ID or throws a standard 404 API exception.
     * Centralizes this common lookup pattern used by every method above.
     *
     * @param userId the user ID to find
     * @return the found User entity
     * @throws ApiException with 404 NOT_FOUND if no such user exists
     */
    private User findUserOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
    }

    /**
     * Maps a User entity to its public-facing response DTO, ensuring
     * the password hash is never exposed to the client.
     *
     * @param user the entity to map
     * @return the mapped UserResponse
     */
    private UserResponse mapToResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name(),
                user.getProfileImageUrl(),
                user.getBio(),
                user.isActive(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }
}