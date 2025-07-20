package com.zamaz.mcp.rag.domain.exception;

import com.zamaz.mcp.rag.domain.model.document.ChunkId;

/**
 * Exception thrown when embedding generation fails.
 */
public class EmbeddingGenerationException extends DomainException {
    
    private final ChunkId chunkId;
    
    public EmbeddingGenerationException(String message) {
        super(message, "EMBEDDING_GENERATION_ERROR");
        this.chunkId = null;
    }
    
    public EmbeddingGenerationException(ChunkId chunkId, String message) {
        super("Failed to generate embedding for chunk " + chunkId + ": " + message, "EMBEDDING_GENERATION_ERROR");
        this.chunkId = chunkId;
    }
    
    public EmbeddingGenerationException(ChunkId chunkId, String message, Throwable cause) {
        super("Failed to generate embedding for chunk " + chunkId + ": " + message, "EMBEDDING_GENERATION_ERROR", cause);
        this.chunkId = chunkId;
    }
    
    public ChunkId getChunkId() {
        return chunkId;
    }
}