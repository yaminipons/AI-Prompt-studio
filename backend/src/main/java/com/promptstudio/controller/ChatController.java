package com.promptstudio.controller;

import com.promptstudio.dto.ChatDTOs.ChatMessageRequest;
import com.promptstudio.dto.ChatDTOs.ChatSessionResponse;
import com.promptstudio.dto.ChatDTOs.ChatSessionSummary;
import com.promptstudio.dto.CommonDTOs.ApiResponse;
import com.promptstudio.security.CustomUserDetailsService.UserPrincipal;
import com.promptstudio.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller exposing the AI Chat feature: session management and
 * message exchange. Every endpoint is scoped to the authenticated
 * user's own chat sessions.
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "AI Chat", description = "Conversational AI chat sessions")
public class ChatController {

    private final ChatService chatService;

    /**
     * Creates a new, empty chat session.
     *
     * @param principal the authenticated user
     * @return 201 CREATED with the new ChatSessionResponse
     */
    @PostMapping("/sessions")
    @Operation(summary = "Create a new chat session")
    public ResponseEntity<ApiResponse<ChatSessionResponse>> createSession(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ChatSessionResponse response = chatService.createSession(principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Chat session created", response));
    }

    /**
     * Lists all chat sessions belonging to the authenticated user,
     * as lightweight summaries (no full message history).
     *
     * @param principal the authenticated user
     * @return 200 OK with the list of ChatSessionSummary
     */
    @GetMapping("/sessions")
    @Operation(summary = "List all chat sessions for the authenticated user")
    public ResponseEntity<ApiResponse<List<ChatSessionSummary>>> getSessions(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<ChatSessionSummary> response = chatService.getSessions(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Chat sessions retrieved", response));
    }

    /**
     * Retrieves a single chat session with its full message history.
     *
     * @param principal the authenticated user
     * @param sessionId the ID of the session to retrieve
     * @return 200 OK with the full ChatSessionResponse
     */
    @GetMapping("/sessions/{sessionId}")
    @Operation(summary = "Get a chat session with full message history")
    public ResponseEntity<ApiResponse<ChatSessionResponse>> getSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String sessionId
    ) {
        ChatSessionResponse response = chatService.getSession(principal.getUserId(), sessionId);
        return ResponseEntity.ok(ApiResponse.success("Chat session retrieved", response));
    }

    /**
     * Sends a new message within a chat session and returns the
     * updated session including the AI's reply.
     *
     * @param principal the authenticated user
     * @param sessionId the ID of the session to send the message in
     * @param request   the message payload
     * @return 200 OK with the updated ChatSessionResponse
     */
    @PostMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "Send a message and receive an AI reply")
    public ResponseEntity<ApiResponse<ChatSessionResponse>> sendMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String sessionId,
            @Valid @RequestBody ChatMessageRequest request
    ) {
        ChatSessionResponse response = chatService.sendMessage(principal.getUserId(), sessionId, request);
        return ResponseEntity.ok(ApiResponse.success("Message sent", response));
    }

    /**
     * Renames a chat session's title.
     *
     * @param principal the authenticated user
     * @param sessionId the ID of the session to rename
     * @param body      a map containing the new "title" field
     * @return 200 OK with the updated ChatSessionResponse
     */
    @PatchMapping("/sessions/{sessionId}/title")
    @Operation(summary = "Rename a chat session")
    public ResponseEntity<ApiResponse<ChatSessionResponse>> renameSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String sessionId,
            @RequestBody Map<String, String> body
    ) {
        ChatSessionResponse response = chatService.renameSession(principal.getUserId(), sessionId, body.get("title"));
        return ResponseEntity.ok(ApiResponse.success("Chat session renamed", response));
    }

    /**
     * Deletes a chat session permanently.
     *
     * @param principal the authenticated user
     * @param sessionId the ID of the session to delete
     * @return 200 OK with no data payload
     */
    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "Delete a chat session")
    public ResponseEntity<ApiResponse<Void>> deleteSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String sessionId
    ) {
        chatService.deleteSession(principal.getUserId(), sessionId);
        return ResponseEntity.ok(ApiResponse.success("Chat session deleted"));
    }
}