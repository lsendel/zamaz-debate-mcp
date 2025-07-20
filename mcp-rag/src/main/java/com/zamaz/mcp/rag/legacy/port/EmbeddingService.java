package com.zamaz.mcp.rag.domain.port;

import com.zamaz.mcp.rag.domain.model.ChunkContent;
import com.zamaz.mcp.rag.domain.model.Embedding;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Port for embedding generation service.
 */
public interface EmbeddingService {
    
    /**
     * Generate embedding for a single chunk of text.
     * 
     * @param content the text content to embed
     * @return the generated embedding
     */
    Embedding generateEmbedding(ChunkContent content);
    
    /**
     * Generate embeddings for multiple chunks in batch.
     * 
     * @param contents the text contents to embed
     * @return list of generated embeddings in same order as input
     */
    List<Embedding> generateEmbeddings(List<ChunkContent> contents);
    
    /**
     * Generate embedding asynchronously.
     * 
     * @param content the text content to embed
     * @return future containing the generated embedding
     */
    CompletableFuture<Embedding> generateEmbeddingAsync(ChunkContent content);
    
    /**
     * Get the dimension of embeddings produced by this service.
     * 
     * @return embedding dimension
     */
    int getEmbeddingDimension();
    
    /**
     * Get the model name used for embeddings.
     * 
     * @return model name
     */
    String getModelName();
}