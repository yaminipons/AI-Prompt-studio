package com.promptstudio.dto;

import com.promptstudio.enums.PromptType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Container for all Prompt-related request and response DTOs, covering
 * the Prompt Generator, Optimizer, Analyzer, Battle Arena, Library,
 * History, and Collections features.
 */
public final class PromptDTOs {

    private PromptDTOs() {
    }

    // ==================== GENERATOR ====================

    public record GenerateRequest(

            @NotBlank(message = "Task description is required")
            @Size(max = 2000, message = "Task description must not exceed 2000 characters")
            String task,

            @NotNull(message = "Prompt type is required")
            PromptType promptType,

            @Size(max = 1000, message = "Context must not exceed 1000 characters")
            String context
    ) {
    }

    // ==================== OPTIMIZER ====================

    public record OptimizeRequest(

            @NotBlank(message = "Original prompt is required")
            @Size(max = 3000, message = "Prompt must not exceed 3000 characters")
            String originalPrompt
    ) {
    }

    // ==================== ANALYZER ====================

    public record AnalyzeRequest(

            @NotBlank(message = "Prompt text is required")
            @Size(max = 3000, message = "Prompt must not exceed 3000 characters")
            String promptText
    ) {
    }

    /**
     * Response payload containing Prompt Analyzer scores and suggestions.
     * Mirrors the structure of {@code Prompt.AnalysisResult}.
     */
    public record AnalysisResponse(
            int grammarScore,
            int clarityScore,
            int contextScore,
            int hallucinationRisk,
            int complexityScore,
            int overallScore,
            List<String> suggestions
    ) {
    }

    // ==================== BATTLE ARENA ====================

    public record BattleRequest(

            @NotBlank(message = "Task description is required")
            @Size(max = 2000, message = "Task description must not exceed 2000 characters")
            String task,

            @Size(max = 1000, message = "Context must not exceed 1000 characters")
            String context,

            List<PromptType> styles
    ) {
    }

    /**
     * Represents one competing prompt style's result within a Battle
     * Arena run, for frontend side-by-side display.
     */
    public record BattleEntryResponse(
            PromptType promptType,
            String promptText,
            String aiOutput,
            int score
    ) {
    }

    /**
     * Response payload containing the full Battle Arena comparison result.
     */
    public record BattleResponse(
            List<BattleEntryResponse> entries,
            PromptType recommendedType,
            String recommendationReason
    ) {
    }

    // ==================== LIBRARY / HISTORY (SHARED RESPONSE) ====================

    /**
     * Full response representation of a single Prompt record, used
     * across the Generator result view, Library, and History listings.
     */
    public record PromptResponse(
            String id,
            String title,
            String action,
            String promptType,
            String originalInput,
            String context,
            String generatedPrompt,
            boolean saved,
            boolean favorite,
            List<String> tags,
            String collectionId,
            AnalysisResponse analysis,
            BattleResponse battle,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record SaveRequest(

            @NotBlank(message = "Title is required")
            @Size(max = 150, message = "Title must not exceed 150 characters")
            String title,

            @NotBlank(message = "Original input is required")
            String originalInput,

            @NotBlank(message = "Generated prompt is required")
            String generatedPrompt,

            PromptType promptType,

            List<String> tags,

            String collectionId
    ) {
    }

    public record UpdateRequest(

            @NotBlank(message = "Title is required")
            @Size(max = 150, message = "Title must not exceed 150 characters")
            String title,

            @NotBlank(message = "Generated prompt is required")
            String generatedPrompt,

            List<String> tags,

            String collectionId,

            boolean favorite
    ) {
    }

    // ==================== COLLECTIONS ====================

    public record CollectionRequest(

            @NotBlank(message = "Collection name is required")
            @Size(max = 100, message = "Collection name must not exceed 100 characters")
            String name,

            @Size(max = 300, message = "Description must not exceed 300 characters")
            String description,

            String color
    ) {
    }

    public record CollectionResponse(
            String id,
            String name,
            String description,
            String color,
            int promptCount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    // ==================== EXPORT ====================

    public record ExportRequest(

            @NotBlank(message = "Prompt ID is required")
            String promptId,

            @NotBlank(message = "Export format is required")
            String format
    ) {
    }
}