package com.zamaz.mcp.context.domain.event;

import com.zamaz.mcp.common.domain.event.AbstractDomainEvent;
import java.time.Instant;

/**
 * Domain event raised when a Message is hidden in a Context.
 */
public class MessageHiddenEvent extends AbstractDomainEvent {
    
    private final String contextId;
    private final String messageId;
    
    public MessageHiddenEvent(
            String contextId,
            String messageId,
            Instant occurredOn
    ) {
        super(occurredOn);
        this.contextId = contextId;
        this.messageId = messageId;
    }
    
    public String getContextId() {
        return contextId;
    }
    
    public String getMessageId() {
        return messageId;
    }
    
    @Override
    public String getEventType() {
        return "context.message.hidden";
    }
    
    @Override
    public String getAggregateId() {
        return contextId;
    }
}