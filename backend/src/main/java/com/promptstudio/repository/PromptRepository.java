package com.promptstudio.repository;

import com.promptstudio.entity.Prompt;
import com.promptstudio.enums.PromptAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Prompt entity operations against the "prompts" collection.
 * This single repository serves the Prompt Generator, Optimizer, Analyzer,
 * Battle Arena, Library, History, and Collections features, since they all
 * operate on the same unified Prompt document shape.
 */
@Repository
public interface PromptRepository extends MongoRepository<Prompt, String> {

    /**
     * Finds a single prompt by its ID, scoped to a specific owner.
     * Used to ensure users can only access their own prompts.
     *
     * @param id     the prompt ID
     * @param userId the owning user's ID
     * @return an Optional containing the prompt if found and owned by the user
     */
    Optional<Prompt> findByIdAndUserId(String id, String userId);

    /**
     * Retrieves all prompts belonging to a user, most recent first.
     * Used for the Prompt History view (includes both saved and unsaved records).
     *
     * @param userId   the owning user's ID
     * @param pageable pagination and sorting parameters
     * @return a page of the user's prompt records
     */
    Page<Prompt> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * Retrieves only the prompts a user has explicitly saved to their
     * Prompt Library, most recent first.
     *
     * @param userId   the owning user's ID
     * @param saved    whether the prompt is saved to the library (true)
     * @param pageable pagination and sorting parameters
     * @return a page of the user's saved library prompts
     */
    Page<Prompt> findByUserIdAndSavedOrderByCreatedAtDesc(String userId, boolean saved, Pageable pageable);

    /**
     * Retrieves a user's favorited prompts, most recent first.
     *
     * @param userId   the owning user's ID
     * @param favorite whether the prompt is marked as favorite (true)
     * @param pageable pagination and sorting parameters
     * @return a page of the user's favorite prompts
     */
    Page<Prompt> findByUserIdAndFavoriteOrderByCreatedAtDesc(String userId, boolean favorite, Pageable pageable);

    /**
     * Retrieves all prompts belonging to a specific collection, owned by a user.
     *
     * @param userId       the owning user's ID
     * @param collectionId the collection ID to filter by
     * @return the list of prompts in that collection
     */
    List<Prompt> findByUserIdAndCollectionId(String userId, String collectionId);

    /**
     * Retrieves a user's prompts filtered by which AI feature created them
     * (GENERATE, OPTIMIZE, ANALYZE, BATTLE, MANUAL), most recent first.
     *
     * @param userId   the owning user's ID
     * @param action   the action/feature type to filter by
     * @param pageable pagination and sorting parameters
     * @return a page of matching prompt records
     */
    Page<Prompt> findByUserIdAndActionOrderByCreatedAtDesc(String userId, PromptAction action, Pageable pageable);

    /**
     * Performs a case-insensitive text search across a user's saved
     * library prompts, matching against title, original input, or the
     * generated prompt text. Used by the Prompt Library search bar.
     *
     * @param userId   the owning user's ID
     * @param keyword  the search keyword (regex-matched, case-insensitive)
     * @param pageable pagination parameters
     * @return a page of matching saved prompts
     */
    @Query("{ 'user_id': ?0, 'is_saved': true, $or: [ " +
           "{ 'title': { $regex: ?1, $options: 'i' } }, " +
           "{ 'original_input': { $regex: ?1, $options: 'i' } }, " +
           "{ 'generated_prompt': { $regex: ?1, $options: 'i' } } ] }")
    Page<Prompt> searchSavedPrompts(String userId, String keyword, Pageable pageable);

    /**
     * Counts how many prompt records a user has created in total.
     * Used for Dashboard and Profile statistics.
     *
     * @param userId the owning user's ID
     * @return total prompt record count for this user
     */
    long countByUserId(String userId);

    /**
     * Counts how many prompts a user has saved to their Library.
     * Used for Dashboard statistics.
     *
     * @param userId the owning user's ID
     * @param saved  whether counting saved (true) records
     * @return count of matching prompt records
     */
    long countByUserIdAndSaved(String userId, boolean saved);

    /**
     * Counts how many prompt records exist for a user, grouped implicitly
     * by action, called once per action type from the service layer to
     * build a per-feature usage breakdown for the Dashboard.
     *
     * @param userId the owning user's ID
     * @param action the action/feature type to count
     * @return count of matching prompt records
     */
    long countByUserIdAndAction(String userId, PromptAction action);

    /**
     * Deletes all prompt records belonging to a given collection, used
     * when a collection is deleted so its prompts don't reference a
     * dangling collection ID (they remain in the Library, just unlinked).
     * Note: this is a placeholder query hook; actual unlinking (not deletion)
     * logic is handled in the service layer via findByUserIdAndCollectionId.
     *
     * @param userId the owning user's ID
     * @return total count of records for internal admin statistics
     */
    long deleteByUserIdAndCollectionId(String userId, String collectionId);

    /**
     * Retrieves ALL prompts across all users, most recent first.
     * Used exclusively by the Admin Dashboard for platform-wide visibility.
     *
     * @param pageable pagination and sorting parameters
     * @return a page of all prompt records in the system
     */
    Page<Prompt> findAllByOrderByCreatedAtDesc(Pageable pageable);
}