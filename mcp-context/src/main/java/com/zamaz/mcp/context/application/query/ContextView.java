package com.zamaz.mcp.context.application.query;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * View/DTO record containing all context information.
 * Used as a read model for context queries.
 */
public record ContextView(
    String id,
    String organizationId,
    String userId,
    String name,
    String status,
    List<MessageView> messages,
    Map<String, Object> metadata,
    int totalTokens,
    int messageCount,
    int visibleMessageCount,
    Instant createdAt,
    Instant updatedAt
) {
    
    public ContextView {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Context ID cannot be null or empty");
        }
        if (organizationId == null || organizationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Organization ID cannot be null or empty");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status cannot be null or empty");
        }
        if (messages == null) {
            throw new IllegalArgumentException("Messages cannot be null");
        }
        if (metadata == null) {
            throw new IllegalArgumentException("Metadata cannot be null");
        }
        if (totalTokens < 0) {
            throw new IllegalArgumentException("Total tokens cannot be negative");
        }
        if (messageCount < 0) {
            throw new IllegalArgumentException("Message count cannot be negative");
        }
        if (visibleMessageCount < 0) {
            throw new IllegalArgumentException("Visible message count cannot be negative");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("Created at cannot be null");
        }
        if (updatedAt == null) {
            throw new IllegalArgumentException("Updated at cannot be null");
        }
    }
    
    /**
     * View/DTO for individual messages within a context.
     */
    public record MessageView(
        String id,
        String role,
        String content,
        int tokenCount,
        Instant timestamp,
        boolean hidden
    ) {
        public MessageView {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("Message ID cannot be null or empty");
            }
            if (role == null || role.trim().isEmpty()) {
                throw new IllegalArgumentException("Role cannot be null or empty");
            }
            if (content == null) {
                throw new IllegalArgumentException("Content cannot be null");
            }
            if (tokenCount < 0) {
                throw new IllegalArgumentException("Token count cannot be negative");
            }
            if (timestamp == null) {
                throw new IllegalArgumentException("Timestamp cannot be null");
            }
        }
    }
}