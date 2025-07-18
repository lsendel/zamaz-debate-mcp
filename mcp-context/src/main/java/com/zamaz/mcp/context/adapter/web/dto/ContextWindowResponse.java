package com.zamaz.mcp.context.adapter.web.dto;

import java.time.Instant;
import java.util.List;

/**
 * Web DTO for context window response.
 * Represents a token-limited window of messages from a context.
 */
public record ContextWindowResponse(
    String contextId,
    List<MessageResponse> messages,
    int totalTokens,
    int messageCount
) {
    /**
     * Nested record for message response within context window.
     */
    public record MessageResponse(
        String id,
        String role,
        String content,
        int tokenCount,
        Instant timestamp,
        boolean hidden
    ) {}
    
    /**
     * Creates an empty context window response.
     */
    public static ContextWindowResponse empty(String contextId) {
        return new ContextWindowResponse(contextId, List.of(), 0, 0);
    }
}