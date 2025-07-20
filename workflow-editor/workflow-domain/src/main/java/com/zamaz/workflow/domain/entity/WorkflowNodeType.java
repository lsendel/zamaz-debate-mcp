package com.zamaz.workflow.domain.entity;

public enum WorkflowNodeType {
    START("Start Node"),
    END("End Node"),
    TASK("Task Node"),
    DECISION("Decision Node"),
    PARALLEL_GATEWAY("Parallel Gateway"),
    EXCLUSIVE_GATEWAY("Exclusive Gateway"),
    TIMER("Timer Node"),
    ERROR_HANDLER("Error Handler");
    
    private final String displayName;
    
    WorkflowNodeType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}