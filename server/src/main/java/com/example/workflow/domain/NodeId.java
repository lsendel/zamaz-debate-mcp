package com.example.workflow.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a node identifier
 */
public record NodeId(String value) {
    
    public NodeId {
        Objects.requireNonNull(value, "Node ID value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Node ID value cannot be empty");
        }
    }
    
    /**
     * Generate a new random node ID
     */
    public static NodeId generate() {
        return new NodeId(UUID.randomUUID().toString());
    }
    
    /**
     * Create node ID from string
     */
    public static NodeId of(String value) {
        return new NodeId(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}