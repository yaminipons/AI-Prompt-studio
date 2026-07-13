package com.promptstudio.entity;

import com.promptstudio.enums.PromptAction;
import com.promptstudio.enums.PromptType;
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
 * Unified entity representing a prompt-related record produced by any
 * of the AI features: Generator, Optimizer, Analyzer, or Battle Arena.
 * <p>
 * A single collection design is used instead of separate collections
 * for "library", "history", and "analysis results" because they all
 * share the same core shape (a prompt with metadata). The {@code action}
 * field indicates which feature created it, and the {@code saved} flag
 * indicates whether it should appear in the user's Prompt Library
 * (true) or only in their Prompt History (false, until they save it).
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "prompts")
public class Prompt {

    /** Unique MongoDB-generated identifier for this prompt record. */
    @Id
    private String id;

    /** ID of the User who owns this prompt record. */
    @Indexed
    @Field("user_id")
    private String userId;

    /** User-friendly title for this prompt (auto-generated or user-edited). */
    @Field("title")
    private String title;

    /** Which AI feature produced this record. */
    @Indexed
    @Field("action")
    private PromptAction action;

    /** Which prompt engineering style was used (null for ANALYZE action). */
    @Field("prompt_type")
    private PromptType promptType;

    /** The original raw input/task description the user provided. */
    @NotBlank(message = "Original input is required")
    @Field("original_input")
    private String originalInput;

    /** Additional context supplied by the user (audience, tone, constraints). */
    @Field("context")
    private String context;

    /** The final AI-generated or AI-optimized prompt text. */
    @Field("generated_prompt")
    private String generatedPrompt;

    /**
     * Whether this record is explicitly saved to the user's Prompt Library.
     * False means it only shows up in Prompt History and can be deleted
     * freely; true means the user has chosen to keep it long-term.
     */
    @Field("is_saved")
    @Builder.Default
    private boolean saved = false;

    /** Whether the user has marked this prompt as a favorite. */
    @Field("is_favorite")
    @Builder.Default
    private boolean favorite = false;

    /** Free-text tags for organizing/searching prompts in the Library. */
    @Field("tags")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    /** ID of the PromptCollection this prompt belongs to, if any. */
    @Field("collection_id")
    private String collectionId;

    /** Analysis scores, populated only when action == ANALYZE. */
    @Field("analysis")
    private AnalysisResult analysis;

    /** Battle Arena comparison results, populated only when action == BATTLE. */
    @Field("battle")
    private BattleResult battle;

    /** Optional reference to a vector embedding ID stored in ChromaDB. */
    @Field("embedding_id")
    private String embeddingId;

    /** Timestamp when this record was created (auto-populated). */
    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    /** Timestamp when this record was last updated (auto-populated). */
    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;

    /**
     * Nested result object holding Prompt Analyzer scores and feedback.
     * Embedded directly in the Prompt document rather than a separate
     * collection, since an analysis result has no independent lifecycle.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalysisResult {

        /** Grammar quality score, 0-100. */
        private int grammarScore;

        /** Clarity of instructions score, 0-100. */
        private int clarityScore;

        /** Sufficiency of provided context score, 0-100. */
        private int contextScore;

        /** Estimated risk of the prompt causing model hallucination, 0-100 (lower is better). */
        private int hallucinationRisk;

        /** Structural/linguistic complexity score, 0-100. */
        private int complexityScore;

        /** Weighted overall prompt quality score, 0-100. */
        private int overallScore;

        /** List of actionable improvement suggestions. */
        @Builder.Default
        private List<String> suggestions = new ArrayList<>();
    }

    /**
     * Nested result object holding Prompt Battle Arena comparison data:
     * each competing prompt style, its AI-generated output, and which
     * one was recommended as the best performer.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BattleResult {

        /** The competing prompt variants and their outputs. */
        @Builder.Default
        private List<BattleEntry> entries = new ArrayList<>();

        /** The PromptType judged to have produced the best output. */
        private PromptType recommendedType;

        /** Explanation of why the recommended type was chosen. */
        private String recommendationReason;

        /**
         * Represents a single competing prompt style within a Battle Arena run.
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class BattleEntry {

            /** The prompt engineering style used for this entry. */
            private PromptType promptType;

            /** The generated prompt text for this style. */
            private String promptText;

            /** The AI's output when this prompt was executed against Gemini. */
            private String aiOutput;

            /** A quality score assigned to this entry for comparison, 0-100. */
            private int score;
        }
    }
}