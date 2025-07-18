package com.zamaz.mcp.controller.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Objects;

/**
 * Value object representing configuration for an LLM provider.
 */
public record ProviderConfig(
    String model,
    int maxTokens,
    BigDecimal temperature,
    BigDecimal topP,
    String systemPrompt,
    Map<String, Object> additionalParams
) implements ValueObject {
    
    private static final BigDecimal MIN_TEMPERATURE = BigDecimal.ZERO;
    private static final BigDecimal MAX_TEMPERATURE = BigDecimal.valueOf(2.0);
    private static final BigDecimal MIN_TOP_P = BigDecimal.valueOf(0.1);
    private static final BigDecimal MAX_TOP_P = BigDecimal.ONE;
    private static final int MIN_MAX_TOKENS = 1;
    private static final int MAX_MAX_TOKENS = 32_768;
    private static final int MAX_SYSTEM_PROMPT_LENGTH = 10_000;
    
    public ProviderConfig {
        Objects.requireNonNull(model, "Model cannot be null");
        Objects.requireNonNull(temperature, "Temperature cannot be null");
        Objects.requireNonNull(topP, "TopP cannot be null");
        Objects.requireNonNull(systemPrompt, "System prompt cannot be null");
        Objects.requireNonNull(additionalParams, "Additional params cannot be null");
        
        if (model.trim().isEmpty()) {
            throw new IllegalArgumentException("Model cannot be empty");
        }
        
        if (maxTokens < MIN_MAX_TOKENS || maxTokens > MAX_MAX_TOKENS) {
            throw new IllegalArgumentException(
                "Max tokens must be between " + MIN_MAX_TOKENS + " and " + MAX_MAX_TOKENS
            );
        }
        
        if (temperature.compareTo(MIN_TEMPERATURE) < 0 || temperature.compareTo(MAX_TEMPERATURE) > 0) {
            throw new IllegalArgumentException(
                "Temperature must be between " + MIN_TEMPERATURE + " and " + MAX_TEMPERATURE
            );
        }
        
        if (topP.compareTo(MIN_TOP_P) < 0 || topP.compareTo(MAX_TOP_P) > 0) {
            throw new IllegalArgumentException(
                "TopP must be between " + MIN_TOP_P + " and " + MAX_TOP_P
            );
        }
        
        if (systemPrompt.length() > MAX_SYSTEM_PROMPT_LENGTH) {
            throw new IllegalArgumentException(
                "System prompt cannot exceed " + MAX_SYSTEM_PROMPT_LENGTH + " characters"
            );
        }
    }
    
    public static ProviderConfig of(String model, int maxTokens, double temperature, 
                                  double topP, String systemPrompt) {
        return new ProviderConfig(
            model,
            maxTokens,
            BigDecimal.valueOf(temperature).setScale(2, RoundingMode.HALF_UP),
            BigDecimal.valueOf(topP).setScale(2, RoundingMode.HALF_UP),
            systemPrompt,
            Map.of()
        );
    }
    
    public static ProviderConfig forProvider(LlmProvider provider, String systemPrompt) {
        Objects.requireNonNull(provider, "Provider cannot be null");
        Objects.requireNonNull(systemPrompt, "System prompt cannot be null");
        
        String defaultModel = getDefaultModel(provider);
        int defaultMaxTokens = provider.getDefaultMaxTokens();
        double defaultTemperature = provider.getDefaultTemperature();
        
        return of(defaultModel, defaultMaxTokens, defaultTemperature, 0.95, systemPrompt);
    }
    
    private static String getDefaultModel(LlmProvider provider) {
        return switch (provider) {
            case CLAUDE -> "claude-3-5-sonnet-20241022";
            case OPENAI -> "gpt-4o";
            case GEMINI -> "gemini-1.5-pro";
            case LLAMA -> "llama-3.1-70b-instruct";
            case COHERE -> "command-r-plus";
            case MISTRAL -> "mistral-large-2407";
        };
    }
    
    public static ProviderConfig debateConfig(LlmProvider provider, Position position, String topic) {
        Objects.requireNonNull(provider, "Provider cannot be null");
        Objects.requireNonNull(position, "Position cannot be null");
        Objects.requireNonNull(topic, "Topic cannot be null");
        
        String systemPrompt = createDebateSystemPrompt(position, topic);
        return forProvider(provider, systemPrompt);
    }
    
    private static String createDebateSystemPrompt(Position position, String topic) {
        return String.format("""
            You are participating in a structured debate on the topic: "%s"
            
            Your position: %s
            
            Guidelines:
            1. Present clear, logical arguments supporting your position
            2. Use evidence and examples to support your claims
            3. Address counterarguments thoughtfully
            4. Maintain a respectful and professional tone
            5. Be concise but thorough in your responses
            6. Focus on the strongest aspects of your position
            7. Acknowledge valid points from the opposition when appropriate
            
            Remember: Your goal is to present the most compelling case for your position while engaging constructively with opposing viewpoints.
            """, topic, position.value());
    }
    
    public ProviderConfig withModel(String newModel) {
        Objects.requireNonNull(newModel, "Model cannot be null");
        return new ProviderConfig(newModel, maxTokens, temperature, topP, systemPrompt, additionalParams);
    }
    
    public ProviderConfig withMaxTokens(int newMaxTokens) {
        return new ProviderConfig(model, newMaxTokens, temperature, topP, systemPrompt, additionalParams);
    }
    
    public ProviderConfig withTemperature(double newTemperature) {
        BigDecimal temp = BigDecimal.valueOf(newTemperature).setScale(2, RoundingMode.HALF_UP);
        return new ProviderConfig(model, maxTokens, temp, topP, systemPrompt, additionalParams);
    }
    
    public ProviderConfig withTopP(double newTopP) {
        BigDecimal top = BigDecimal.valueOf(newTopP).setScale(2, RoundingMode.HALF_UP);
        return new ProviderConfig(model, maxTokens, temperature, top, systemPrompt, additionalParams);
    }
    
    public ProviderConfig withSystemPrompt(String newSystemPrompt) {
        Objects.requireNonNull(newSystemPrompt, "System prompt cannot be null");
        return new ProviderConfig(model, maxTokens, temperature, topP, newSystemPrompt, additionalParams);
    }
    
    public ProviderConfig withAdditionalParam(String key, Object value) {
        Objects.requireNonNull(key, "Parameter key cannot be null");
        Objects.requireNonNull(value, "Parameter value cannot be null");
        
        Map<String, Object> newParams = Map.copyOf(additionalParams);
        newParams.put(key, value);
        
        return new ProviderConfig(model, maxTokens, temperature, topP, systemPrompt, newParams);
    }
    
    public boolean hasSystemPrompt() {
        return !systemPrompt.trim().isEmpty();
    }
    
    public boolean isHighCreativity() {
        return temperature.compareTo(BigDecimal.valueOf(0.8)) >= 0;
    }
    
    public boolean isLowCreativity() {
        return temperature.compareTo(BigDecimal.valueOf(0.3)) <= 0;
    }
    
    public boolean isOptimizedForDebate() {
        return hasSystemPrompt() && systemPrompt.toLowerCase().contains("debate");
    }
    
    @Override
    public String toString() {
        return String.format("ProviderConfig{model='%s', maxTokens=%d, temperature=%.2f, topP=%.2f}",
            model, maxTokens, temperature, topP);
    }
}