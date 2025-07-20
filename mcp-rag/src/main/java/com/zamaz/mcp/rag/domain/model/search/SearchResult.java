package com.zamaz.mcp.rag.domain.model.search;

import com.zamaz.mcp.rag.domain.model.document.ChunkId;
import com.zamaz.mcp.rag.domain.model.document.DocumentId;
import com.zamaz.mcp.rag.domain.model.document.ChunkContent;

import java.util.Objects;

/**
 * Value Object representing a search result.
 * Immutable representation of a document chunk that matches a search query.
 */
public record SearchResult(
    DocumentId documentId,
    ChunkId chunkId,
    ChunkContent content,
    double relevanceScore,
    SearchMetadata metadata
) {
    public SearchResult {
        Objects.requireNonNull(documentId, "Document ID cannot be null");
        Objects.requireNonNull(chunkId, "Chunk ID cannot be null");
        Objects.requireNonNull(content, "Content cannot be null");
        Objects.requireNonNull(metadata, "Metadata cannot be null");
        
        if (relevanceScore < 0.0 || relevanceScore > 1.0) {
            throw new IllegalArgumentException("Relevance score must be between 0.0 and 1.0");
        }
    }
    
    /**
     * Create a search result from similarity score
     */
    public static SearchResult fromSimilarity(
            DocumentId documentId,
            ChunkId chunkId,
            ChunkContent content,
            double similarityScore,
            String documentTitle,
            int chunkIndex) {
        
        SearchMetadata metadata = new SearchMetadata(
            documentTitle,
            chunkIndex,
            content.length(),
            similarityScore
        );
        
        return new SearchResult(documentId, chunkId, content, similarityScore, metadata);
    }
    
    /**
     * Check if this result meets a minimum relevance threshold
     */
    public boolean meetsThreshold(double threshold) {
        return relevanceScore >= threshold;
    }
    
    /**
     * Get a preview of the content
     */
    public String getContentPreview(int maxLength) {
        return content.preview(maxLength);
    }
    
    /**
     * Highlight search terms in the content
     */
    public String getHighlightedContent(String searchTerm) {
        return content.highlightTerm(searchTerm, "<mark>", "</mark>");
    }
}

/**
 * Metadata for a search result
 */
record SearchMetadata(
    String documentTitle,
    int chunkIndex,
    int chunkLength,
    double rawScore
) {
    public SearchMetadata {
        Objects.requireNonNull(documentTitle, "Document title cannot be null");
        
        if (chunkIndex < 0) {
            throw new IllegalArgumentException("Chunk index cannot be negative");
        }
        
        if (chunkLength <= 0) {
            throw new IllegalArgumentException("Chunk length must be positive");
        }
        
        if (rawScore < 0.0) {
            throw new IllegalArgumentException("Raw score cannot be negative");
        }
    }
}