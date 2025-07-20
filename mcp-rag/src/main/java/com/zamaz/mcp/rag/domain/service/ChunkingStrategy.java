package com.zamaz.mcp.rag.domain.service;

import com.zamaz.mcp.rag.domain.model.document.Document;
import com.zamaz.mcp.rag.domain.model.document.DocumentChunk;

import java.util.List;

/**
 * Domain Service interface for chunking strategies.
 * Represents different algorithms for splitting documents into chunks.
 * This is a pure domain interface with no implementation details.
 */
public interface ChunkingStrategy {
    
    /**
     * Split a document into chunks based on the strategy implementation.
     * 
     * @param document The document to chunk
     * @return List of document chunks
     * @throws ChunkingException if chunking fails
     */
    List<DocumentChunk> chunk(Document document);
    
    /**
     * Get the name of this chunking strategy
     */
    String getName();
    
    /**
     * Get the configuration parameters for this strategy
     */
    ChunkingParameters getParameters();
    
    /**
     * Validate if this strategy can be applied to the given document
     */
    boolean canProcess(Document document);
}

/**
 * Value object representing chunking parameters
 */
record ChunkingParameters(
    int maxChunkSize,
    int overlapSize,
    int minChunkSize,
    boolean preserveSentences,
    boolean preserveParagraphs
) {
    public ChunkingParameters {
        if (maxChunkSize <= 0) {
            throw new IllegalArgumentException("Max chunk size must be positive");
        }
        if (overlapSize < 0) {
            throw new IllegalArgumentException("Overlap size cannot be negative");
        }
        if (overlapSize >= maxChunkSize) {
            throw new IllegalArgumentException("Overlap size must be less than max chunk size");
        }
        if (minChunkSize <= 0 || minChunkSize > maxChunkSize) {
            throw new IllegalArgumentException("Min chunk size must be positive and less than max chunk size");
        }
    }
    
    /**
     * Default parameters for sliding window chunking
     */
    public static ChunkingParameters slidingWindow() {
        return new ChunkingParameters(512, 128, 50, true, false);
    }
    
    /**
     * Default parameters for paragraph-based chunking
     */
    public static ChunkingParameters paragraphBased() {
        return new ChunkingParameters(1000, 0, 100, true, true);
    }
    
    /**
     * Default parameters for semantic chunking
     */
    public static ChunkingParameters semantic() {
        return new ChunkingParameters(800, 200, 100, true, true);
    }
}

/**
 * Domain exception for chunking failures
 */
class ChunkingException extends RuntimeException {
    public ChunkingException(String message) {
        super(message);
    }
    
    public ChunkingException(String message, Throwable cause) {
        super(message, cause);
    }
}