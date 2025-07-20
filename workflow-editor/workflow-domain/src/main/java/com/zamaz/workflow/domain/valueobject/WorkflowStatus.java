package com.zamaz.workflow.domain.valueobject;

public enum WorkflowStatus {
    DRAFT("Draft", "Workflow is being designed"),
    ACTIVE("Active", "Workflow is active and can be executed"),
    INACTIVE("Inactive", "Workflow is temporarily disabled"),
    ARCHIVED("Archived", "Workflow is archived and cannot be executed"),
    ERROR("Error", "Workflow has errors and cannot be executed");
    
    private final String displayName;
    private final String description;
    
    WorkflowStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean canExecute() {
        return this == ACTIVE;
    }
    
    public boolean canEdit() {
        return this == DRAFT || this == ERROR;
    }
}