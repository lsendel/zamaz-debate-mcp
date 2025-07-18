package com.zamaz.mcp.llm.adapter.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Web DTO for provider information.
 */
public record ProviderResponse(
    String id,
    String name,
    String displayName,
    String description,
    String status,
    List<ModelResponse> models,
    HealthInfo health,
    Map<String, Object> configuration,
    int priority
) {
    
    /**
     * Nested DTO for model information.
     */
    public record ModelResponse(
        String name,
        String displayName,
        int maxTokens,
        BigDecimal inputTokenCost,
        BigDecimal outputTokenCost,
        List<String> capabilities,
        String status
    ) {}
    
    /**
     * Nested DTO for provider health information.
     */
    public record HealthInfo(
        String status,
        String message,
        Instant lastChecked,
        long responseTimeMs
    ) {}
}