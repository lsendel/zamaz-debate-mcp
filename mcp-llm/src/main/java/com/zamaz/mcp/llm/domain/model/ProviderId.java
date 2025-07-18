package com.zamaz.mcp.llm.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;
import java.util.Objects;

/**
 * Value object representing a unique identifier for an LLM provider.
 */
public record ProviderId(String value) implements ValueObject {
    
    public ProviderId {
        Objects.requireNonNull(value, "Provider ID cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Provider ID cannot be empty");
        }
    }
    
    public static ProviderId of(String value) {
        return new ProviderId(value.toLowerCase().trim());
    }
    
    public static ProviderId claude() {
        return new ProviderId("claude");
    }
    
    public static ProviderId openai() {
        return new ProviderId("openai");
    }
    
    public static ProviderId gemini() {
        return new ProviderId("gemini");
    }
    
    public static ProviderId llama() {
        return new ProviderId("llama");
    }
    
    @Override
    public String toString() {
        return value;
    }
}