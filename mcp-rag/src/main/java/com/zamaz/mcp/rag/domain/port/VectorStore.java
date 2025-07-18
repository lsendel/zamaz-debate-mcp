package com.zamaz.mcp.rag.domain.port;

import com.zamaz.mcp.rag.domain.model.ChunkId;
import com.zamaz.mcp.rag.domain.model.DocumentChunk;
import com.zamaz.mcp.rag.domain.model.Embedding;
import com.zamaz.mcp.rag.domain.model.OrganizationId;
import com.zamaz.mcp.rag.domain.model.SearchQuery;
import java.util.List;
import java.util.Optional;

/**
 * Port for vector storage and similarity search.
 */
public interface VectorStore {
    
    /**
     * Store an embedding with its associated chunk.
     * 
     * @param chunk the document chunk
     * @param embedding the embedding vector
     */
    void storeEmbedding(DocumentChunk chunk, Embedding embedding);
    
    /**
     * Store multiple embeddings in batch.
     * 
     * @param chunks the document chunks with their embeddings
     */
    void storeEmbeddings(List<DocumentChunk> chunks);
    
    /**
     * Search for similar chunks based on a query.
     * 
     * @param query the search query
     * @param queryEmbedding the embedding of the query
     * @return list of chunks ordered by similarity (highest first)
     */
    List<DocumentChunk> search(SearchQuery query, Embedding queryEmbedding);
    
    /**
     * Delete embeddings for a document.
     * 
     * @param documentId the document ID whose embeddings to delete
     * @return number of embeddings deleted
     */
    int deleteByDocumentId(com.zamaz.mcp.rag.domain.model.DocumentId documentId);
    
    /**
     * Delete embeddings for an organization.
     * 
     * @param organizationId the organization ID whose embeddings to delete
     * @return number of embeddings deleted
     */
    int deleteByOrganizationId(OrganizationId organizationId);
    
    /**
     * Get an embedding by chunk ID.
     * 
     * @param chunkId the chunk ID
     * @return the embedding if found
     */
    Optional<Embedding> getEmbedding(ChunkId chunkId);
    
    /**
     * Count embeddings for a document.
     * 
     * @param documentId the document ID
     * @return count of embeddings
     */
    long countByDocumentId(com.zamaz.mcp.rag.domain.model.DocumentId documentId);
    
    /**
     * Count embeddings for an organization.
     * 
     * @param organizationId the organization ID
     * @return count of embeddings
     */
    long countByOrganizationId(OrganizationId organizationId);
    
    /**
     * Check if the vector store is healthy and accessible.
     * 
     * @return true if healthy, false otherwise
     */
    boolean isHealthy();
    
    /**
     * Get information about the vector store.
     * 
     * @return store information (e.g., type, version, capacity)
     */
    VectorStoreInfo getInfo();
    
    record VectorStoreInfo(
        String type,
        String version,
        long totalVectors,
        long capacity,
        int dimensions
    ) {}
}