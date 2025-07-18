package com.zamaz.mcp.debateengine.domain.model;

import com.zamaz.mcp.common.domain.ValueObject;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a message in context.
 */
public record Message(
    UUID id,
    MessageRole role,
    String content,
    int sequenceNumber,
    int tokenCount,
    LocalDateTime timestamp,
    RoundId roundId,
    ParticipantId participantId
) implements ValueObject {
    
    public Message {
        Objects.requireNonNull(id, "Message ID cannot be null");
        Objects.requireNonNull(role, "Role cannot be null");
        Objects.requireNonNull(content, "Content cannot be null");
        if (content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content cannot be empty");
        }
        if (sequenceNumber < 0) {
            throw new IllegalArgumentException("Sequence number must be non-negative");
        }
        if (tokenCount < 0) {
            throw new IllegalArgumentException("Token count must be non-negative");
        }
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");
    }
    
    /**
     * Create a new message.
     */
    public static Message create(
            MessageRole role,
            String content,
            int sequenceNumber,
            int tokenCount) {
        return new Message(
            UUID.randomUUID(),
            role,
            content,
            sequenceNumber,
            tokenCount,
            LocalDateTime.now(),
            null,
            null
        );
    }
    
    /**
     * Create a debate message.
     */
    public static Message createDebateMessage(
            MessageRole role,
            String content,
            int sequenceNumber,
            int tokenCount,
            RoundId roundId,
            ParticipantId participantId) {
        return new Message(
            UUID.randomUUID(),
            role,
            content,
            sequenceNumber,
            tokenCount,
            LocalDateTime.now(),
            roundId,
            participantId
        );
    }
    
    /**
     * Check if this is a debate message.
     */
    public boolean isDebateMessage() {
        return roundId != null && participantId != null;
    }
    
    /**
     * Message role enumeration.
     */
    public enum MessageRole {
        SYSTEM("System message"),
        USER("User message"),
        ASSISTANT("Assistant message"),
        MODERATOR("Moderator message");
        
        private final String description;
        
        MessageRole(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}