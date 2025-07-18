package com.zamaz.mcp.context.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;
import java.time.Instant;

/**
 * Value object representing an immutable snapshot of a Message at a point in time.
 * Used for versioning and historical tracking.
 */
public record MessageSnapshot(
    String messageId,
    MessageRole role,
    String content,
    int tokenCount,
    Instant timestamp,
    boolean hidden
) implements ValueObject {
    
    public MessageSnapshot {
        if (messageId == null || messageId.isBlank()) {
            throw new IllegalArgumentException("Message ID cannot be null or blank");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
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
    
    public static MessageSnapshot fromMessage(Message message) {
        return message.toSnapshot();
    }
    
    public Message toMessage() {
        return Message.restore(
            MessageId.from(messageId),
            role,
            MessageContent.of(content),
            TokenCount.of(tokenCount),
            timestamp,
            hidden
        );
    }
}