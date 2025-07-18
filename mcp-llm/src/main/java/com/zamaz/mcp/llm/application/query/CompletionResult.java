package com.zamaz.mcp.llm.application.query;

import com.zamaz.mcp.llm.domain.model.TokenUsage;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Result DTO for completion responses.
 * Contains the generated content, usage statistics, and metadata.
 */
public record CompletionResult(
    String requestId,
    String content,
    TokenUsage usage,
    String providerId,
    String modelName,
    Instant createdAt,
    Instant completedAt,
    boolean fromCache,
    Optional<String> finishReason,
    Map<String, Object> metadata
) {
    
    public CompletionResult {
        Objects.requireNonNull(requestId, "Request ID cannot be null");
        Objects.requireNonNull(content, "Content cannot be null");
        Objects.requireNonNull(usage, "Usage cannot be null");
        Objects.requireNonNull(providerId, "Provider ID cannot be null");
        Objects.requireNonNull(modelName, "Model name cannot be null");
        Objects.requireNonNull(createdAt, "Created at cannot be null");
        Objects.requireNonNull(completedAt, "Completed at cannot be null");
        Objects.requireNonNull(finishReason, "Finish reason cannot be null");
        Objects.requireNonNull(metadata, "Metadata cannot be null");
        
        if (requestId.trim().isEmpty()) {
            throw new IllegalArgumentException("Request ID cannot be empty");
        }
        
        if (providerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Provider ID cannot be empty");
        }
        
        if (modelName.trim().isEmpty()) {
            throw new IllegalArgumentException("Model name cannot be empty");
        }
        
        if (completedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Completed at cannot be before created at");
        }
    }
    
    /**
     * Creates a basic completion result.
     * 
     * @param requestId the request identifier
     * @param content the generated content
     * @param usage the token usage statistics
     * @param providerId the provider that generated the content
     * @param modelName the model that was used
     * @return a new CompletionResult
     */
    public static CompletionResult of(
            String requestId,
            String content,
            TokenUsage usage,
            String providerId,
            String modelName
    ) {
        Instant now = Instant.now();
        return new CompletionResult(
            requestId,
            content,
            usage,
            providerId,
            modelName,
            now,
            now,
            false,
            Optional.empty(),
            Map.of()
        );
    }
    
    /**
     * Creates a completion result with timing information.
     * 
     * @param requestId the request identifier
     * @param content the generated content
     * @param usage the token usage statistics
     * @param providerId the provider that generated the content
     * @param modelName the model that was used
     * @param createdAt when the request was created
     * @param completedAt when the request was completed
     * @return a new CompletionResult
     */
    public static CompletionResult withTiming(
            String requestId,
            String content,
            TokenUsage usage,
            String providerId,
            String modelName,
            Instant createdAt,
            Instant completedAt
    ) {
        return new CompletionResult(
            requestId,
            content,
            usage,
            providerId,
            modelName,
            createdAt,
            completedAt,
            false,
            Optional.empty(),
            Map.of()
        );
    }
    
    /**
     * Creates a cached completion result.
     * 
     * @param requestId the request identifier
     * @param content the cached content
     * @param usage the token usage statistics
     * @param providerId the original provider
     * @param modelName the original model
     * @param originalCreatedAt when the original request was created
     * @return a new CompletionResult marked as from cache
     */
    public static CompletionResult fromCache(
            String requestId,
            String content,
            TokenUsage usage,
            String providerId,
            String modelName,
            Instant originalCreatedAt
    ) {
        Instant now = Instant.now();
        return new CompletionResult(
            requestId,
            content,
            usage,
            providerId,
            modelName,
            originalCreatedAt,
            now,
            true, // From cache
            Optional.of("cached"),
            Map.of("cache_hit", true)
        );
    }
    
    /**
     * Creates a completion result with finish reason.
     * 
     * @param requestId the request identifier
     * @param content the generated content
     * @param usage the token usage statistics
     * @param providerId the provider that generated the content
     * @param modelName the model that was used
     * @param finishReason why the completion finished
     * @return a new CompletionResult
     */
    public static CompletionResult withFinishReason(
            String requestId,
            String content,
            TokenUsage usage,
            String providerId,
            String modelName,
            String finishReason
    ) {
        Instant now = Instant.now();
        return new CompletionResult(
            requestId,
            content,
            usage,
            providerId,
            modelName,
            now,
            now,
            false,
            Optional.of(finishReason),
            Map.of()
        );
    }
    
    /**
     * Creates a completion result with custom metadata.
     * 
     * @param requestId the request identifier
     * @param content the generated content
     * @param usage the token usage statistics
     * @param providerId the provider that generated the content
     * @param modelName the model that was used
     * @param metadata additional metadata
     * @return a new CompletionResult
     */
    public static CompletionResult withMetadata(
            String requestId,
            String content,
            TokenUsage usage,
            String providerId,
            String modelName,
            Map<String, Object> metadata
    ) {
        Instant now = Instant.now();
        return new CompletionResult(
            requestId,
            content,
            usage,
            providerId,
            modelName,
            now,
            now,
            false,
            Optional.empty(),
            Map.copyOf(metadata)
        );
    }
    
    /**
     * Calculates the duration of the completion request.
     * 
     * @return the duration in milliseconds
     */
    public long getDurationMs() {
        return completedAt.toEpochMilli() - createdAt.toEpochMilli();
    }
    
    /**
     * Checks if the completion was successful (has content).
     * 
     * @return true if the completion has content
     */
    public boolean isSuccessful() {
        return content != null && !content.trim().isEmpty();
    }
    
    /**
     * Gets the content length in characters.
     * 
     * @return the content length
     */
    public int getContentLength() {
        return content != null ? content.length() : 0;
    }
}