package com.zamaz.mcp.context.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Value object representing a token-limited window of messages from a Context.
 * Immutable view of a subset of messages that fit within token constraints.
 */
public class ContextWindow implements ValueObject {
    
    private final ContextId contextId;
    private final List<Message> messages;
    private final TokenCount totalTokens;
    
    private ContextWindow(ContextId contextId, List<Message> messages, TokenCount totalTokens) {
        this.contextId = Objects.requireNonNull(contextId, "Context ID cannot be null");
        this.messages = Collections.unmodifiableList(messages);
        this.totalTokens = Objects.requireNonNull(totalTokens, "Total tokens cannot be null");
    }
    
    public static ContextWindow empty(ContextId contextId) {
        return new ContextWindow(contextId, Collections.emptyList(), TokenCount.zero());
    }
    
    public static ContextWindow of(ContextId contextId, List<Message> messages, TokenCount totalTokens) {
        Objects.requireNonNull(messages, "Messages cannot be null");
        return new ContextWindow(contextId, List.copyOf(messages), totalTokens);
    }
    
    public boolean isEmpty() {
        return messages.isEmpty();
    }
    
    public boolean hasMessages() {
        return !messages.isEmpty();
    }
    
    public int getMessageCount() {
        return messages.size();
    }
    
    public boolean fitsWithin(TokenCount limit) {
        return totalTokens.isWithinLimit(limit);
    }
    
    public List<MessageSnapshot> toSnapshots() {
        return messages.stream()
            .map(Message::toSnapshot)
            .toList();
    }
    
    // Getters
    public ContextId getContextId() {
        return contextId;
    }
    
    public List<Message> getMessages() {
        return messages;
    }
    
    public TokenCount getTotalTokens() {
        return totalTokens;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContextWindow that = (ContextWindow) o;
        return Objects.equals(contextId, that.contextId) &&
               Objects.equals(messages, that.messages) &&
               Objects.equals(totalTokens, that.totalTokens);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(contextId, messages, totalTokens);
    }
    
    @Override
    public String toString() {
        return "ContextWindow{" +
               "contextId=" + contextId +
               ", messageCount=" + messages.size() +
               ", totalTokens=" + totalTokens +
               '}';
    }
}