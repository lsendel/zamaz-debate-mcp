package com.zamaz.mcp.llm.application.port.outbound;

import com.zamaz.mcp.llm.domain.model.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Gateway interface for communicating with external LLM provider APIs.
 * This is an outbound port in hexagonal architecture.
 */
public interface LlmProviderGateway {
    
    /**
     * Generate a text completion using the specified provider and model.
     */
    Mono<CompletionResponse> generateCompletion(
        ProviderId providerId,
        ModelName model,
        CompletionRequest request
    );
    
    /**
     * Generate a streaming text completion.
     */
    Flux<CompletionChunk> generateStreamingCompletion(
        ProviderId providerId,
        ModelName model,
        CompletionRequest request
    );
    
    /**
     * Check the health status of a specific provider.
     */
    Mono<ProviderHealthCheck> checkProviderHealth(ProviderId providerId);
    
    /**
     * Get available models for a specific provider.
     */
    Mono<List<ModelInfo>> getAvailableModels(ProviderId providerId);
    
    /**
     * Estimate token count for a prompt using provider-specific tokenization.
     */
    Mono<Integer> estimateTokenCount(
        ProviderId providerId,
        ModelName model,
        PromptContent prompt
    );
    
    /**
     * Check if a provider supports a specific capability.
     */
    boolean supportsCapability(ProviderId providerId, LlmModel.ModelCapability capability);
    
    /**
     * Response from a completion request.
     */
    record CompletionResponse(
        String content,
        TokenUsage usage,
        String finishReason,
        String model,
        long latencyMs
    ) {}
    
    /**
     * Chunk of a streaming completion response.
     */
    record CompletionChunk(
        String content,
        boolean isComplete,
        String finishReason
    ) {}
    
    /**
     * Health check result for a provider.
     */
    record ProviderHealthCheck(
        ProviderId providerId,
        ProviderStatus status,
        String message,
        long responseTimeMs,
        java.time.Instant checkedAt
    ) {}
    
    /**
     * Information about a model from the provider.
     */
    record ModelInfo(
        ModelName name,
        String displayName,
        int maxTokens,
        boolean supportsStreaming,
        boolean supportsSystemMessages,
        boolean supportsVision
    ) {}
}