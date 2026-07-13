package com.promptstudio.controller;

import com.promptstudio.dto.CommonDTOs.ApiResponse;
import com.promptstudio.dto.CommonDTOs.PageResponse;
import com.promptstudio.dto.PromptDTOs.*;
import com.promptstudio.enums.PromptAction;
import com.promptstudio.security.CustomUserDetailsService.UserPrincipal;
import com.promptstudio.service.PromptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing every prompt-related feature: Generator,
 * Optimizer, Analyzer, Battle Arena, Library, History, and Collections.
 * These are grouped into a single controller since they all operate on
 * the same underlying Prompt resource, mirroring the consolidated
 * PromptService beneath it. Every endpoint is scoped to the
 * authenticated user via the injected UserPrincipal.
 */
@RestController
@RequestMapping("/api/prompts")
@RequiredArgsConstructor
@Tag(name = "Prompts", description = "Prompt Generator, Optimizer, Analyzer, Battle Arena, Library, History, and Collections")
public class PromptController {

    private final PromptService promptService;

    // ==================== GENERATOR ====================

    /**
     * Generates a new prompt in the requested engineering style.
     *
     * @param principal the authenticated user
     * @param request   the generation request payload
     * @return 201 CREATED with the generated PromptResponse
     */
    @PostMapping("/generate")
    @Operation(summary = "Generate a new prompt in a chosen style")
    public ResponseEntity<ApiResponse<PromptResponse>> generatePrompt(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody GenerateRequest request
    ) {
        PromptResponse response = promptService.generatePrompt(principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Prompt generated successfully", response));
    }

    // ==================== OPTIMIZER ====================

    /**
     * Improves an existing prompt's clarity and structure.
     *
     * @param principal the authenticated user
     * @param request   the optimization request payload
     * @return 201 CREATED with the optimized PromptResponse
     */
    @PostMapping("/optimize")
    @Operation(summary = "Optimize and improve an existing prompt")
    public ResponseEntity<ApiResponse<PromptResponse>> optimizePrompt(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody OptimizeRequest request
    ) {
        PromptResponse response = promptService.optimizePrompt(principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Prompt optimized successfully", response));
    }

    // ==================== ANALYZER ====================

    /**
     * Analyzes a prompt's quality across grammar, clarity, context,
     * hallucination risk, and complexity dimensions.
     *
     * @param principal the authenticated user
     * @param request   the analysis request payload
     * @return 201 CREATED with the analyzed PromptResponse
     */
    @PostMapping("/analyze")
    @Operation(summary = "Analyze and score a prompt's quality")
    public ResponseEntity<ApiResponse<PromptResponse>> analyzePrompt(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AnalyzeRequest request
    ) {
        PromptResponse response = promptService.analyzePrompt(principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Prompt analyzed successfully", response));
    }

    // ==================== BATTLE ARENA ====================

    /**
     * Runs a Battle Arena comparison across multiple prompt styles for
     * the same task, executing each against Gemini and recommending
     * the best performer.
     *
     * @param principal the authenticated user
     * @param request   the battle request payload
     * @return 201 CREATED with the battle PromptResponse
     */
    @PostMapping("/battle")
    @Operation(summary = "Run a Battle Arena comparison across prompt styles")
    public ResponseEntity<ApiResponse<PromptResponse>> runBattle(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody BattleRequest request
    ) {
        PromptResponse response = promptService.runBattle(principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Battle arena comparison complete", response));
    }

    // ==================== LIBRARY ====================

    /**
     * Saves a new prompt (or manually authored text) directly to the
     * Prompt Library.
     *
     * @param principal the authenticated user
     * @param request   the save request payload
     * @return 201 CREATED with the saved PromptResponse
     */
    @PostMapping("/library")
    @Operation(summary = "Save a new prompt to the library")
    public ResponseEntity<ApiResponse<PromptResponse>> savePrompt(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SaveRequest request
    ) {
        PromptResponse response = promptService.savePrompt(principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Prompt saved to library", response));
    }

    /**
     * Marks an existing prompt record (e.g., a Generator or Optimizer
     * result) as saved to the Library, without creating a duplicate.
     *
     * @param principal the authenticated user
     * @param promptId  the ID of the existing prompt record to save
     * @return 200 OK with the updated PromptResponse
     */
    @PatchMapping("/{promptId}/save")
    @Operation(summary = "Save an existing history record to the library")
    public ResponseEntity<ApiResponse<PromptResponse>> saveExistingToLibrary(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String promptId
    ) {
        PromptResponse response = promptService.saveExistingToLibrary(principal.getUserId(), promptId);
        return ResponseEntity.ok(ApiResponse.success("Prompt saved to library", response));
    }

    /**
     * Retrieves a paginated list of the user's saved Library prompts.
     *
     * @param principal the authenticated user
     * @param page      zero-based page number, defaults to 0
     * @param size      page size, defaults to 20
     * @return 200 OK with a PageResponse of PromptResponse
     */
    @GetMapping("/library")
    @Operation(summary = "List all saved prompts in the library")
    public ResponseEntity<ApiResponse<PageResponse<PromptResponse>>> getLibrary(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<PromptResponse> result = promptService.getLibrary(principal.getUserId(), page, size);
        return ResponseEntity.ok(ApiResponse.success("Library retrieved", toPageResponse(result)));
    }

    /**
     * Searches the user's saved Library prompts by keyword.
     *
     * @param principal the authenticated user
     * @param keyword   the search keyword
     * @param page      zero-based page number, defaults to 0
     * @param size      page size, defaults to 20
     * @return 200 OK with a PageResponse of matching PromptResponse
     */
    @GetMapping("/library/search")
    @Operation(summary = "Search saved prompts in the library")
    public ResponseEntity<ApiResponse<PageResponse<PromptResponse>>> searchLibrary(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<PromptResponse> result = promptService.searchLibrary(principal.getUserId(), keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success("Search results retrieved", toPageResponse(result)));
    }

    /**
     * Retrieves a single prompt record by ID.
     *
     * @param principal the authenticated user
     * @param promptId  the ID of the prompt to retrieve
     * @return 200 OK with the PromptResponse
     */
    @GetMapping("/{promptId}")
    @Operation(summary = "Get a single prompt by ID")
    public ResponseEntity<ApiResponse<PromptResponse>> getPromptById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String promptId
    ) {
        PromptResponse response = promptService.getPromptById(principal.getUserId(), promptId);
        return ResponseEntity.ok(ApiResponse.success("Prompt retrieved", response));
    }

    /**
     * Updates an existing saved prompt's editable fields.
     *
     * @param principal the authenticated user
     * @param promptId  the ID of the prompt to update
     * @param request   the updated field values
     * @return 200 OK with the updated PromptResponse
     */
    @PutMapping("/{promptId}")
    @Operation(summary = "Update a saved prompt")
    public ResponseEntity<ApiResponse<PromptResponse>> updatePrompt(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String promptId,
            @Valid @RequestBody UpdateRequest request
    ) {
        PromptResponse response = promptService.updatePrompt(principal.getUserId(), promptId, request);
        return ResponseEntity.ok(ApiResponse.success("Prompt updated successfully", response));
    }

    /**
     * Toggles a prompt's favorite status.
     *
     * @param principal the authenticated user
     * @param promptId  the ID of the prompt to toggle
     * @return 200 OK with the updated PromptResponse
     */
    @PatchMapping("/{promptId}/favorite")
    @Operation(summary = "Toggle a prompt's favorite status")
    public ResponseEntity<ApiResponse<PromptResponse>> toggleFavorite(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String promptId
    ) {
        PromptResponse response = promptService.toggleFavorite(principal.getUserId(), promptId);
        String message = response.favorite() ? "Prompt marked as favorite" : "Prompt removed from favorites";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    /**
     * Retrieves a paginated list of the user's favorited prompts.
     *
     * @param principal the authenticated user
     * @param page      zero-based page number, defaults to 0
     * @param size      page size, defaults to 20
     * @return 200 OK with a PageResponse of PromptResponse
     */
    @GetMapping("/favorites")
    @Operation(summary = "List favorited prompts")
    public ResponseEntity<ApiResponse<PageResponse<PromptResponse>>> getFavorites(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<PromptResponse> result = promptService.getFavorites(principal.getUserId(), page, size);
        return ResponseEntity.ok(ApiResponse.success("Favorites retrieved", toPageResponse(result)));
    }

    /**
     * Permanently deletes a prompt record.
     *
     * @param principal the authenticated user
     * @param promptId  the ID of the prompt to delete
     * @return 200 OK with no data payload
     */
    @DeleteMapping("/{promptId}")
    @Operation(summary = "Delete a prompt permanently")
    public ResponseEntity<ApiResponse<Void>> deletePrompt(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String promptId
    ) {
        promptService.deletePrompt(principal.getUserId(), promptId);
        return ResponseEntity.ok(ApiResponse.success("Prompt deleted successfully"));
    }

    // ==================== HISTORY ====================

    /**
     * Retrieves a paginated list of all the user's prompt records
     * (saved and unsaved), most recent first.
     *
     * @param principal the authenticated user
     * @param page      zero-based page number, defaults to 0
     * @param size      page size, defaults to 20
     * @return 200 OK with a PageResponse of PromptResponse
     */
    @GetMapping("/history")
    @Operation(summary = "List full prompt history")
    public ResponseEntity<ApiResponse<PageResponse<PromptResponse>>> getHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<PromptResponse> result = promptService.getHistory(principal.getUserId(), page, size);
        return ResponseEntity.ok(ApiResponse.success("History retrieved", toPageResponse(result)));
    }

    /**
     * Retrieves a paginated list of the user's history filtered to a
     * specific feature/action type.
     *
     * @param principal the authenticated user
     * @param action    the action type to filter by ("GENERATE", "OPTIMIZE", "ANALYZE", "BATTLE", "MANUAL")
     * @param page      zero-based page number, defaults to 0
     * @param size      page size, defaults to 20
     * @return 200 OK with a PageResponse of matching PromptResponse
     */
    @GetMapping("/history/{action}")
    @Operation(summary = "List prompt history filtered by feature type")
    public ResponseEntity<ApiResponse<PageResponse<PromptResponse>>> getHistoryByAction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PromptAction promptAction = PromptAction.valueOf(action.trim().toUpperCase());
        Page<PromptResponse> result = promptService.getHistoryByAction(principal.getUserId(), promptAction, page, size);
        return ResponseEntity.ok(ApiResponse.success("History retrieved", toPageResponse(result)));
    }

    /**
     * Clears all of the user's unsaved history records, leaving
     * Library-saved prompts untouched.
     *
     * @param principal the authenticated user
     * @return 200 OK with no data payload
     */
    @DeleteMapping("/history")
    @Operation(summary = "Clear all unsaved history records")
    public ResponseEntity<ApiResponse<Void>> clearHistory(@AuthenticationPrincipal UserPrincipal principal) {
        promptService.clearHistory(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("History cleared successfully"));
    }

    // ==================== COLLECTIONS ====================

    /**
     * Creates a new Prompt Collection.
     *
     * @param principal the authenticated user
     * @param request   the collection creation payload
     * @return 201 CREATED with the new CollectionResponse
     */
    @PostMapping("/collections")
    @Operation(summary = "Create a new prompt collection")
    public ResponseEntity<ApiResponse<CollectionResponse>> createCollection(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CollectionRequest request
    ) {
        CollectionResponse response = promptService.createCollection(principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Collection created successfully", response));
    }

    /**
     * Lists all collections belonging to the user.
     *
     * @param principal the authenticated user
     * @return 200 OK with the list of CollectionResponse
     */
    @GetMapping("/collections")
    @Operation(summary = "List all prompt collections")
    public ResponseEntity<ApiResponse<List<CollectionResponse>>> getCollections(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<CollectionResponse> response = promptService.getCollections(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Collections retrieved", response));
    }

    /**
     * Updates an existing collection's name, description, or color.
     *
     * @param principal    the authenticated user
     * @param collectionId the ID of the collection to update
     * @param request      the updated field values
     * @return 200 OK with the updated CollectionResponse
     */
    @PutMapping("/collections/{collectionId}")
    @Operation(summary = "Update a prompt collection")
    public ResponseEntity<ApiResponse<CollectionResponse>> updateCollection(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String collectionId,
            @Valid @RequestBody CollectionRequest request
    ) {
        CollectionResponse response = promptService.updateCollection(principal.getUserId(), collectionId, request);
        return ResponseEntity.ok(ApiResponse.success("Collection updated successfully", response));
    }

    /**
     * Deletes a collection. Prompts within it are unlinked, not deleted.
     *
     * @param principal    the authenticated user
     * @param collectionId the ID of the collection to delete
     * @return 200 OK with no data payload
     */
    @DeleteMapping("/collections/{collectionId}")
    @Operation(summary = "Delete a prompt collection")
    public ResponseEntity<ApiResponse<Void>> deleteCollection(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String collectionId
    ) {
        promptService.deleteCollection(principal.getUserId(), collectionId);
        return ResponseEntity.ok(ApiResponse.success("Collection deleted successfully"));
    }

    /**
     * Adds a prompt to a collection.
     *
     * @param principal    the authenticated user
     * @param collectionId the ID of the target collection
     * @param promptId     the ID of the prompt to add
     * @return 200 OK with the updated CollectionResponse
     */
    @PostMapping("/collections/{collectionId}/prompts/{promptId}")
    @Operation(summary = "Add a prompt to a collection")
    public ResponseEntity<ApiResponse<CollectionResponse>> addPromptToCollection(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String collectionId,
            @PathVariable String promptId
    ) {
        CollectionResponse response = promptService.addPromptToCollection(principal.getUserId(), collectionId, promptId);
        return ResponseEntity.ok(ApiResponse.success("Prompt added to collection", response));
    }

    /**
     * Removes a prompt from a collection.
     *
     * @param principal    the authenticated user
     * @param collectionId the ID of the target collection
     * @param promptId     the ID of the prompt to remove
     * @return 200 OK with the updated CollectionResponse
     */
    @DeleteMapping("/collections/{collectionId}/prompts/{promptId}")
    @Operation(summary = "Remove a prompt from a collection")
    public ResponseEntity<ApiResponse<CollectionResponse>> removePromptFromCollection(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String collectionId,
            @PathVariable String promptId
    ) {
        CollectionResponse response = promptService.removePromptFromCollection(principal.getUserId(), collectionId, promptId);
        return ResponseEntity.ok(ApiResponse.success("Prompt removed from collection", response));
    }

    /**
     * Retrieves all prompts belonging to a specific collection.
     *
     * @param principal    the authenticated user
     * @param collectionId the ID of the collection
     * @return 200 OK with the list of PromptResponse in that collection
     */
    @GetMapping("/collections/{collectionId}/prompts")
    @Operation(summary = "List all prompts in a collection")
    public ResponseEntity<ApiResponse<List<PromptResponse>>> getPromptsInCollection(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String collectionId
    ) {
        List<PromptResponse> response = promptService.getPromptsInCollection(principal.getUserId(), collectionId);
        return ResponseEntity.ok(ApiResponse.success("Collection prompts retrieved", response));
    }

    // ==================== SHARED HELPER ====================

    /**
     * Converts a Spring Data {@link Page} into our API-facing
     * {@link PageResponse} DTO, keeping pagination metadata consistent
     * across every paginated endpoint in this controller.
     *
     * @param page the Spring Data page to convert
     * @param <T>  the type of content in the page
     * @return the mapped PageResponse
     */
    private <T> PageResponse<T> toPageResponse(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}