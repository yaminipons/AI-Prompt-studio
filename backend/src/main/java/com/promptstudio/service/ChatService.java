package com.promptstudio.service;

import com.promptstudio.dto.ChatDTOs.ChatMessageRequest;
import com.promptstudio.dto.ChatDTOs.ChatMessageResponse;
import com.promptstudio.dto.ChatDTOs.ChatSessionResponse;
import com.promptstudio.dto.ChatDTOs.ChatSessionSummary;
import com.promptstudio.entity.ChatSession;
import com.promptstudio.exception.ApiException;
import com.promptstudio.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service layer implementing the AI Chat feature. Manages chat session
 * lifecycle (create, list, delete) and message exchange, delegating
 * actual AI response generation to {@link AiService}. Chat history is
 * included as conversational context on every message so the AI can
 * maintain coherent multi-turn conversations.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final AiService aiService;

    private static final Pattern SENTENCE_SPLIT = Pattern.compile("(?<=[.!?])\\s+");
    private static final int MAX_HISTORY_MESSAGES = 20;

    /**
     * Creates a new, empty chat session for the user with a default title.
     *
     * @param userId the requesting user's ID
     * @return the created ChatSessionResponse
     */
    public ChatSessionResponse createSession(String userId) {
        ChatSession session = ChatSession.builder()
                .userId(userId)
                .title("New Chat")
                .messages(new ArrayList<>())
                .build();

        ChatSession saved = chatSessionRepository.save(session);
        return mapToResponse(saved);
    }

    /**
     * Retrieves a lightweight summary list of all chat sessions
     * belonging to the user, most recently updated first. Used to
     * populate the chat sidebar without loading full message histories.
     *
     * @param userId the requesting user's ID
     * @return the list of ChatSessionSummary
     */
    public List<ChatSessionSummary> getSessions(String userId) {
        return chatSessionRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(session -> new ChatSessionSummary(
                        session.getId(),
                        session.getTitle(),
                        session.getMessages().size(),
                        session.getUpdatedAt()
                ))
                .toList();
    }

    /**
     * Retrieves a single chat session with its full message history.
     *
     * @param userId    the requesting user's ID
     * @param sessionId the ID of the session to retrieve
     * @return the full ChatSessionResponse
     * @throws ApiException with 404 NOT_FOUND if not found or not owned by this user
     */
    public ChatSessionResponse getSession(String userId, String sessionId) {
        ChatSession session = findOwnedSessionOrThrow(sessionId, userId);
        return mapToResponse(session);
    }

    /**
     * Sends a new user message within a session, generates an AI reply
     * using the recent conversation as context, appends both messages
     * to the session, and auto-titles the session from the first
     * message if it's still using the default title.
     *
     * @param userId    the requesting user's ID
     * @param sessionId the ID of the session to send the message in
     * @param request   the message payload
     * @return the full updated ChatSessionResponse, including the new AI reply
     * @throws ApiException with 404 NOT_FOUND if the session isn't found or owned by this user
     */
    public ChatSessionResponse sendMessage(String userId, String sessionId, ChatMessageRequest request) {
        ChatSession session = findOwnedSessionOrThrow(sessionId, userId);

        ChatSession.ChatMessage userMessage = ChatSession.ChatMessage.builder()
                .role("USER")
                .content(request.message())
                .timestamp(LocalDateTime.now())
                .build();
        session.getMessages().add(userMessage);

        String aiReplyText = generateContextualReply(session.getMessages());

        ChatSession.ChatMessage aiMessage = ChatSession.ChatMessage.builder()
                .role("ASSISTANT")
                .content(aiReplyText)
                .timestamp(LocalDateTime.now())
                .build();
        session.getMessages().add(aiMessage);

        if ("New Chat".equals(session.getTitle())) {
            session.setTitle(buildTitleFromMessage(request.message()));
        }

        ChatSession saved = chatSessionRepository.save(session);
        return mapToResponse(saved);
    }

    /**
     * Builds a conversational prompt for Gemini incorporating recent
     * message history (capped to avoid excessive token usage), so the
     * AI's reply accounts for prior context in the conversation.
     *
     * @param messages the full message history of the session, including the just-added user message
     * @return the AI's generated reply text
     */
    private String generateContextualReply(List<ChatSession.ChatMessage> messages) {
        int startIndex = Math.max(0, messages.size() - MAX_HISTORY_MESSAGES);
        List<ChatSession.ChatMessage> recentHistory = messages.subList(startIndex, messages.size());

        StringBuilder conversationBuilder = new StringBuilder();
        conversationBuilder.append("You are a helpful AI assistant specialized in prompt engineering and ")
                .append("general assistance within an AI Prompt Engineering Studio application. Continue ")
                .append("the following conversation naturally, providing a helpful, clear, and well-formatted ")
                .append("response to the final user message.\n\nCONVERSATION:\n");

        for (ChatSession.ChatMessage message : recentHistory) {
            String speaker = "USER".equals(message.getRole()) ? "User" : "Assistant";
            conversationBuilder.append(speaker).append(": ").append(message.getContent()).append("\n");
        }

        conversationBuilder.append("Assistant:");

        return aiService.generateContent(conversationBuilder.toString());
    }

    /**
     * Derives a short chat session title from the user's first message,
     * truncating to the first sentence or a maximum length.
     *
     * @param firstMessage the user's first message in the session
     * @return a shortened display title
     */
    private String buildTitleFromMessage(String firstMessage) {
        String trimmed = firstMessage.trim();
        String[] sentences = SENTENCE_SPLIT.split(trimmed);
        String candidate = sentences.length > 0 ? sentences[0] : trimmed;
        return candidate.length() > 60 ? candidate.substring(0, 60).trim() + "..." : candidate;
    }

    /**
     * Renames a chat session's title manually.
     *
     * @param userId    the requesting user's ID
     * @param sessionId the ID of the session to rename
     * @param newTitle  the new title text
     * @return the updated ChatSessionResponse
     */
    public ChatSessionResponse renameSession(String userId, String sessionId, String newTitle) {
        ChatSession session = findOwnedSessionOrThrow(sessionId, userId);

        if (newTitle == null || newTitle.isBlank()) {
            throw new ApiException("Title cannot be empty", HttpStatus.BAD_REQUEST);
        }

        session.setTitle(newTitle.trim());
        ChatSession saved = chatSessionRepository.save(session);
        return mapToResponse(saved);
    }

    /**
     * Deletes a chat session permanently.
     *
     * @param userId    the requesting user's ID
     * @param sessionId the ID of the session to delete
     * @throws ApiException with 404 NOT_FOUND if not found or not owned by this user
     */
    public void deleteSession(String userId, String sessionId) {
        ChatSession session = findOwnedSessionOrThrow(sessionId, userId);
        chatSessionRepository.deleteById(session.getId());
    }

    /**
     * Looks up a chat session by ID, ensuring it belongs to the given user.
     *
     * @param sessionId the session ID to find
     * @param userId    the expected owning user's ID
     * @return the found ChatSession entity
     * @throws ApiException with 404 NOT_FOUND if not found or not owned by this user
     */
    private ChatSession findOwnedSessionOrThrow(String sessionId, String userId) {
        return chatSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ApiException("Chat session not found", HttpStatus.NOT_FOUND));
    }

    /**
     * Maps a ChatSession entity to its API-facing response DTO,
     * including the full ordered list of messages.
     *
     * @param session the entity to map
     * @return the mapped ChatSessionResponse
     */
    private ChatSessionResponse mapToResponse(ChatSession session) {
        List<ChatMessageResponse> messageResponses = session.getMessages().stream()
                .map(m -> new ChatMessageResponse(m.getRole(), m.getContent(), m.getTimestamp()))
                .toList();

        return new ChatSessionResponse(
                session.getId(),
                session.getTitle(),
                messageResponses,
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}