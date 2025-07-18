package com.zamaz.mcp.llm.domain.model;

import com.zamaz.mcp.common.domain.model.DomainEntity;
import com.zamaz.mcp.common.domain.exception.DomainRuleViolationException;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Entity representing an LLM model with its capabilities and pricing.
 * Part of the Provider aggregate.
 */
public class LlmModel extends DomainEntity<ModelName> {
    
    private final ProviderId providerId;
    private final String displayName;
    private final int maxTokens;
    private final boolean supportsStreaming;
    private final boolean supportsSystemMessages;
    private final boolean supportsVision;
    private final BigDecimal inputTokenCost; // Cost per 1000 tokens
    private final BigDecimal outputTokenCost; // Cost per 1000 tokens
    private final ModelStatus status;
    
    private LlmModel(
            ModelName modelName,
            ProviderId providerId,
            String displayName,
            int maxTokens,
            boolean supportsStreaming,
            boolean supportsSystemMessages,
            boolean supportsVision,
            BigDecimal inputTokenCost,
            BigDecimal outputTokenCost,
            ModelStatus status
    ) {
        super(modelName);
        this.providerId = Objects.requireNonNull(providerId, "Provider ID cannot be null");
        this.displayName = Objects.requireNonNull(displayName, "Display name cannot be null");
        this.maxTokens = maxTokens;
        this.supportsStreaming = supportsStreaming;
        this.supportsSystemMessages = supportsSystemMessages;
        this.supportsVision = supportsVision;
        this.inputTokenCost = Objects.requireNonNull(inputTokenCost, "Input token cost cannot be null");
        this.outputTokenCost = Objects.requireNonNull(outputTokenCost, "Output token cost cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
    }
    
    public static LlmModel create(
            ModelName modelName,
            ProviderId providerId,
            String displayName,
            int maxTokens,
            boolean supportsStreaming,
            boolean supportsSystemMessages,
            boolean supportsVision,
            BigDecimal inputTokenCost,
            BigDecimal outputTokenCost
    ) {
        return new LlmModel(
            modelName,
            providerId,
            displayName,
            maxTokens,
            supportsStreaming,
            supportsSystemMessages,
            supportsVision,
            inputTokenCost,
            outputTokenCost,
            ModelStatus.AVAILABLE
        );
    }
    
    public TokenUsage calculateCost(int inputTokens, int outputTokens) {
        BigDecimal inputCost = inputTokenCost.multiply(BigDecimal.valueOf(inputTokens))
            .divide(BigDecimal.valueOf(1000), 6, BigDecimal.ROUND_HALF_UP);
        BigDecimal outputCost = outputTokenCost.multiply(BigDecimal.valueOf(outputTokens))
            .divide(BigDecimal.valueOf(1000), 6, BigDecimal.ROUND_HALF_UP);
        
        return TokenUsage.withCost(inputTokens, outputTokens, inputCost, outputCost);
    }
    
    public boolean canHandleRequest(int requestTokens) {
        return requestTokens <= maxTokens && status.isAvailable();
    }
    
    public boolean isAvailable() {
        return status.isAvailable();
    }
    
    public boolean hasCapability(ModelCapability capability) {
        return switch (capability) {
            case STREAMING -> supportsStreaming;
            case SYSTEM_MESSAGES -> supportsSystemMessages;
            case VISION -> supportsVision;
            case TEXT_COMPLETION -> true; // All models support basic text completion
        };
    }
    
    @Override
    public void validateInvariants() {
        if (maxTokens <= 0) {
            throw new DomainRuleViolationException(
                "Model.maxTokens.invalid",
                "Max tokens must be positive"
            );
        }
        if (inputTokenCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainRuleViolationException(
                "Model.inputTokenCost.invalid",
                "Input token cost cannot be negative"
            );
        }
        if (outputTokenCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainRuleViolationException(
                "Model.outputTokenCost.invalid",
                "Output token cost cannot be negative"
            );
        }
        if (displayName == null || displayName.trim().isEmpty()) {
            throw new DomainRuleViolationException(
                "Model.displayName.required",
                "Display name is required"
            );
        }
    }
    
    // Getters
    public ModelName getModelName() {
        return getId();
    }
    
    public ProviderId getProviderId() {
        return providerId;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getMaxTokens() {
        return maxTokens;
    }
    
    public boolean isSupportsStreaming() {
        return supportsStreaming;
    }
    
    public boolean isSupportsSystemMessages() {
        return supportsSystemMessages;
    }
    
    public boolean isSupportsVision() {
        return supportsVision;
    }
    
    public BigDecimal getInputTokenCost() {
        return inputTokenCost;
    }
    
    public BigDecimal getOutputTokenCost() {
        return outputTokenCost;
    }
    
    public ModelStatus getStatus() {
        return status;
    }
    
    /**
     * Enum representing model capabilities.
     */
    public enum ModelCapability {
        TEXT_COMPLETION,
        STREAMING,
        SYSTEM_MESSAGES,
        VISION
    }
    
    /**
     * Enum representing model status.
     */
    public enum ModelStatus {
        AVAILABLE,
        DEPRECATED,
        UNAVAILABLE;
        
        public boolean isAvailable() {
            return this == AVAILABLE;
        }
    }
}