package com.zamaz.mcp.rag.service;

import java.util.List;

/**
 * Service interface for generating embeddings.
 */
public interface EmbeddingService {
    
    /**
     * Generate embedding for a single text.
     */
    List<Float> generateEmbedding(String text);
    
    /**
     * Generate embeddings for multiple texts.
     */
    List<List<Float>> generateEmbeddings(List<String> texts);
    
    /**
     * Get the embedding dimension.
     */
    int getEmbeddingDimension();
}