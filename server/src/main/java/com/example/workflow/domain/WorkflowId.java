package com.example.workflow.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a workflow identifier
 */
public record WorkflowId(String value) {
    
    public WorkflowId {
        Objects.requireNonNull(value, "Workflow ID value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Workflow ID value cannot be empty");
        }
    }
    
    /**
     * Generate a new random workflow ID
     */
    public static WorkflowId generate() {
        return new WorkflowId(UUID.randomUUID().toString());
    }
    
    /**
     * Create workflow ID from string
     */
    public static WorkflowId of(String value) {
        return new WorkflowId(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}