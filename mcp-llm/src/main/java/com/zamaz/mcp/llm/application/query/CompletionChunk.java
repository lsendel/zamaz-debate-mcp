package com.zamaz.mcp.llm.application.query;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Result DTO for streaming completion chunks.
 * Represents a single chunk of content in a streaming response.
 */
public record CompletionChunk(
    String requestId,
    String streamId,
    int chunkIndex,
    String content,
    boolean isDelta,
    boolean isLast,
    Optional<String> finishReason,
    Instant timestamp,
    Map<String, Object> metadata
) {
    
    public CompletionChunk {
        Objects.requireNonNull(requestId, "Request ID cannot be null");
        Objects.requireNonNull(streamId, "Stream ID cannot be null");
        Objects.requireNonNull(content, "Content cannot be null");
        Objects.requireNonNull(finishReason, "Finish reason cannot be null");
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        Objects.requireNonNull(metadata, "Metadata cannot be null");
        
        if (requestId.trim().isEmpty()) {
            throw new IllegalArgumentException("Request ID cannot be empty");
        }
        
        if (streamId.trim().isEmpty()) {
            throw new IllegalArgumentException("Stream ID cannot be empty");
        }
        
        if (chunkIndex < 0) {
            throw new IllegalArgumentException("Chunk index cannot be negative");
        }
        
        if (isLast && finishReason.isEmpty()) {
            throw new IllegalArgumentException("Last chunk must have a finish reason");
        }
    }
    
    /**
     * Creates a basic content chunk.
     * 
     * @param requestId the request identifier
     * @param streamId the stream identifier
     * @param chunkIndex the index of this chunk in the stream
     * @param content the content of this chunk
     * @return a new CompletionChunk
     */
    public static CompletionChunk of(
            String requestId,
            String streamId,
            int chunkIndex,
            String content
    ) {
        return new CompletionChunk(
            requestId,
            streamId,
            chunkIndex,
            content,
            true, // Default to delta mode
            false, // Not last by default
            Optional.empty(),
            Instant.now(),
            Map.of()
        );
    }
    
    /**
     * Creates a delta chunk (incremental content).
     * 
     * @param requestId the request identifier
     * @param streamId the stream identifier
     * @param chunkIndex the index of this chunk in the stream
     * @param deltaContent the incremental content
     * @return a new CompletionChunk in delta mode
     */
    public static CompletionChunk delta(
            String requestId,
            String streamId,
            int chunkIndex,
            String deltaContent
    ) {
        return new CompletionChunk(
            requestId,
            streamId,
            chunkIndex,
            deltaContent,
            true, // Delta mode
            false,
            Optional.empty(),
            Instant.now(),
            Map.of("chunk_type", "delta")
        );
    }
    
    /**
     * Creates a full content chunk (complete content so far).
     * 
     * @param requestId the request identifier
     * @param streamId the stream identifier
     * @param chunkIndex the index of this chunk in the stream
     * @param fullContent the complete content so far
     * @return a new CompletionChunk with full content
     */
    public static CompletionChunk full(
            String requestId,
            String streamId,
            int chunkIndex,
            String fullContent
    ) {
        return new CompletionChunk(
            requestId,
            streamId,
            chunkIndex,
            fullContent,
            false, // Full content mode
            false,
            Optional.empty(),
            Instant.now(),
            Map.of("chunk_type", "full")
        );
    }
    
    /**
     * Creates the final chunk that ends the stream.
     * 
     * @param requestId the request identifier
     * @param streamId the stream identifier
     * @param chunkIndex the index of this chunk in the stream
     * @param finishReason why the stream ended
     * @return a new CompletionChunk that ends the stream
     */
    public static CompletionChunk last(
            String requestId,
            String streamId,
            int chunkIndex,
            String finishReason
    ) {
        return new CompletionChunk(
            requestId,
            streamId,
            chunkIndex,
            "", // No content in final chunk
            false,
            true, // This is the last chunk
            Optional.of(finishReason),
            Instant.now(),
            Map.of("chunk_type", "final", "finish_reason", finishReason)
        );
    }
    
    /**
     * Creates a chunk with custom metadata.
     * 
     * @param requestId the request identifier
     * @param streamId the stream identifier
     * @param chunkIndex the index of this chunk in the stream
     * @param content the content of this chunk
     * @param metadata additional metadata
     * @return a new CompletionChunk with metadata
     */
    public static CompletionChunk withMetadata(
            String requestId,
            String streamId,
            int chunkIndex,
            String content,
            Map<String, Object> metadata
    ) {
        return new CompletionChunk(
            requestId,
            streamId,
            chunkIndex,
            content,
            true,
            false,
            Optional.empty(),
            Instant.now(),
            Map.copyOf(metadata)
        );
    }
    
    /**
     * Creates an error chunk to signal streaming errors.
     * 
     * @param requestId the request identifier
     * @param streamId the stream identifier
     * @param chunkIndex the index of this chunk in the stream
     * @param errorMessage the error message
     * @return a new CompletionChunk representing an error
     */
    public static CompletionChunk error(
            String requestId,
            String streamId,
            int chunkIndex,
            String errorMessage
    ) {
        return new CompletionChunk(
            requestId,
            streamId,
            chunkIndex,
            "",
            false,
            true, // Error ends the stream
            Optional.of("error"),
            Instant.now(),
            Map.of(
                "chunk_type", "error",
                "error_message", errorMessage,
                "is_error", true
            )
        );
    }
    
    /**
     * Checks if this chunk contains actual content.
     * 
     * @return true if the chunk has non-empty content
     */
    public boolean hasContent() {
        return content != null && !content.isEmpty();
    }
    
    /**
     * Checks if this chunk represents an error.
     * 
     * @return true if this is an error chunk
     */
    public boolean isError() {
        return metadata.containsKey("is_error") && 
               Boolean.TRUE.equals(metadata.get("is_error"));
    }
    
    /**
     * Gets the error message if this is an error chunk.
     * 
     * @return the error message, or empty if not an error chunk
     */
    public Optional<String> getErrorMessage() {
        if (isError()) {
            return Optional.ofNullable((String) metadata.get("error_message"));
        }
        return Optional.empty();
    }
    
    /**
     * Gets the chunk type from metadata.
     * 
     * @return the chunk type (delta, full, final, error)
     */
    public String getChunkType() {
        return (String) metadata.getOrDefault("chunk_type", "delta");
    }
}