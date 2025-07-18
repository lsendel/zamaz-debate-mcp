package com.zamaz.mcp.debateengine.domain.port;

import com.zamaz.mcp.debateengine.domain.model.AIModel;
import com.zamaz.mcp.debateengine.domain.model.Context;
import com.zamaz.mcp.debateengine.domain.model.QualityScore;

import java.util.concurrent.CompletableFuture;

/**
 * Port for AI service interactions.
 */
public interface AIService {
    
    /**
     * Generate a response using the specified AI model.
     */
    CompletableFuture<AIResponse> generateResponse(
        AIModel model,
        Context context,
        String prompt
    );
    
    /**
     * Analyze response quality.
     */
    CompletableFuture<QualityScore> analyzeQuality(
        String response,
        String topic,
        String position
    );
    
    /**
     * Count tokens in text.
     */
    int countTokens(String text, String model);
    
    /**
     * Response from AI service.
     */
    record AIResponse(
        String content,
        int tokenCount,
        long responseTimeMs,
        String modelUsed
    ) {}
}