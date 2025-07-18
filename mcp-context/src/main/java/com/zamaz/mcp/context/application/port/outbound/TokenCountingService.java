package com.zamaz.mcp.context.application.port.outbound;

import com.zamaz.mcp.context.domain.model.MessageContent;
import com.zamaz.mcp.context.domain.model.TokenCount;

/**
 * Outbound port for token counting functionality.
 * Implementations may use different tokenization strategies.
 */
public interface TokenCountingService {
    
    /**
     * Count tokens in the given text for a specific model.
     * 
     * @param content The text content to count tokens for
     * @param model The LLM model name (e.g., "gpt-4", "claude-3")
     * @return The token count
     */
    TokenCount countTokens(MessageContent content, String model);
    
    /**
     * Count tokens using the default model.
     * 
     * @param content The text content to count tokens for
     * @return The token count
     */
    TokenCount countTokens(MessageContent content);
    
    /**
     * Truncate text to fit within a token limit.
     * 
     * @param content The text to truncate
     * @param maxTokens The maximum token count
     * @param model The LLM model name
     * @return The truncated content
     */
    MessageContent truncateToTokenLimit(MessageContent content, TokenCount maxTokens, String model);
    
    /**
     * Estimate tokens without exact counting (faster but less accurate).
     * 
     * @param content The text content
     * @return Estimated token count
     */
    TokenCount estimateTokens(MessageContent content);
    
    /**
     * Check if a specific model is supported.
     * 
     * @param model The model name to check
     * @return true if the model is supported
     */
    boolean isModelSupported(String model);
    
    /**
     * Get the default model used for token counting.
     * 
     * @return The default model name
     */
    String getDefaultModel();
}