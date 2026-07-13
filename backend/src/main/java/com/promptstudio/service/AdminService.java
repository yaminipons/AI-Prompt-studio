package com.promptstudio.service;

import com.promptstudio.dto.PromptDTOs.PromptResponse;
import com.promptstudio.dto.PromptDTOs.AnalysisResponse;
import com.promptstudio.dto.PromptDTOs.BattleResponse;
import com.promptstudio.dto.PromptDTOs.BattleEntryResponse;
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
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service layer implementing Admin Dashboard functionality: user
 * management (listing, activating/deactivating accounts) and
 * platform-wide statistics and prompt visibility. All methods here
 * operate across every user in the system, unlike {@link UserService}
 * and {@link PromptService} which are strictly scoped to the currently
 * authenticated user. Access to this service's controller endpoints is
 * already restricted to ROLE_ADMIN at the security filter chain level.
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PromptRepository promptRepository;
    private final PromptCollectionRepository promptCollectionRepository;
    private final ChatSessionRepository chatSessionRepository;

    /**
     * Platform-wide statistics summary displayed on the Admin Dashboard.
     *
     * @param totalUsers          total number of registered accounts
     * @param activeUsers         number of currently active accounts
     * @param deactivatedUsers    number of deactivated accounts
     * @param totalPrompts        total number of prompt records across all users
     * @param totalSavedPrompts   total number of prompts saved to any Library
     * @param totalCollections    total number of collections across all users
     * @param totalChatSessions   total number of chat sessions across all users
     * @param actionBreakdown     platform-wide count of prompts grouped by action type
     */
    public record AdminStatsResponse(
            long totalUsers,
            long activeUsers,
            long deactivatedUsers,
            long totalPrompts,
            long totalSavedPrompts,
            long totalCollections,
            long totalChatSessions,
            Map<String, Long> actionBreakdown
    ) {
    }

    /**
     * Retrieves a paginated list of every registered user, most
     * recently created first, for the Admin user-management view.
     *
     * @param page zero-based page number
     * @param size page size
     * @return a page of UserResponse
     */
    public Page<UserResponse> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findAll(pageable).map(this::mapUserToResponse);
    }

    /**
     * Retrieves a single user's full profile by ID, for admin inspection.
     *
     * @param userId the ID of the user to retrieve
     * @return the mapped UserResponse
     * @throws ApiException with 404 NOT_FOUND if no such user exists
     */
    public UserResponse getUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        return mapUserToResponse(user);
    }

    /**
     * Toggles a user's active status, allowing an admin to deactivate
     * (soft-ban) or reactivate an account without deleting their data.
     * Deactivated users are still blocked at login via
     * {@code UserPrincipal.isEnabled()}, which reads this same flag.
     *
     * @param userId the ID of the user to toggle
     * @return the updated UserResponse
     * @throws ApiException with 404 NOT_FOUND if no such user exists
     */
    public UserResponse toggleUserActiveStatus(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        user.setActive(!user.isActive());
        User saved = userRepository.save(user);
        return mapUserToResponse(saved);
    }

    /**
     * Promotes or demotes a user's role between USER and ADMIN.
     * Only accessible to admins, matching this service's overall
     * access restriction.
     *
     * @param userId the ID of the user whose role should be changed
     * @param role   the new role, as a string ("USER" or "ADMIN")
     * @return the updated UserResponse
     * @throws ApiException with 404 NOT_FOUND if no such user exists,
     *                       or 400 BAD_REQUEST if the role string is invalid
     */
    public UserResponse updateUserRole(String userId, String role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        try {
            user.setRole(com.promptstudio.enums.UserRole.valueOf(role.trim().toUpperCase()));
        } catch (IllegalArgumentException ex) {
            throw new ApiException("Invalid role. Use USER or ADMIN", HttpStatus.BAD_REQUEST);
        }

        User saved = userRepository.save(user);
        return mapUserToResponse(saved);
    }

    /**
     * Retrieves a paginated view of every prompt record across every
     * user in the system, most recent first, for admin visibility into
     * platform usage and content.
     *
     * @param page zero-based page number
     * @param size page size
     * @return a page of PromptResponse
     */
    public Page<PromptResponse> getAllPrompts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return promptRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::mapPromptToResponse);
    }

    /**
     * Builds an aggregated platform-wide statistics summary for the
     * Admin Dashboard home view.
     *
     * @return the aggregated AdminStatsResponse
     */
    public AdminStatsResponse getPlatformStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByActive(true);
        long deactivatedUsers = userRepository.countByActive(false);

        long totalPrompts = promptRepository.count();

        long totalSavedPrompts = promptRepository.findAllByOrderByCreatedAtDesc(Pageable.unpaged())
                .getContent().stream()
                .filter(Prompt::isSaved)
                .count();

        long totalCollections = promptCollectionRepository.count();
        long totalChatSessions = chatSessionRepository.count();

        Map<String, Long> actionBreakdown = new LinkedHashMap<>();
        for (PromptAction action : PromptAction.values()) {
            long count = promptRepository.findAllByOrderByCreatedAtDesc(Pageable.unpaged())
                    .getContent().stream()
                    .filter(p -> p.getAction() == action)
                    .count();
            actionBreakdown.put(action.name(), count);
        }

        return new AdminStatsResponse(
                totalUsers,
                activeUsers,
                deactivatedUsers,
                totalPrompts,
                totalSavedPrompts,
                totalCollections,
                totalChatSessions,
                actionBreakdown
        );
    }

    /**
     * Maps a User entity to its public-facing response DTO, ensuring
     * the password hash is never exposed to the client.
     *
     * @param user the entity to map
     * @return the mapped UserResponse
     */
    private UserResponse mapUserToResponse(User user) {
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

    /**
     * Maps a Prompt entity to its API-facing response DTO, converting
     * nested analysis/battle data and enum fields to their DTO forms.
     * Mirrors the equivalent mapping logic in PromptService, kept
     * separate here since AdminService has a distinct, unscoped access
     * pattern (all users' prompts rather than one user's own).
     *
     * @param prompt the entity to map
     * @return the mapped PromptResponse
     */
    private PromptResponse mapPromptToResponse(Prompt prompt) {
        AnalysisResponse analysisResponse = null;
        if (prompt.getAnalysis() != null) {
            Prompt.AnalysisResult a = prompt.getAnalysis();
            analysisResponse = new AnalysisResponse(
                    a.getGrammarScore(), a.getClarityScore(), a.getContextScore(),
                    a.getHallucinationRisk(), a.getComplexityScore(), a.getOverallScore(),
                    a.getSuggestions()
            );
        }

        BattleResponse battleResponse = null;
        if (prompt.getBattle() != null) {
            Prompt.BattleResult b = prompt.getBattle();
            List<BattleEntryResponse> entryResponses = b.getEntries().stream()
                    .map(e -> new BattleEntryResponse(e.getPromptType(), e.getPromptText(), e.getAiOutput(), e.getScore()))
                    .toList();
            battleResponse = new BattleResponse(entryResponses, b.getRecommendedType(), b.getRecommendationReason());
        }

        return new PromptResponse(
                prompt.getId(),
                prompt.getTitle(),
                prompt.getAction() != null ? prompt.getAction().name() : null,
                prompt.getPromptType() != null ? prompt.getPromptType().name() : null,
                prompt.getOriginalInput(),
                prompt.getContext(),
                prompt.getGeneratedPrompt(),
                prompt.isSaved(),
                prompt.isFavorite(),
                prompt.getTags(),
                prompt.getCollectionId(),
                analysisResponse,
                battleResponse,
                prompt.getCreatedAt(),
                prompt.getUpdatedAt()
        );
    }
}