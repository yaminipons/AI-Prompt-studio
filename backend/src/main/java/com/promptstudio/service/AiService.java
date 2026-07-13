package com.promptstudio.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptstudio.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Integration layer for all external AI systems used by the platform:
 * the Groq API (text generation, via an OpenAI-compatible Chat
 * Completions endpoint) and ChromaDB (optional vector storage for
 * semantic search). Every feature service (PromptService, ChatService)
 * calls into this class rather than talking to WebClient directly,
 * keeping HTTP/parsing concerns in one place and business logic in
 * the callers.
 * <p>
 * Every failure path here logs the full request URL, HTTP status code,
 * and raw response body, and propagates Groq's own error message and
 * type (e.g. {@code invalid_api_key}, {@code rate_limit_exceeded})
 * to the caller rather than a generic message, so the true cause is
 * always visible both in logs and in the API response.
 * <p>
 * Groq does not provide a text embeddings endpoint (it is an
 * inference-only provider). Since ChromaDB semantic search is fully
 * optional and already gated behind {@code app.chromadb.enabled}
 * (default {@code false}), {@link #embedText(String)} throws a clear
 * {@link ApiException} if ever invoked; the existing best-effort
 * try/catch in {@link #storeEmbedding} and {@link #semanticSearch}
 * catches this and logs a non-fatal warning, falling back to keyword
 * search exactly as it already does for any other embedding failure.
 * This preserves identical behavior for the default configuration and
 * graceful degradation if ChromaDB is ever enabled.
 */
@Service
@RequiredArgsConstructor
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${app.groq.api-key}")
    private String groqApiKey;

    @Value("${app.groq.base-url}")
    private String groqBaseUrl;

    @Value("${app.groq.model}")
    private String groqModel;

    @Value("${app.groq.timeout-seconds}")
    private long groqTimeoutSeconds;

    @Value("${app.chromadb.enabled}")
    private boolean chromaEnabled;

    @Value("${app.chromadb.base-url}")
    private String chromaBaseUrl;

    @Value("${app.chromadb.collection-name}")
    private String chromaCollectionName;

    @Value("${app.chromadb.timeout-seconds}")
    private long chromaTimeoutSeconds;

    /** Lazily resolved and cached ChromaDB collection UUID. */
    private volatile String cachedCollectionId;

    // ==================== GROQ TEXT GENERATION ====================

    /**
     * Sends a text prompt to the Groq Chat Completions API and returns
     * the model's generated response text.
     *
     * @param prompt the full prompt text to send to Groq
     * @return the generated text response
     * @throws ApiException carrying Groq's actual error message/type
     *                       (e.g. invalid_api_key, rate_limit_exceeded)
     *                       and the corresponding HTTP status
     */
    public String generateContent(String prompt) {
        if (groqApiKey == null || groqApiKey.isBlank()) {
            log.error("Groq API key is not configured (app.groq.api-key / GROQ_API_KEY is blank)");
            throw new ApiException("Groq API key is not configured on the server", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String url = groqBaseUrl + "/chat/completions";

        Map<String, Object> requestBody = Map.of(
                "model", groqModel,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        log.info("Calling Groq chat completions | url={} | model={}", url, groqModel);

        try {
            String responseBody = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + groqApiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(groqTimeoutSeconds));

            log.debug("Groq chat completions raw response | body={}", responseBody);

            return extractGroqText(responseBody, url);

        } catch (WebClientResponseException ex) {
            // This branch fires for any 4xx/5xx HTTP status returned by Groq.
            String rawErrorBody = ex.getResponseBodyAsString();
            int statusCode = ex.getStatusCode().value();

            log.error("Groq API HTTP error | url={} | status={} | body={}", url, statusCode, rawErrorBody);

            GroqErrorDetails details = parseGroqError(rawErrorBody);
            HttpStatus mappedStatus = mapGroqStatusToHttpStatus(statusCode, details.type());

            String message = buildErrorMessage(details, statusCode, rawErrorBody);
            throw new ApiException(message, mappedStatus);

        } catch (ApiException ex) {
            // Already a well-formed ApiException (e.g. thrown by extractGroqText) - rethrow as-is.
            throw ex;

        } catch (Exception ex) {
            // Network failure, timeout, or any other unexpected error.
            log.error("Unexpected error calling Groq API | url={}", url, ex);
            throw new ApiException(
                    "Failed to reach the AI service: " + ex.getClass().getSimpleName() + " - " + ex.getMessage(),
                    HttpStatus.BAD_GATEWAY
            );
        }
    }

    /**
     * Parses a Groq chat completions JSON response (OpenAI-compatible
     * shape) and extracts the generated text from the first choice.
     * Also inspects {@code finish_reason} so a response cut short for
     * an unexpected reason (e.g. {@code content_filter}) is surfaced
     * with a clear, specific message instead of a silent generic
     * failure.
     *
     * @param responseBody the raw JSON response body from Groq
     * @param url          the request URL, for logging context
     * @return the extracted generated text
     * @throws ApiException with the specific reason if no valid choice text is found
     */
    private String extractGroqText(String responseBody, String url) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");

            if (!choices.isArray() || choices.isEmpty()) {
                log.error("Groq response contained no choices | url={} | body={}", url, responseBody);
                throw new ApiException("Groq returned no choices for this request", HttpStatus.BAD_GATEWAY);
            }

            JsonNode firstChoice = choices.get(0);
            String finishReason = firstChoice.path("finish_reason").asText(null);

            if ("content_filter".equals(finishReason)) {
                log.error("Groq blocked the response via content filter | url={} | body={}", url, responseBody);
                throw new ApiException(
                        "Groq blocked this request due to content filtering. Please rephrase your input.",
                        HttpStatus.BAD_GATEWAY
                );
            }

            String text = firstChoice.path("message").path("content").asText();
            if (text == null || text.isBlank()) {
                log.error("Groq choice content was blank | url={} | finishReason={} | body={}",
                        url, finishReason, responseBody);
                throw new ApiException(
                        "Groq returned an empty response" + (finishReason != null ? " (finishReason: " + finishReason + ")" : ""),
                        HttpStatus.BAD_GATEWAY
                );
            }

            return text.trim();

        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to parse Groq response JSON | url={} | body={}", url, responseBody, ex);
            throw new ApiException(
                    "Failed to interpret the AI service response: " + ex.getMessage(),
                    HttpStatus.BAD_GATEWAY
            );
        }
    }

    /**
     * Structured representation of a Groq API error payload:
     * {@code {"error": {"message": ..., "type": ..., "code": ...}}}
     * (the standard OpenAI-compatible error shape Groq uses).
     */
    private record GroqErrorDetails(String code, String message, String type) {
    }

    /**
     * Parses Groq's standard OpenAI-compatible error JSON shape to
     * extract the machine-readable type (e.g. {@code invalid_api_key},
     * {@code rate_limit_exceeded}) and the human-readable message.
     * Falls back to nulls if the body isn't valid JSON or doesn't
     * match the expected shape - callers must handle nulls gracefully.
     *
     * @param rawErrorBody the raw error response body from Groq
     * @return the parsed error details (fields may be null if parsing failed)
     */
    private GroqErrorDetails parseGroqError(String rawErrorBody) {
        if (rawErrorBody == null || rawErrorBody.isBlank()) {
            return new GroqErrorDetails(null, null, null);
        }
        try {
            JsonNode root = objectMapper.readTree(rawErrorBody);
            JsonNode error = root.path("error");
            String code = error.has("code") ? error.path("code").asText() : null;
            String message = error.has("message") ? error.path("message").asText() : null;
            String type = error.has("type") ? error.path("type").asText() : null;
            return new GroqErrorDetails(code, message, type);
        } catch (Exception ex) {
            log.warn("Could not parse Groq error body as JSON: {}", rawErrorBody);
            return new GroqErrorDetails(null, null, null);
        }
    }

    /**
     * Builds a clear, specific error message combining Groq's
     * machine-readable error type and human-readable message, so the
     * caller (and ultimately the frontend toast) shows the real cause
     * - e.g. "Groq error [invalid_api_key]: Invalid API Key" -
     * instead of a generic string.
     *
     * @param details    the parsed Groq error details
     * @param statusCode the raw HTTP status code returned by Groq
     * @param rawBody    the raw error body, used as a last-resort fallback
     * @return the fully composed error message
     */
    private String buildErrorMessage(GroqErrorDetails details, int statusCode, String rawBody) {
        if (details.type() != null && details.message() != null) {
            return "Groq error [" + details.type() + "]: " + details.message();
        }
        if (details.message() != null) {
            return "Groq error (HTTP " + statusCode + "): " + details.message();
        }
        return "Groq error (HTTP " + statusCode + "): " + (rawBody != null && !rawBody.isBlank() ? rawBody : "no response body");
    }

    /**
     * Maps Groq's HTTP status code and/or machine-readable error type
     * to the most appropriate HTTP status to return from our own API,
     * so clients can distinguish "your API key is bad" (500 - a server
     * misconfiguration) from "you're being rate limited" (429) from
     * "the request itself was invalid" (400) rather than everything
     * collapsing into one generic 502.
     *
     * @param groqStatusCode the raw HTTP status Groq returned
     * @param groqErrorType  Groq's machine-readable error type string, if parsed
     * @return the HttpStatus to respond with
     */
    private HttpStatus mapGroqStatusToHttpStatus(int groqStatusCode, String groqErrorType) {
        if (groqErrorType != null) {
            return switch (groqErrorType) {
                case "invalid_api_key", "authentication_error" -> HttpStatus.INTERNAL_SERVER_ERROR;
                case "permission_error" -> HttpStatus.INTERNAL_SERVER_ERROR;
                case "rate_limit_exceeded" -> HttpStatus.TOO_MANY_REQUESTS;
                case "invalid_request_error" -> HttpStatus.BAD_REQUEST;
                case "service_unavailable", "timeout" -> HttpStatus.SERVICE_UNAVAILABLE;
                default -> HttpStatus.BAD_GATEWAY;
            };
        }
        return switch (groqStatusCode) {
            case 400 -> HttpStatus.BAD_REQUEST;
            case 401, 403 -> HttpStatus.INTERNAL_SERVER_ERROR;
            case 429 -> HttpStatus.TOO_MANY_REQUESTS;
            case 503 -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_GATEWAY;
        };
    }

    // ==================== TEXT EMBEDDINGS (NOT SUPPORTED BY GROQ) ====================

    /**
     * Groq is an inference-only provider and does not expose a text
     * embeddings endpoint. This method always throws; it exists so
     * {@link #storeEmbedding} and {@link #semanticSearch} keep their
     * existing best-effort try/catch structure unchanged, which
     * already treats any embedding failure as non-fatal and falls
     * back to keyword search.
     *
     * @param text the text that would have been embedded
     * @return never returns
     * @throws ApiException always, indicating embeddings are unsupported
     */
    private List<Float> embedText(String text) {
        throw new ApiException(
                "Text embeddings are not supported by the configured AI provider (Groq). "
                        + "Semantic search via ChromaDB is unavailable; falling back to keyword search.",
                HttpStatus.NOT_IMPLEMENTED
        );
    }

    // ==================== CHROMADB SEMANTIC SEARCH (OPTIONAL) ====================

    /**
     * Generates an embedding for the given prompt text and stores it in
     * ChromaDB under the given ID, along with basic metadata for later
     * filtering. This is a best-effort operation: if ChromaDB is
     * disabled, embeddings are unsupported by the current AI provider,
     * or ChromaDB is unreachable, the failure is logged and swallowed
     * rather than propagated, since semantic search is a supplementary
     * feature and should never block core prompt-saving functionality.
     *
     * @param id       the ID to store the embedding under (typically the Prompt's MongoDB ID)
     * @param text     the text to embed and index
     * @param metadata additional metadata to attach (e.g., userId, title)
     */
    public void storeEmbedding(String id, String text, Map<String, Object> metadata) {
        if (!chromaEnabled) {
            return;
        }

        try {
            List<Float> embedding = embedText(text);
            String collectionId = getOrCreateCollectionId();

            Map<String, Object> requestBody = Map.of(
                    "ids", List.of(id),
                    "embeddings", List.of(embedding),
                    "documents", List.of(text),
                    "metadatas", List.of(metadata)
            );

            webClient.post()
                    .uri(chromaBaseUrl + "/api/v1/collections/" + collectionId + "/add")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(chromaTimeoutSeconds));

        } catch (Exception ex) {
            log.warn("ChromaDB embedding storage failed for id={} (non-fatal): {}", id, ex.getMessage());
        }
    }

    /**
     * Performs a semantic similarity search against stored embeddings
     * in ChromaDB, returning the IDs of the closest matching records.
     * Returns an empty list if ChromaDB is disabled, embeddings are
     * unsupported by the current AI provider, or the search fails, so
     * callers can safely fall back to keyword search.
     *
     * @param queryText the natural-language search query
     * @param topK      the maximum number of matching IDs to return
     * @return a list of matching record IDs, ordered by relevance (closest first)
     */
    public List<String> semanticSearch(String queryText, int topK) {
        if (!chromaEnabled) {
            return List.of();
        }

        try {
            List<Float> queryEmbedding = embedText(queryText);
            String collectionId = getOrCreateCollectionId();

            Map<String, Object> requestBody = Map.of(
                    "query_embeddings", List.of(queryEmbedding),
                    "n_results", topK
            );

            String responseBody = webClient.post()
                    .uri(chromaBaseUrl + "/api/v1/collections/" + collectionId + "/query")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(chromaTimeoutSeconds));

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode idsArray = root.path("ids");

            List<String> results = new ArrayList<>();
            if (idsArray.isArray() && !idsArray.isEmpty()) {
                for (JsonNode idNode : idsArray.get(0)) {
                    results.add(idNode.asText());
                }
            }
            return results;

        } catch (Exception ex) {
            log.warn("ChromaDB semantic search failed (non-fatal, falling back to keyword search): {}", ex.getMessage());
            return List.of();
        }
    }

    /**
     * Removes a stored embedding from ChromaDB by ID. Best-effort and
     * non-fatal, same as {@link #storeEmbedding}.
     *
     * @param id the ID of the embedding to remove
     */
    public void deleteEmbedding(String id) {
        if (!chromaEnabled) {
            return;
        }

        try {
            String collectionId = getOrCreateCollectionId();
            Map<String, Object> requestBody = Map.of("ids", List.of(id));

            webClient.post()
                    .uri(chromaBaseUrl + "/api/v1/collections/" + collectionId + "/delete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(chromaTimeoutSeconds));

        } catch (Exception ex) {
            log.warn("ChromaDB embedding deletion failed for id={} (non-fatal): {}", id, ex.getMessage());
        }
    }

    /**
     * Resolves the ChromaDB collection's UUID, creating the collection
     * first if it does not yet exist. The result is cached in memory
     * after the first successful resolution to avoid a lookup on every
     * single embedding operation.
     *
     * @return the ChromaDB collection UUID
     */
    private synchronized String getOrCreateCollectionId() {
        if (cachedCollectionId != null) {
            return cachedCollectionId;
        }

        try {
            String getResponse = webClient.get()
                    .uri(chromaBaseUrl + "/api/v1/collections/" + chromaCollectionName)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(chromaTimeoutSeconds));

            JsonNode root = objectMapper.readTree(getResponse);
            cachedCollectionId = root.path("id").asText();
            return cachedCollectionId;

        } catch (Exception ex) {
            Map<String, Object> createBody = Map.of("name", chromaCollectionName);

            String createResponse = webClient.post()
                    .uri(chromaBaseUrl + "/api/v1/collections")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(chromaTimeoutSeconds));

            try {
                JsonNode root = objectMapper.readTree(createResponse);
                cachedCollectionId = root.path("id").asText();
                return cachedCollectionId;
            } catch (Exception parseEx) {
                throw new ApiException("Failed to initialize ChromaDB collection: " + parseEx.getMessage(), HttpStatus.BAD_GATEWAY);
            }
        }
    }
}