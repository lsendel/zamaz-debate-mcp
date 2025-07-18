package com.zamaz.mcp.llm.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value object representing token usage and cost calculation for LLM requests.
 */
public record TokenUsage(
    int inputTokens,
    int outputTokens,
    BigDecimal inputCost,
    BigDecimal outputCost
) implements ValueObject {
    
    public TokenUsage {
        if (inputTokens < 0) {
            throw new IllegalArgumentException("Input tokens cannot be negative");
        }
        if (outputTokens < 0) {
            throw new IllegalArgumentException("Output tokens cannot be negative");
        }
        Objects.requireNonNull(inputCost, "Input cost cannot be null");
        Objects.requireNonNull(outputCost, "Output cost cannot be null");
        if (inputCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Input cost cannot be negative");
        }
        if (outputCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Output cost cannot be negative");
        }
    }
    
    public static TokenUsage zero() {
        return new TokenUsage(0, 0, BigDecimal.ZERO, BigDecimal.ZERO);
    }
    
    public static TokenUsage of(int inputTokens, int outputTokens) {
        return new TokenUsage(inputTokens, outputTokens, BigDecimal.ZERO, BigDecimal.ZERO);
    }
    
    public static TokenUsage withCost(
            int inputTokens, 
            int outputTokens, 
            BigDecimal inputCost, 
            BigDecimal outputCost
    ) {
        return new TokenUsage(inputTokens, outputTokens, inputCost, outputCost);
    }
    
    public int totalTokens() {
        return inputTokens + outputTokens;
    }
    
    public BigDecimal totalCost() {
        return inputCost.add(outputCost);
    }
    
    public TokenUsage add(TokenUsage other) {
        Objects.requireNonNull(other, "Other token usage cannot be null");
        return new TokenUsage(
            this.inputTokens + other.inputTokens,
            this.outputTokens + other.outputTokens,
            this.inputCost.add(other.inputCost),
            this.outputCost.add(other.outputCost)
        );
    }
    
    public TokenUsage withCosts(BigDecimal newInputCost, BigDecimal newOutputCost) {
        return new TokenUsage(inputTokens, outputTokens, newInputCost, newOutputCost);
    }
    
    public boolean hasUsage() {
        return inputTokens > 0 || outputTokens > 0;
    }
    
    public boolean hasCost() {
        return inputCost.compareTo(BigDecimal.ZERO) > 0 || outputCost.compareTo(BigDecimal.ZERO) > 0;
    }
    
    @Override
    public String toString() {
        return String.format("TokenUsage{input=%d, output=%d, total=%d, cost=$%.4f}", 
            inputTokens, outputTokens, totalTokens(), totalCost());
    }
}