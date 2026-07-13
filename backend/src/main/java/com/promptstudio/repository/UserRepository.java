package com.promptstudio.repository;

import com.promptstudio.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User entity operations against the "users" collection.
 * Provides authentication lookups and admin user-management queries.
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    /**
     * Finds a user by their email address. Used during login and
     * registration duplicate-check.
     *
     * @param email the email to search for
     * @return an Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks whether a user with the given email already exists.
     * Used during registration to enforce uniqueness before insert.
     *
     * @param email the email to check
     * @return true if a user with this email exists
     */
    boolean existsByEmail(String email);

    /**
     * Counts how many users currently have the given active status.
     * Used by the Admin Dashboard for platform statistics
     * (e.g., total active vs deactivated accounts).
     *
     * @param active the active status to count
     * @return the number of matching users
     */
    long countByActive(boolean active);
}