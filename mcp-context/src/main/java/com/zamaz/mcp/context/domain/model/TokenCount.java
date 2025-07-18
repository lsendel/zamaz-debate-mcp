package com.zamaz.mcp.context.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;
import java.util.Objects;

/**
 * Value object representing a count of tokens in text.
 * Ensures non-negative values.
 */
public record TokenCount(int value) implements ValueObject {
    
    public TokenCount {
        if (value < 0) {
            throw new IllegalArgumentException("Token count cannot be negative: " + value);
        }
    }
    
    public static TokenCount zero() {
        return new TokenCount(0);
    }
    
    public static TokenCount of(int value) {
        return new TokenCount(value);
    }
    
    public TokenCount add(TokenCount other) {
        Objects.requireNonNull(other, "Other token count cannot be null");
        return new TokenCount(this.value + other.value);
    }
    
    public TokenCount subtract(TokenCount other) {
        Objects.requireNonNull(other, "Other token count cannot be null");
        int result = this.value - other.value;
        if (result < 0) {
            throw new IllegalArgumentException("Token count would be negative: " + result);
        }
        return new TokenCount(result);
    }
    
    public boolean isGreaterThan(TokenCount other) {
        Objects.requireNonNull(other, "Other token count cannot be null");
        return this.value > other.value;
    }
    
    public boolean isLessThan(TokenCount other) {
        Objects.requireNonNull(other, "Other token count cannot be null");
        return this.value < other.value;
    }
    
    public boolean isWithinLimit(TokenCount limit) {
        Objects.requireNonNull(limit, "Limit cannot be null");
        return this.value <= limit.value;
    }
    
    @Override
    public String toString() {
        return String.valueOf(value);
    }
}