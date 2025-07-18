package com.zamaz.mcp.llm.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;
import java.util.Objects;

/**
 * Value object representing the content of a prompt sent to an LLM.
 */
public record PromptContent(String value) implements ValueObject {
    
    private static final int MAX_LENGTH = 2_000_000; // 2M characters
    private static final int MIN_LENGTH = 1;
    
    public PromptContent {
        Objects.requireNonNull(value, "Prompt content cannot be null");
        if (value.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Prompt content cannot be empty");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                "Prompt content exceeds maximum length of " + MAX_LENGTH + " characters"
            );
        }
    }
    
    public static PromptContent of(String value) {
        return new PromptContent(value);
    }
    
    public int length() {
        return value.length();
    }
    
    public PromptContent truncate(int maxLength) {
        if (maxLength < MIN_LENGTH) {
            throw new IllegalArgumentException("Max length must be at least " + MIN_LENGTH);
        }
        if (value.length() <= maxLength) {
            return this;
        }
        return new PromptContent(value.substring(0, maxLength));
    }
    
    public PromptContent append(String additionalContent) {
        Objects.requireNonNull(additionalContent, "Additional content cannot be null");
        return new PromptContent(value + additionalContent);
    }
    
    public PromptContent prepend(String prefixContent) {
        Objects.requireNonNull(prefixContent, "Prefix content cannot be null");
        return new PromptContent(prefixContent + value);
    }
    
    public boolean contains(String substring) {
        return value.contains(substring);
    }
    
    public boolean isEmpty() {
        return value.trim().isEmpty();
    }
    
    public boolean isNotEmpty() {
        return !isEmpty();
    }
    
    /**
     * Estimates token count using a simple heuristic.
     * For accurate token counting, use a proper tokenizer.
     */
    public int estimateTokenCount() {
        // Simple heuristic: roughly 1 token per 4 characters
        return Math.max(1, value.length() / 4);
    }
    
    @Override
    public String toString() {
        if (value.length() <= 100) {
            return value;
        }
        return value.substring(0, 97) + "...";
    }
}