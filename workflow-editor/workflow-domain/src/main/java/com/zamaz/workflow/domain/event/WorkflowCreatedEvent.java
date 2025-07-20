package com.zamaz.workflow.domain.event;

import com.zamaz.workflow.domain.valueobject.WorkflowId;
import com.zamaz.workflow.domain.valueobject.OrganizationId;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.time.Instant;

@Getter
@Builder
public class WorkflowCreatedEvent implements DomainEvent {
    @NonNull
    private final WorkflowId workflowId;
    
    @NonNull
    private final String workflowName;
    
    @NonNull
    private final OrganizationId organizationId;
    
    @NonNull
    @Builder.Default
    private final Instant occurredAt = Instant.now();
    
    @Override
    public String getEventType() {
        return "workflow.created";
    }
}