package com.zamaz.mcp.controller.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;

/**
 * Value object representing an LLM provider for AI participants.
 */
public enum LlmProvider implements ValueObject {
    CLAUDE("claude", "Anthropic Claude", "anthropic"),
    OPENAI("openai", "OpenAI GPT", "openai"),
    GEMINI("gemini", "Google Gemini", "google"),
    LLAMA("llama", "Meta Llama", "meta"),
    COHERE("cohere", "Cohere Command", "cohere"),
    MISTRAL("mistral", "Mistral AI", "mistral");
    
    private final String value;
    private final String displayName;
    private final String organization;
    
    LlmProvider(String value, String displayName, String organization) {
        this.value = value;
        this.displayName = displayName;
        this.organization = organization;
    }
    
    public String getValue() {
        return value;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getOrganization() {
        return organization;
    }
    
    public boolean isOpenSource() {
        return this == LLAMA || this == MISTRAL;
    }
    
    public boolean isProprietaryAPI() {
        return this == CLAUDE || this == OPENAI || this == GEMINI || this == COHERE;
    }
    
    public boolean supportsStreaming() {
        // All modern providers support streaming
        return true;
    }
    
    public boolean supportsTools() {
        return this == CLAUDE || this == OPENAI || this == GEMINI;
    }
    
    public int getDefaultMaxTokens() {
        return switch (this) {
            case CLAUDE -> 4096;
            case OPENAI -> 4096;
            case GEMINI -> 8192;
            case LLAMA -> 4096;
            case COHERE -> 4096;
            case MISTRAL -> 4096;
        };
    }
    
    public double getDefaultTemperature() {
        return switch (this) {
            case CLAUDE -> 0.7;
            case OPENAI -> 0.7;
            case GEMINI -> 0.9;
            case LLAMA -> 0.7;
            case COHERE -> 0.8;
            case MISTRAL -> 0.7;
        };
    }
    
    public static LlmProvider fromValue(String value) {
        for (LlmProvider provider : LlmProvider.values()) {
            if (provider.value.equalsIgnoreCase(value)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Invalid LLM provider: " + value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}