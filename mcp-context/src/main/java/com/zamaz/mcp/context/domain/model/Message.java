package com.zamaz.mcp.context.domain.model;

import com.zamaz.mcp.common.domain.model.DomainEntity;
import com.zamaz.mcp.common.domain.exception.DomainRuleViolationException;
import java.time.Instant;
import java.util.Objects;

/**
 * Entity representing a message within a Context.
 * Part of the Context aggregate.
 */
public class Message extends DomainEntity<MessageId> {
    
    private final MessageRole role;
    private final MessageContent content;
    private final TokenCount tokenCount;
    private final Instant timestamp;
    private boolean hidden;
    
    private Message(
            MessageId id,
            MessageRole role,
            MessageContent content,
            TokenCount tokenCount,
            Instant timestamp,
            boolean hidden
    ) {
        super(id);
        this.role = Objects.requireNonNull(role, "Message role cannot be null");
        this.content = Objects.requireNonNull(content, "Message content cannot be null");
        this.tokenCount = Objects.requireNonNull(tokenCount, "Token count cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        this.hidden = hidden;
    }
    
    public static Message create(
            MessageRole role,
            MessageContent content,
            TokenCount tokenCount
    ) {
        return new Message(
            MessageId.generate(),
            role,
            content,
            tokenCount,
            Instant.now(),
            false
        );
    }
    
    public static Message restore(
            MessageId id,
            MessageRole role,
            MessageContent content,
            TokenCount tokenCount,
            Instant timestamp,
            boolean hidden
    ) {
        return new Message(id, role, content, tokenCount, timestamp, hidden);
    }
    
    public void hide() {
        if (hidden) {
            throw new DomainRuleViolationException(
                "Message.already.hidden",
                "Message is already hidden"
            );
        }
        this.hidden = true;
    }
    
    public void unhide() {
        if (!hidden) {
            throw new DomainRuleViolationException(
                "Message.not.hidden",
                "Message is not hidden"
            );
        }
        this.hidden = false;
    }
    
    public MessageSnapshot toSnapshot() {
        return new MessageSnapshot(
            getId().asString(),
            role,
            content.value(),
            tokenCount.value(),
            timestamp,
            hidden
        );
    }
    
    public boolean isVisible() {
        return !hidden;
    }
    
    public boolean isUserMessage() {
        return role == MessageRole.USER;
    }
    
    public boolean isAssistantMessage() {
        return role == MessageRole.ASSISTANT;
    }
    
    public boolean isSystemMessage() {
        return role == MessageRole.SYSTEM;
    }
    
    // Getters
    public MessageRole getRole() {
        return role;
    }
    
    public MessageContent getContent() {
        return content;
    }
    
    public TokenCount getTokenCount() {
        return tokenCount;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public boolean isHidden() {
        return hidden;
    }
    
    @Override
    public void validateInvariants() {
        if (role == null) {
            throw new DomainRuleViolationException(
                "Message.role.required",
                "Message must have a role"
            );
        }
        if (content == null) {
            throw new DomainRuleViolationException(
                "Message.content.required",
                "Message must have content"
            );
        }
        if (tokenCount == null || tokenCount.value() < 0) {
            throw new DomainRuleViolationException(
                "Message.tokenCount.invalid",
                "Message must have a valid token count"
            );
        }
    }
}