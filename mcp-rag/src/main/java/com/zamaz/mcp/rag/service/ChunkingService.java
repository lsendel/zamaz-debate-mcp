package com.zamaz.mcp.rag.service;

import com.zamaz.mcp.rag.entity.DocumentChunk;

import java.util.List;

/**
 * Service interface for document chunking.
 */
public interface ChunkingService {
    
    /**
     * Chunk a document into smaller pieces.
     */
    List<DocumentChunk> chunkDocument(String content, String documentId, String organizationId);
    
    /**
     * Get optimal chunk size based on content type.
     */
    int getOptimalChunkSize(String contentType);
    
    /**
     * Get chunk overlap size.
     */
    int getChunkOverlap();
}