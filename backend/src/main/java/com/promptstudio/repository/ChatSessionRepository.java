package com.promptstudio.repository;

import com.promptstudio.entity.ChatSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ChatSession entity operations against the
 * "chat_sessions" collection. Supports the AI Chat feature.
 * Individual chat messages are embedded within each session document,
 * so no separate message repository is needed.
 */
@Repository
public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {

    /**
     * Retrieves all chat sessions belonging to a user, most recently
     * updated first, so active conversations appear at the top of the
     * sidebar chat list.
     *
     * @param userId the owning user's ID
     * @return the list of the user's chat sessions
     */
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(String userId);

    /**
     * Finds a single chat session by its ID, scoped to a specific owner.
     * Used to ensure users can only access/modify their own chat sessions.
     *
     * @param id     the chat session ID
     * @param userId the owning user's ID
     * @return an Optional containing the session if found and owned by the user
     */
    Optional<ChatSession> findByIdAndUserId(String id, String userId);

    /**
     * Counts how many chat sessions a user currently has.
     * Used for Dashboard and Profile statistics.
     *
     * @param userId the owning user's ID
     * @return the number of chat sessions owned by the user
     */
    long countByUserId(String userId);
}