package com.zamaz.mcp.llm.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;
import java.util.Objects;

/**
 * Value object representing the name of an LLM model.
 */
public record ModelName(String value) implements ValueObject {
    
    public ModelName {
        Objects.requireNonNull(value, "Model name cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Model name cannot be empty");
        }
    }
    
    public static ModelName of(String value) {
        return new ModelName(value.trim());
    }
    
    // Claude models
    public static ModelName claude3Opus() {
        return new ModelName("claude-3-opus-20240229");
    }
    
    public static ModelName claude3Sonnet() {
        return new ModelName("claude-3-sonnet-20240229");
    }
    
    public static ModelName claude3Haiku() {
        return new ModelName("claude-3-haiku-20240307");
    }
    
    // OpenAI models
    public static ModelName gpt4() {
        return new ModelName("gpt-4");
    }
    
    public static ModelName gpt4Turbo() {
        return new ModelName("gpt-4-turbo-preview");
    }
    
    public static ModelName gpt35Turbo() {
        return new ModelName("gpt-3.5-turbo");
    }
    
    // Gemini models
    public static ModelName geminiPro() {
        return new ModelName("gemini-pro");
    }
    
    public static ModelName geminiProVision() {
        return new ModelName("gemini-pro-vision");
    }
    
    @Override
    public String toString() {
        return value;
    }
}