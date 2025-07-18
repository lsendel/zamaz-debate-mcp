package com.zamaz.mcp.llm.domain.event;

import com.zamaz.mcp.common.domain.event.AbstractDomainEvent;
import java.time.Instant;

/**
 * Domain event raised when a completion request is created.
 */
public class CompletionRequestCreatedEvent extends AbstractDomainEvent {
    
    private final String requestId;
    private final String organizationId;
    private final String userId;
    private final int estimatedTokens;
    private final String preferredModel;
    private final String preferredProvider;
    
    public CompletionRequestCreatedEvent(
            String requestId,
            String organizationId,
            String userId,
            int estimatedTokens,
            String preferredModel,
            String preferredProvider,
            Instant occurredOn
    ) {
        super(occurredOn);
        this.requestId = requestId;
        this.organizationId = organizationId;
        this.userId = userId;
        this.estimatedTokens = estimatedTokens;
        this.preferredModel = preferredModel;
        this.preferredProvider = preferredProvider;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public String getOrganizationId() {
        return organizationId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public int getEstimatedTokens() {
        return estimatedTokens;
    }
    
    public String getPreferredModel() {
        return preferredModel;
    }
    
    public String getPreferredProvider() {
        return preferredProvider;
    }
    
    @Override
    public String getEventType() {
        return "completion.request.created";
    }
    
    @Override
    public String getAggregateId() {
        return requestId;
    }
}