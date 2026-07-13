package com.promptstudio.repository;

import com.promptstudio.entity.PromptCollection;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for PromptCollection entity operations against the
 * "prompt_collections" collection. Supports the Prompt Collections
 * feature, allowing users to group related saved prompts.
 */
@Repository
public interface PromptCollectionRepository extends MongoRepository<PromptCollection, String> {

    /**
     * Retrieves all collections belonging to a user, most recently
     * updated first, so actively-used collections surface at the top.
     *
     * @param userId the owning user's ID
     * @return the list of the user's collections
     */
    List<PromptCollection> findByUserIdOrderByUpdatedAtDesc(String userId);

    /**
     * Finds a single collection by its ID, scoped to a specific owner.
     * Used to ensure users can only access/modify their own collections.
     *
     * @param id     the collection ID
     * @param userId the owning user's ID
     * @return an Optional containing the collection if found and owned by the user
     */
    Optional<PromptCollection> findByIdAndUserId(String id, String userId);

    /**
     * Checks whether a user already has a collection with the given name,
     * used to prevent duplicate collection names for the same user.
     *
     * @param userId the owning user's ID
     * @param name   the collection name to check
     * @return true if a collection with this name already exists for the user
     */
    boolean existsByUserIdAndName(String userId, String name);

    /**
     * Counts how many collections a user currently has.
     * Used for Dashboard and Profile statistics.
     *
     * @param userId the owning user's ID
     * @return the number of collections owned by the user
     */
    long countByUserId(String userId);
}