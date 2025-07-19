package com.example.workflow.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a workflow execution identifier
 */
public record ExecutionId(String value) {
    
    public ExecutionId {
        Objects.requireNonNull(value, "Execution ID value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Execution ID value cannot be empty");
        }
    }
    
    /**
     * Generate a new random execution ID
     */
    public static ExecutionId generate() {
        return new ExecutionId(UUID.randomUUID().toString());
    }
    
    /**
     * Create execution ID from string
     */
    public static ExecutionId of(String value) {
        return new ExecutionId(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}