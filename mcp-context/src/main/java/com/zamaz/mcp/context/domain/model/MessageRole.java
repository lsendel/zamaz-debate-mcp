package com.zamaz.mcp.context.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;

/**
 * Value object representing the role of a message sender.
 */
public enum MessageRole implements ValueObject {
    USER("user"),
    ASSISTANT("assistant"),
    SYSTEM("system"),
    FUNCTION("function");
    
    private final String value;
    
    MessageRole(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    public static MessageRole fromValue(String value) {
        for (MessageRole role : MessageRole.values()) {
            if (role.value.equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Invalid message role: " + value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}