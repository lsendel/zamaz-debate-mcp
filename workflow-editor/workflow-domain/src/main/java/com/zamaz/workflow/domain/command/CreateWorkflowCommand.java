package com.zamaz.workflow.domain.command;

import com.zamaz.workflow.domain.valueobject.OrganizationId;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Getter
@Builder
public class CreateWorkflowCommand {
    @NonNull
    private final String name;
    
    private final String description;
    
    @NonNull
    private final OrganizationId organizationId;
    
    @NonNull
    private final String createdBy;
}