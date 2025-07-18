package com.zamaz.mcp.context.domain.event;

import com.zamaz.mcp.common.domain.event.AbstractDomainEvent;
import java.time.Instant;

/**
 * Domain event raised when a Message is appended to a Context.
 */
public class MessageAppendedEvent extends AbstractDomainEvent {
    
    private final String contextId;
    private final String messageId;
    private final String role;
    private final String content;
    private final int tokenCount;
    
    public MessageAppendedEvent(
            String contextId,
            String messageId,
            String role,
            String content,
            int tokenCount,
            Instant occurredOn
    ) {
        super(occurredOn);
        this.contextId = contextId;
        this.messageId = messageId;
        this.role = role;
        this.content = content;
        this.tokenCount = tokenCount;
    }
    
    public String getContextId() {
        return contextId;
    }
    
    public String getMessageId() {
        return messageId;
    }
    
    public String getRole() {
        return role;
    }
    
    public String getContent() {
        return content;
    }
    
    public int getTokenCount() {
        return tokenCount;
    }
    
    @Override
    public String getEventType() {
        return "context.message.appended";
    }
    
    @Override
    public String getAggregateId() {
        return contextId;
    }
}