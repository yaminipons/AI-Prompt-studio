package com.promptstudio.entity;

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
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a user-defined grouping of prompts (e.g., "Marketing Copy",
 * "University Assignments"). Maps to the "prompt_collections" collection
 * in MongoDB. Prompts reference a collection via their {@code collectionId}
 * field; this entity also keeps a denormalized list of prompt IDs for
 * quick collection-size lookups without an extra aggregation query.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "prompt_collections")
public class PromptCollection {

    /** Unique MongoDB-generated identifier for this collection. */
    @Id
    private String id;

    /** ID of the User who owns this collection. */
    @Indexed
    @Field("user_id")
    private String userId;

    /** Display name of the collection. */
    @NotBlank(message = "Collection name is required")
    @Field("name")
    private String name;

    /** Optional description of what this collection is for. */
    @Field("description")
    private String description;

    /** Hex color code used to visually distinguish this collection in the UI. */
    @Field("color")
    @Builder.Default
    private String color = "#6366F1";

    /** Denormalized list of prompt IDs currently in this collection. */
    @Field("prompt_ids")
    @Builder.Default
    private List<String> promptIds = new ArrayList<>();

    /** Timestamp when this collection was created (auto-populated). */
    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    /** Timestamp when this collection was last updated (auto-populated). */
    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;
}