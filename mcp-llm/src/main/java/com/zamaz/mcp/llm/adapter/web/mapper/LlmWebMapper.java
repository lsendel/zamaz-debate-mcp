package com.zamaz.mcp.llm.adapter.web.mapper;

import com.zamaz.mcp.common.architecture.mapper.DomainMapper;
import com.zamaz.mcp.llm.adapter.web.dto.*;
import com.zamaz.mcp.llm.application.command.GenerateCompletionCommand;
import com.zamaz.mcp.llm.application.command.StreamCompletionCommand;
import com.zamaz.mcp.llm.application.query.*;
import com.zamaz.mcp.llm.domain.model.LlmModel;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between web DTOs and application commands/queries.
 */
@Component
public class LlmWebMapper implements DomainMapper {
    
    /**
     * Map CompletionRequest DTO to GenerateCompletionCommand.
     */
    public GenerateCompletionCommand toGenerateCommand(
            CompletionRequest request,
            String organizationId,
            String userId
    ) {
        return new GenerateCompletionCommand(
            request.prompt(),
            request.getModel(),
            request.getProvider(),
            request.maxTokens(),
            request.temperature(),
            request.isEnableCaching(),
            organizationId,
            userId
        );
    }
    
    /**
     * Map CompletionRequest DTO to StreamCompletionCommand.
     */
    public StreamCompletionCommand toStreamCommand(
            CompletionRequest request,
            String organizationId,
            String userId
    ) {
        return new StreamCompletionCommand(
            request.prompt(),
            request.getModel(),
            request.getProvider(),
            request.maxTokens(),
            request.temperature(),
            true, // Enable delta for streaming
            Optional.empty(), // Stream ID will be generated
            1024, // Default buffer size
            organizationId,
            userId
        );
    }
    
    /**
     * Map CompletionResult to CompletionResponse DTO.
     */
    public CompletionResponse toResponse(CompletionResult result) {
        return new CompletionResponse(
            result.content(),
            new CompletionResponse.UsageInfo(
                result.inputTokens(),
                result.outputTokens(),
                result.inputTokens() + result.outputTokens(),
                result.totalCost()
            ),
            result.provider(),
            result.model(),
            result.finishReason(),
            result.completedAt(),
            result.durationMs(),
            result.fromCache()
        );
    }
    
    /**
     * Map ProviderListResult to list of ProviderResponse DTOs.
     */
    public ProvidersListResponse toResponse(ProviderListResult result) {
        var providers = result.providers().stream()
            .map(this::toProviderResponse)
            .collect(Collectors.toList());
        
        return new ProvidersListResponse(
            providers,
            result.pagination(),
            result.aggregatedMetrics().orElse(null)
        );
    }
    
    /**
     * Map ProviderInfo to ProviderResponse DTO.
     */
    private ProviderResponse toProviderResponse(ProviderListResult.ProviderInfo providerInfo) {
        var models = providerInfo.models().stream()
            .map(this::toModelResponse)
            .collect(Collectors.toList());
        
        return new ProviderResponse(
            providerInfo.id(),
            providerInfo.name(),
            providerInfo.displayName(),
            providerInfo.description(),
            providerInfo.status(),
            models,
            new ProviderResponse.HealthInfo(
                providerInfo.status(),
                providerInfo.healthMessage(),
                providerInfo.lastHealthCheck(),
                0L // Response time not available in ProviderInfo
            ),
            providerInfo.configuration(),
            providerInfo.priority()
        );
    }
    
    /**
     * Map ModelInfo to ModelResponse DTO.
     */
    private ProviderResponse.ModelResponse toModelResponse(ProviderListResult.ModelInfo modelInfo) {
        return new ProviderResponse.ModelResponse(
            modelInfo.name(),
            modelInfo.displayName(),
            modelInfo.maxTokens(),
            modelInfo.inputTokenCost(),
            modelInfo.outputTokenCost(),
            modelInfo.capabilities(),
            modelInfo.status()
        );
    }
    
    /**
     * Map CompletionChunk to streaming response DTO.
     */
    public StreamingChunkResponse toStreamingResponse(CompletionChunk chunk) {
        return new StreamingChunkResponse(
            chunk.content(),
            chunk.isDelta(),
            chunk.isComplete(),
            chunk.finishReason().orElse(null),
            chunk.error().orElse(null)
        );
    }
    
    /**
     * Map capabilities enum to string list.
     */
    private java.util.List<String> mapCapabilities(java.util.List<LlmModel.ModelCapability> capabilities) {
        return capabilities.stream()
            .map(Enum::name)
            .map(String::toLowerCase)
            .collect(Collectors.toList());
    }
    
    /**
     * Response DTO for providers list endpoint.
     */
    public record ProvidersListResponse(
        java.util.List<ProviderResponse> providers,
        ProviderListResult.PaginationInfo pagination,
        ProviderListResult.ProviderMetrics aggregatedMetrics
    ) {}
    
    /**
     * Response DTO for streaming completion chunks.
     */
    public record StreamingChunkResponse(
        String content,
        boolean isDelta,
        boolean isComplete,
        String finishReason,
        String error
    ) {}
}