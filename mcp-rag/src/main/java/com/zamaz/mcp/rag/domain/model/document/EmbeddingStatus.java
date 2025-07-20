package com.zamaz.mcp.rag.domain.model.document;

/**
 * Enum representing the embedding status of a document.
 * Tracks the progress of embedding generation for document chunks.
 */
public enum EmbeddingStatus {
    /**
     * No embeddings have been generated yet
     */
    PENDING,
    
    /**
     * Embedding generation is currently in progress
     */
    IN_PROGRESS,
    
    /**
     * Some chunks have embeddings, but not all
     */
    PARTIAL,
    
    /**
     * All chunks have been successfully embedded
     */
    COMPLETE,
    
    /**
     * Embedding generation failed
     */
    FAILED;
    
    /**
     * Check if embeddings are being or have been processed
     */
    public boolean hasStartedProcessing() {
        return this != PENDING;
    }
    
    /**
     * Check if embedding process is complete (either success or failure)
     */
    public boolean isTerminal() {
        return this == COMPLETE || this == FAILED;
    }
    
    /**
     * Check if embeddings are available (complete or partial)
     */
    public boolean hasEmbeddings() {
        return this == COMPLETE || this == PARTIAL;
    }
}