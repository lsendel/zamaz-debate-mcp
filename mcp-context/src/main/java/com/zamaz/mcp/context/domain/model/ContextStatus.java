package com.zamaz.mcp.context.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;

/**
 * Value object representing the lifecycle status of a Context.
 */
public enum ContextStatus implements ValueObject {
    ACTIVE("active"),
    ARCHIVED("archived"),
    DELETED("deleted");
    
    private final String value;
    
    ContextStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    public static ContextStatus fromValue(String value) {
        for (ContextStatus status : ContextStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid context status: " + value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}