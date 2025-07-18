package com.zamaz.mcp.debateengine.domain.model;

import com.zamaz.mcp.common.domain.ValueObject;
import java.util.Map;
import java.util.Objects;

/**
 * Value object representing AI model configuration.
 */
public record AIModel(
    String provider,
    String name,
    Map<String, Object> config
) implements ValueObject {
    
    public AIModel {
        Objects.requireNonNull(provider, "Provider cannot be null");
        Objects.requireNonNull(name, "Model name cannot be null");
        Objects.requireNonNull(config, "Config cannot be null");
        
        provider = provider.trim().toLowerCase();
        name = name.trim();
        
        if (provider.isEmpty()) {
            throw new IllegalArgumentException("Provider cannot be empty");
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Model name cannot be empty");
        }
        
        config = Map.copyOf(config); // Make immutable
    }
    
    /**
     * Create AI model configuration.
     */
    public static AIModel of(String provider, String name, Map<String, Object> config) {
        return new AIModel(provider, name, config);
    }
    
    /**
     * Create OpenAI model.
     */
    public static AIModel openAI(String model) {
        return new AIModel("openai", model, Map.of(
            "temperature", 0.7,
            "max_tokens", 1000
        ));
    }
    
    /**
     * Create Anthropic model.
     */
    public static AIModel anthropic(String model) {
        return new AIModel("anthropic", model, Map.of(
            "temperature", 0.7,
            "max_tokens", 1000
        ));
    }
    
    /**
     * Update configuration.
     */
    public AIModel withConfig(Map<String, Object> newConfig) {
        return new AIModel(provider, name, newConfig);
    }
    
    /**
     * Get configuration value.
     */
    @SuppressWarnings("unchecked")
    public <T> T getConfigValue(String key, T defaultValue) {
        return (T) config.getOrDefault(key, defaultValue);
    }
    
    /**
     * Get display name.
     */
    public String getDisplayName() {
        return provider + "/" + name;
    }
}