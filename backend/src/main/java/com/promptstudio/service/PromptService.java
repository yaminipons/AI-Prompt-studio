package com.promptstudio.service;

import com.promptstudio.dto.PromptDTOs.*;
import com.promptstudio.entity.Prompt;
import com.promptstudio.entity.PromptCollection;
import com.promptstudio.enums.PromptAction;
import com.promptstudio.enums.PromptType;
import com.promptstudio.exception.ApiException;
import com.promptstudio.repository.PromptCollectionRepository;
import com.promptstudio.repository.PromptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Core service implementing every prompt-related feature: Generator,
 * Optimizer, Analyzer, Battle Arena, Library, History, and Collections.
 * These are grouped into a single service because they all operate on
 * the same underlying {@link Prompt} entity and share persistence,
 * ownership-validation, and mapping logic.
 */
@Service
@RequiredArgsConstructor
public class PromptService {

    private final PromptRepository promptRepository;
    private final PromptCollectionRepository collectionRepository;
    private final AiService aiService;

    private static final Pattern SENTENCE_SPLIT = Pattern.compile("(?<=[.!?])\\s+");

    // ==================== PROMPT GENERATOR ====================

    /**
     * Generates a new prompt in the requested style for the given task,
     * using a hand-crafted template combined with a Gemini refinement
     * pass, then persists the result as a History record (unsaved by
     * default) tied to the requesting user.
     *
     * @param userId  the requesting user's ID
     * @param request the generation request (task, style, context)
     * @return the created PromptResponse
     */
    public PromptResponse generatePrompt(String userId, GenerateRequest request) {
        String template = buildTemplate(request.promptType(), request.task(), request.context());

        String metaInstruction = "You are an expert prompt engineer. Refine and finalize the following "
                + request.promptType().getDisplayName() + " so it is clear, complete, and ready to use "
                + "with a large language model. Return ONLY the final prompt text, with no preamble, "
                + "no explanation, and no markdown formatting.\n\nDRAFT PROMPT:\n" + template;

        String finalPrompt = aiService.generateContent(metaInstruction);

        Prompt prompt = Prompt.builder()
                .userId(userId)
                .title(buildTitle(request.task()))
                .action(PromptAction.GENERATE)
                .promptType(request.promptType())
                .originalInput(request.task())
                .context(request.context())
                .generatedPrompt(finalPrompt)
                .saved(false)
                .favorite(false)
                .tags(new ArrayList<>())
                .build();

        Prompt saved = promptRepository.save(prompt);
        return mapToResponse(saved);
    }

    /**
     * Builds a hand-crafted draft prompt template for the given style,
     * task, and optional context. This draft is then refined by Gemini
     * in {@link #generatePrompt} to ensure grammatical correctness and
     * natural phrasing while preserving the intended technique.
     *
     * @param type    the prompt engineering style to apply
     * @param task    the task/topic to build a prompt for
     * @param context optional additional context
     * @return the draft template string
     */
    private String buildTemplate(PromptType type, String task, String context) {
        String contextLine = (context != null && !context.isBlank())
                ? "\nAdditional context: " + context
                : "";

        return switch (type) {
            case ZERO_SHOT -> "Perform the following task directly without any examples.\nTask: "
                    + task + contextLine;

            case FEW_SHOT -> "Perform the following task. Here are two examples to guide your response format:\n"
                    + "Example 1: [demonstrate a typical input and ideal output for this task]\n"
                    + "Example 2: [demonstrate a second input and ideal output for this task]\n"
                    + "Now perform the task.\nTask: " + task + contextLine;

            case CHAIN_OF_THOUGHT -> "Think through this task step-by-step, showing your reasoning before "
                    + "giving the final answer.\nTask: " + task + contextLine
                    + "\nFirst, reason through the problem. Then provide your final answer clearly labeled.";

            case ROLE_BASED -> "You are an expert professional highly experienced in the relevant field for "
                    + "this task. Respond with the depth, tone, and authority of that expert.\nTask: "
                    + task + contextLine;

            case STEP_BY_STEP -> "Break down and complete the following task as a clear numbered sequence "
                    + "of steps.\nTask: " + task + contextLine
                    + "\nList each step explicitly before arriving at the final result.";

            case INSTRUCTION -> "Follow these instructions precisely to complete the task below. Define the "
                    + "expected output format and any constraints clearly.\nTask: " + task + contextLine
                    + "\nOutput format: provide a clear, well-structured response matching the task's intent.";
        };
    }

    /**
     * Derives a short, human-readable title from a longer task/input
     * string, truncating to the first sentence or a maximum length.
     *
     * @param task the raw task/input text
     * @return a shortened display title
     */
    private String buildTitle(String task) {
        String trimmed = task.trim();
        String[] sentences = SENTENCE_SPLIT.split(trimmed);
        String candidate = sentences.length > 0 ? sentences[0] : trimmed;
        return candidate.length() > 80 ? candidate.substring(0, 80).trim() + "..." : candidate;
    }

    // ==================== PROMPT OPTIMIZER ====================

    /**
     * Improves an existing prompt's clarity, specificity, and structure
     * using Gemini, then persists the result as a History record.
     *
     * @param userId  the requesting user's ID
     * @param request the optimization request containing the original prompt
     * @return the created PromptResponse
     */
    public PromptResponse optimizePrompt(String userId, OptimizeRequest request) {
        String instruction = "You are an expert prompt engineer. Rewrite and improve the following prompt "
                + "to make it clearer, more specific, better structured, and more likely to produce a "
                + "high-quality response from an AI model. Preserve the original intent. Return ONLY the "
                + "improved prompt text, with no preamble, no explanation, and no markdown formatting.\n\n"
                + "ORIGINAL PROMPT:\n" + request.originalPrompt();

        String optimized = aiService.generateContent(instruction);

        Prompt prompt = Prompt.builder()
                .userId(userId)
                .title(buildTitle(request.originalPrompt()))
                .action(PromptAction.OPTIMIZE)
                .originalInput(request.originalPrompt())
                .generatedPrompt(optimized)
                .saved(false)
                .favorite(false)
                .tags(new ArrayList<>())
                .build();

        Prompt saved = promptRepository.save(prompt);
        return mapToResponse(saved);
    }

    // ==================== PROMPT ANALYZER ====================

    /**
     * Analyzes a prompt's quality across five dimensions (grammar,
     * clarity, context sufficiency, hallucination risk, complexity)
     * using a structured Gemini call that returns parseable scores,
     * then persists the result as a History record.
     *
     * @param userId  the requesting user's ID
     * @param request the analysis request containing the prompt text
     * @return the created PromptResponse with populated analysis field
     */
    public PromptResponse analyzePrompt(String userId, AnalyzeRequest request) {
        String instruction = buildAnalysisInstruction(request.promptText());
        String rawResponse = aiService.generateContent(instruction);

        Prompt.AnalysisResult analysis = parseAnalysisResponse(rawResponse);

        Prompt prompt = Prompt.builder()
                .userId(userId)
                .title(buildTitle(request.promptText()))
                .action(PromptAction.ANALYZE)
                .originalInput(request.promptText())
                .generatedPrompt(request.promptText())
                .analysis(analysis)
                .saved(false)
                .favorite(false)
                .tags(new ArrayList<>())
                .build();

        Prompt saved = promptRepository.save(prompt);
        return mapToResponse(saved);
    }

    /**
     * Builds the instruction sent to Gemini for the Analyzer feature,
     * requesting a strict, parseable output format so scores can be
     * reliably extracted with {@link #parseAnalysisResponse}.
     *
     * @param promptText the prompt text to analyze
     * @return the full instruction string
     */
    private String buildAnalysisInstruction(String promptText) {
        return "You are an expert prompt engineering evaluator. Analyze the following prompt and score it "
                + "on five dimensions, each from 0 to 100. Respond in EXACTLY this format with no extra text, "
                + "no markdown, one field per line:\n"
                + "GRAMMAR: <score>\n"
                + "CLARITY: <score>\n"
                + "CONTEXT: <score>\n"
                + "HALLUCINATION_RISK: <score>\n"
                + "COMPLEXITY: <score>\n"
                + "SUGGESTION_1: <one specific actionable suggestion>\n"
                + "SUGGESTION_2: <one specific actionable suggestion>\n"
                + "SUGGESTION_3: <one specific actionable suggestion>\n\n"
                + "Definitions:\n"
                + "- GRAMMAR: correctness of grammar and phrasing.\n"
                + "- CLARITY: how unambiguous and easy to follow the instructions are.\n"
                + "- CONTEXT: whether sufficient context/constraints are provided.\n"
                + "- HALLUCINATION_RISK: likelihood this prompt causes an AI to invent false information (higher score = higher risk).\n"
                + "- COMPLEXITY: structural/linguistic complexity of the prompt.\n\n"
                + "PROMPT TO ANALYZE:\n" + promptText;
    }

    /**
     * Parses Gemini's structured analysis response into an
     * {@link Prompt.AnalysisResult}, extracting each labeled score and
     * suggestion line via regex. Falls back to sensible defaults for
     * any field that fails to parse, so a single formatting slip from
     * the AI never causes the whole analysis to fail.
     *
     * @param rawResponse the raw text response from Gemini
     * @return the populated AnalysisResult
     */
    private Prompt.AnalysisResult parseAnalysisResponse(String rawResponse) {
        int grammar = extractScore(rawResponse, "GRAMMAR");
        int clarity = extractScore(rawResponse, "CLARITY");
        int context = extractScore(rawResponse, "CONTEXT");
        int hallucinationRisk = extractScore(rawResponse, "HALLUCINATION_RISK");
        int complexity = extractScore(rawResponse, "COMPLEXITY");

        List<String> suggestions = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            String suggestion = extractText(rawResponse, "SUGGESTION_" + i);
            if (suggestion != null && !suggestion.isBlank()) {
                suggestions.add(suggestion);
            }
        }
        if (suggestions.isEmpty()) {
            suggestions.add("Consider adding more specific constraints or examples to improve output quality.");
        }

        int overall = (int) Math.round(
                (grammar * 0.2) + (clarity * 0.3) + (context * 0.25)
                        + ((100 - hallucinationRisk) * 0.15) + (complexity * 0.10)
        );

        return Prompt.AnalysisResult.builder()
                .grammarScore(grammar)
                .clarityScore(clarity)
                .contextScore(context)
                .hallucinationRisk(hallucinationRisk)
                .complexityScore(complexity)
                .overallScore(overall)
                .suggestions(suggestions)
                .build();
    }

    /**
     * Extracts an integer score following a labeled field name in the
     * format "LABEL: <number>" from raw text. Returns 50 (a neutral
     * default) if the label is not found or the value isn't parseable.
     *
     * @param text  the raw text to search
     * @param label the field label to look for
     * @return the extracted score, clamped to 0-100
     */
    private int extractScore(String text, String label) {
        Pattern pattern = Pattern.compile(label + "\\s*:\\s*(\\d{1,3})");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            return Math.max(0, Math.min(100, value));
        }
        return 50;
    }

    /**
     * Extracts the free-text value following a labeled field name in
     * the format "LABEL: <text>" from raw text, reading until the end
     * of that line.
     *
     * @param text  the raw text to search
     * @param label the field label to look for
     * @return the extracted text, or null if the label is not found
     */
    private String extractText(String text, String label) {
        Pattern pattern = Pattern.compile(label + "\\s*:\\s*(.+)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    // ==================== PROMPT BATTLE ARENA ====================

    /**
     * Generates competing prompt styles for the given task, executes
     * each one against Gemini in parallel, scores each output, and
     * recommends the best-performing style. Persists the full
     * comparison as a single History record.
     *
     * @param userId  the requesting user's ID
     * @param request the battle request (task, context, optional style subset)
     * @return the created PromptResponse with populated battle field
     */
    public PromptResponse runBattle(String userId, BattleRequest request) {
        List<PromptType> styles = (request.styles() != null && !request.styles().isEmpty())
                ? request.styles()
                : List.of(PromptType.values());

        List<CompletableFuture<Prompt.BattleResult.BattleEntry>> futures = styles.stream()
                .map(style -> CompletableFuture.supplyAsync(() -> runSingleBattleEntry(style, request.task(), request.context())))
                .toList();

        List<Prompt.BattleResult.BattleEntry> entries = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        Prompt.BattleResult.BattleEntry winner = entries.stream()
                .max((a, b) -> Integer.compare(a.getScore(), b.getScore()))
                .orElseThrow(() -> new ApiException("Battle arena produced no results", HttpStatus.INTERNAL_SERVER_ERROR));

        String reason = "The " + winner.getPromptType().getDisplayName() + " scored highest ("
                + winner.getScore() + "/100) due to producing the clearest, most complete, and most "
                + "directly usable output among the styles compared.";

        Prompt.BattleResult battleResult = Prompt.BattleResult.builder()
                .entries(entries)
                .recommendedType(winner.getPromptType())
                .recommendationReason(reason)
                .build();

        Prompt prompt = Prompt.builder()
                .userId(userId)
                .title(buildTitle(request.task()))
                .action(PromptAction.BATTLE)
                .originalInput(request.task())
                .context(request.context())
                .battle(battleResult)
                .saved(false)
                .favorite(false)
                .tags(new ArrayList<>())
                .build();

        Prompt saved = promptRepository.save(prompt);
        return mapToResponse(saved);
    }

    /**
     * Generates and executes a single prompt style for a Battle Arena
     * run: builds the styled prompt text, sends it to Gemini for
     * execution, then requests a quality score for the resulting output.
     *
     * @param style   the prompt style to generate and test
     * @param task    the task/topic being compared
     * @param context optional additional context
     * @return the completed BattleEntry for this style
     */
    private Prompt.BattleResult.BattleEntry runSingleBattleEntry(PromptType style, String task, String context) {
        String promptText = buildTemplate(style, task, context);
        String aiOutput = aiService.generateContent(promptText);
        int score = scoreOutput(promptText, aiOutput);

        return Prompt.BattleResult.BattleEntry.builder()
                .promptType(style)
                .promptText(promptText)
                .aiOutput(aiOutput)
                .score(score)
                .build();
    }

    /**
     * Requests a numeric quality score (0-100) from Gemini for a given
     * prompt/output pair, used to rank Battle Arena entries. Falls back
     * to a length-based heuristic score if Gemini's response cannot be
     * parsed as a number.
     *
     * @param promptText the prompt that was executed
     * @param aiOutput   the output it produced
     * @return a score between 0 and 100
     */
    private int scoreOutput(String promptText, String aiOutput) {
        String instruction = "Rate the quality of the following AI output on a scale of 0 to 100, considering "
                + "how well it fulfills the prompt's intent, completeness, and clarity. Respond with ONLY the "
                + "number, nothing else.\n\nPROMPT:\n" + promptText + "\n\nOUTPUT:\n" + aiOutput;

        try {
            String response = aiService.generateContent(instruction);
            Matcher matcher = Pattern.compile("(\\d{1,3})").matcher(response);
            if (matcher.find()) {
                int value = Integer.parseInt(matcher.group(1));
                return Math.max(0, Math.min(100, value));
            }
        } catch (Exception ignored) {
            // fall through to heuristic below
        }

        int lengthScore = Math.min(100, aiOutput.length() / 10);
        return Math.max(40, lengthScore);
    }

    // ==================== PROMPT LIBRARY ====================

    /**
     * Saves an existing generated prompt (or a manually authored one)
     * to the user's Prompt Library, marking it as saved and optionally
     * storing a semantic embedding for search.
     *
     * @param userId  the requesting user's ID
     * @param request the save request payload
     * @return the created PromptResponse
     */
    public PromptResponse savePrompt(String userId, SaveRequest request) {
        validateCollectionOwnership(userId, request.collectionId());

        Prompt prompt = Prompt.builder()
                .userId(userId)
                .title(request.title())
                .action(PromptAction.MANUAL)
                .promptType(request.promptType())
                .originalInput(request.originalInput())
                .generatedPrompt(request.generatedPrompt())
                .saved(true)
                .favorite(false)
                .tags(request.tags() != null ? request.tags() : new ArrayList<>())
                .collectionId(request.collectionId())
                .build();

        Prompt saved = promptRepository.save(prompt);

        aiService.storeEmbedding(saved.getId(), saved.getGeneratedPrompt(),
                Map.of("userId", userId, "title", saved.getTitle()));

        return mapToResponse(saved);
    }

    /**
     * Marks an already-existing prompt record (e.g., a Generator or
     * Optimizer result) as saved to the Library, without creating a
     * duplicate record.
     *
     * @param userId   the requesting user's ID
     * @param promptId the ID of the existing prompt record to save
     * @return the updated PromptResponse
     */
    public PromptResponse saveExistingToLibrary(String userId, String promptId) {
        Prompt prompt = findOwnedPromptOrThrow(promptId, userId);
        prompt.setSaved(true);
        Prompt saved = promptRepository.save(prompt);

        aiService.storeEmbedding(saved.getId(), saved.getGeneratedPrompt(),
                Map.of("userId", userId, "title", saved.getTitle()));

        return mapToResponse(saved);
    }

    /**
     * Retrieves a page of the user's saved Library prompts, most
     * recent first.
     *
     * @param userId the requesting user's ID
     * @param page   zero-based page number
     * @param size   page size
     * @return a page of PromptResponse
     */
    public Page<PromptResponse> getLibrary(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return promptRepository.findByUserIdAndSavedOrderByCreatedAtDesc(userId, true, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Searches the user's saved Library prompts by keyword, matching
     * against title, original input, or generated prompt text.
     *
     * @param userId  the requesting user's ID
     * @param keyword the search keyword
     * @param page    zero-based page number
     * @param size    page size
     * @return a page of matching PromptResponse
     */
    public Page<PromptResponse> searchLibrary(String userId, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return promptRepository.searchSavedPrompts(userId, keyword, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Updates an existing saved prompt's editable fields.
     *
     * @param userId   the requesting user's ID
     * @param promptId the ID of the prompt to update
     * @param request  the updated field values
     * @return the updated PromptResponse
     */
    public PromptResponse updatePrompt(String userId, String promptId, UpdateRequest request) {
        Prompt prompt = findOwnedPromptOrThrow(promptId, userId);
        validateCollectionOwnership(userId, request.collectionId());

        prompt.setTitle(request.title());
        prompt.setGeneratedPrompt(request.generatedPrompt());
        prompt.setTags(request.tags() != null ? request.tags() : new ArrayList<>());
        prompt.setCollectionId(request.collectionId());
        prompt.setFavorite(request.favorite());

        Prompt saved = promptRepository.save(prompt);
        return mapToResponse(saved);
    }

    /**
     * Toggles the favorite status of a prompt.
     *
     * @param userId   the requesting user's ID
     * @param promptId the ID of the prompt to toggle
     * @return the updated PromptResponse
     */
    public PromptResponse toggleFavorite(String userId, String promptId) {
        Prompt prompt = findOwnedPromptOrThrow(promptId, userId);
        prompt.setFavorite(!prompt.isFavorite());
        Prompt saved = promptRepository.save(prompt);
        return mapToResponse(saved);
    }

    /**
     * Retrieves a page of the user's favorited prompts.
     *
     * @param userId the requesting user's ID
     * @param page   zero-based page number
     * @param size   page size
     * @return a page of PromptResponse
     */
    public Page<PromptResponse> getFavorites(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return promptRepository.findByUserIdAndFavoriteOrderByCreatedAtDesc(userId, true, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Deletes a prompt record permanently, also removing its vector
     * embedding from ChromaDB if one exists.
     *
     * @param userId   the requesting user's ID
     * @param promptId the ID of the prompt to delete
     */
    public void deletePrompt(String userId, String promptId) {
        Prompt prompt = findOwnedPromptOrThrow(promptId, userId);
        promptRepository.deleteById(prompt.getId());
        aiService.deleteEmbedding(prompt.getId());
    }

    /**
     * Retrieves a single prompt record by ID, scoped to its owner.
     *
     * @param userId   the requesting user's ID
     * @param promptId the ID of the prompt to retrieve
     * @return the mapped PromptResponse
     */
    public PromptResponse getPromptById(String userId, String promptId) {
        Prompt prompt = findOwnedPromptOrThrow(promptId, userId);
        return mapToResponse(prompt);
    }

    // ==================== PROMPT HISTORY ====================

    /**
     * Retrieves a page of ALL prompt records (saved or not) for the
     * user, most recent first, powering the Prompt History view.
     *
     * @param userId the requesting user's ID
     * @param page   zero-based page number
     * @param size   page size
     * @return a page of PromptResponse
     */
    public Page<PromptResponse> getHistory(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return promptRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Retrieves a page of the user's history filtered to a specific
     * feature/action type (GENERATE, OPTIMIZE, ANALYZE, BATTLE, MANUAL).
     *
     * @param userId the requesting user's ID
     * @param action the action type to filter by
     * @param page   zero-based page number
     * @param size   page size
     * @return a page of PromptResponse
     */
    public Page<PromptResponse> getHistoryByAction(String userId, PromptAction action, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return promptRepository.findByUserIdAndActionOrderByCreatedAtDesc(userId, action, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Clears (deletes) all of the user's unsaved history records,
     * leaving Library-saved prompts untouched.
     *
     * @param userId the requesting user's ID
     */
    public void clearHistory(String userId) {
        Page<Prompt> unsaved = promptRepository.findByUserIdAndSavedOrderByCreatedAtDesc(
                userId, false, Pageable.unpaged()
        );
        promptRepository.deleteAll(unsaved.getContent());
    }

    // ==================== PROMPT COLLECTIONS ====================

    /**
     * Creates a new Prompt Collection for the user.
     *
     * @param userId  the requesting user's ID
     * @param request the collection name/description/color payload
     * @return the created CollectionResponse
     * @throws ApiException with 409 CONFLICT if a collection with this name already exists
     */
    public CollectionResponse createCollection(String userId, CollectionRequest request) {
        if (collectionRepository.existsByUserIdAndName(userId, request.name())) {
            throw new ApiException("A collection with this name already exists", HttpStatus.CONFLICT);
        }

        PromptCollection collection = PromptCollection.builder()
                .userId(userId)
                .name(request.name())
                .description(request.description())
                .color(request.color() != null && !request.color().isBlank() ? request.color() : "#6366F1")
                .promptIds(new ArrayList<>())
                .build();

        PromptCollection saved = collectionRepository.save(collection);
        return mapCollectionToResponse(saved);
    }

    /**
     * Retrieves all collections belonging to the user, most recently
     * updated first.
     *
     * @param userId the requesting user's ID
     * @return the list of CollectionResponse
     */
    public List<CollectionResponse> getCollections(String userId) {
        return collectionRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(this::mapCollectionToResponse)
                .toList();
    }

    /**
     * Updates an existing collection's editable fields.
     *
     * @param userId       the requesting user's ID
     * @param collectionId the ID of the collection to update
     * @param request      the updated field values
     * @return the updated CollectionResponse
     */
    public CollectionResponse updateCollection(String userId, String collectionId, CollectionRequest request) {
        PromptCollection collection = collectionRepository.findByIdAndUserId(collectionId, userId)
                .orElseThrow(() -> new ApiException("Collection not found", HttpStatus.NOT_FOUND));

        collection.setName(request.name());
        collection.setDescription(request.description());
        if (request.color() != null && !request.color().isBlank()) {
            collection.setColor(request.color());
        }

        PromptCollection saved = collectionRepository.save(collection);
        return mapCollectionToResponse(saved);
    }

    /**
     * Deletes a collection. Prompts that belonged to it are unlinked
     * (their collectionId is cleared) rather than deleted, so they
     * remain safely in the user's Library.
     *
     * @param userId       the requesting user's ID
     * @param collectionId the ID of the collection to delete
     */
    public void deleteCollection(String userId, String collectionId) {
        PromptCollection collection = collectionRepository.findByIdAndUserId(collectionId, userId)
                .orElseThrow(() -> new ApiException("Collection not found", HttpStatus.NOT_FOUND));

        List<Prompt> linkedPrompts = promptRepository.findByUserIdAndCollectionId(userId, collectionId);
        for (Prompt prompt : linkedPrompts) {
            prompt.setCollectionId(null);
        }
        promptRepository.saveAll(linkedPrompts);

        collectionRepository.deleteById(collection.getId());
    }

    /**
     * Adds a prompt to a collection, updating both the prompt's
     * collectionId and the collection's denormalized promptIds list.
     *
     * @param userId       the requesting user's ID
     * @param collectionId the ID of the target collection
     * @param promptId     the ID of the prompt to add
     * @return the updated CollectionResponse
     */
    public CollectionResponse addPromptToCollection(String userId, String collectionId, String promptId) {
        PromptCollection collection = collectionRepository.findByIdAndUserId(collectionId, userId)
                .orElseThrow(() -> new ApiException("Collection not found", HttpStatus.NOT_FOUND));

        Prompt prompt = findOwnedPromptOrThrow(promptId, userId);
        prompt.setCollectionId(collectionId);
        promptRepository.save(prompt);

        if (!collection.getPromptIds().contains(promptId)) {
            collection.getPromptIds().add(promptId);
            collectionRepository.save(collection);
        }

        return mapCollectionToResponse(collection);
    }

    /**
     * Removes a prompt from a collection, updating both the prompt's
     * collectionId and the collection's denormalized promptIds list.
     *
     * @param userId       the requesting user's ID
     * @param collectionId the ID of the target collection
     * @param promptId     the ID of the prompt to remove
     * @return the updated CollectionResponse
     */
    public CollectionResponse removePromptFromCollection(String userId, String collectionId, String promptId) {
        PromptCollection collection = collectionRepository.findByIdAndUserId(collectionId, userId)
                .orElseThrow(() -> new ApiException("Collection not found", HttpStatus.NOT_FOUND));

        Prompt prompt = findOwnedPromptOrThrow(promptId, userId);
        if (collectionId.equals(prompt.getCollectionId())) {
            prompt.setCollectionId(null);
            promptRepository.save(prompt);
        }

        collection.getPromptIds().remove(promptId);
        collectionRepository.save(collection);

        return mapCollectionToResponse(collection);
    }

    /**
     * Retrieves all prompts belonging to a specific collection.
     *
     * @param userId       the requesting user's ID
     * @param collectionId the ID of the collection
     * @return the list of PromptResponse in that collection
     */
    public List<PromptResponse> getPromptsInCollection(String userId, String collectionId) {
        collectionRepository.findByIdAndUserId(collectionId, userId)
                .orElseThrow(() -> new ApiException("Collection not found", HttpStatus.NOT_FOUND));

        return promptRepository.findByUserIdAndCollectionId(userId, collectionId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ==================== SHARED HELPERS ====================

    /**
     * Looks up a prompt by ID, ensuring it belongs to the given user.
     *
     * @param promptId the prompt ID to find
     * @param userId   the expected owning user's ID
     * @return the found Prompt entity
     * @throws ApiException with 404 NOT_FOUND if not found or not owned by this user
     */
    private Prompt findOwnedPromptOrThrow(String promptId, String userId) {
        return promptRepository.findByIdAndUserId(promptId, userId)
                .orElseThrow(() -> new ApiException("Prompt not found", HttpStatus.NOT_FOUND));
    }

    /**
     * Validates that a given collection ID, if provided, actually
     * belongs to the given user before it can be assigned to a prompt.
     *
     * @param userId       the requesting user's ID
     * @param collectionId the collection ID to validate (nullable)
     * @throws ApiException with 404 NOT_FOUND if the collection doesn't exist or isn't owned by this user
     */
    private void validateCollectionOwnership(String userId, String collectionId) {
        if (collectionId != null && !collectionId.isBlank()) {
            collectionRepository.findByIdAndUserId(collectionId, userId)
                    .orElseThrow(() -> new ApiException("Collection not found", HttpStatus.NOT_FOUND));
        }
    }

    /**
     * Maps a Prompt entity to its API-facing response DTO, converting
     * nested analysis/battle data and enum fields to their DTO forms.
     *
     * @param prompt the entity to map
     * @return the mapped PromptResponse
     */
    private PromptResponse mapToResponse(Prompt prompt) {
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

    /**
     * Maps a PromptCollection entity to its API-facing response DTO.
     *
     * @param collection the entity to map
     * @return the mapped CollectionResponse
     */
    private CollectionResponse mapCollectionToResponse(PromptCollection collection) {
        return new CollectionResponse(
                collection.getId(),
                collection.getName(),
                collection.getDescription(),
                collection.getColor(),
                collection.getPromptIds().size(),
                collection.getCreatedAt(),
                collection.getUpdatedAt()
        );
    }
}