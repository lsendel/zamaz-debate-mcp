package com.zamaz.mcp.controller.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;
import java.util.Objects;

/**
 * Value object representing a participant's position in a debate.
 */
public record Position(String value) implements ValueObject {
    
    public Position {
        Objects.requireNonNull(value, "Position cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Position cannot be empty");
        }
        if (value.length() > 1000) {
            throw new IllegalArgumentException("Position cannot exceed 1000 characters");
        }
    }
    
    public static Position of(String value) {
        return new Position(value.trim());
    }
    
    public static Position pro() {
        return new Position("pro");
    }
    
    public static Position con() {
        return new Position("con");
    }
    
    public static Position neutral() {
        return new Position("neutral");
    }
    
    public boolean isPro() {
        return "pro".equalsIgnoreCase(value);
    }
    
    public boolean isCon() {
        return "con".equalsIgnoreCase(value);
    }
    
    public boolean isNeutral() {
        return "neutral".equalsIgnoreCase(value);
    }
    
    public Position opposite() {
        if (isPro()) {
            return con();
        } else if (isCon()) {
            return pro();
        } else {
            return this; // Neutral or custom positions return themselves
        }
    }
    
    public int length() {
        return value.length();
    }
    
    @Override
    public String toString() {
        return value;
    }
}