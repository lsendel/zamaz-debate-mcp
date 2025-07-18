package com.zamaz.mcp.context.adapter.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Web DTO for context response.
 * Represents the complete context with all its fields and messages.
 */
public record ContextResponse(
    String id,
    String organizationId,
    String userId,
    String name,
    Map<String, Object> metadata,
    String status,
    List<MessageResponse> messages,
    int totalTokens,
    int messageCount,
    int visibleMessageCount,
    Instant createdAt,
    Instant updatedAt
) {
    /**
     * Nested record for message response within context.
     */
    public record MessageResponse(
        String id,
        String role,
        String content,
        int tokenCount,
        Instant timestamp,
        boolean hidden
    ) {}
}