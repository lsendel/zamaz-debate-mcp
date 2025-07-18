package com.zamaz.mcp.llm.adapter.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Web DTO for completion responses.
 */
public record CompletionResponse(
    String content,
    UsageInfo usage,
    String provider,
    String model,
    String finishReason,
    Instant timestamp,
    long durationMs,
    boolean fromCache
) {
    
    /**
     * Nested DTO for token usage information.
     */
    public record UsageInfo(
        int inputTokens,
        int outputTokens,
        int totalTokens,
        BigDecimal totalCost
    ) {}
}