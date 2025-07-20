package com.zamaz.mcp.rag.service;

import com.zamaz.mcp.rag.dto.SearchResult;

import java.util.List;
import java.util.Map;

/**
 * Service interface for vector storage operations.
 */
public interface VectorStoreService {
    
    /**
     * Store a vector with metadata.
     */
    String storeVector(String id, List<Float> vector, Map<String, Object> metadata);
    
    /**
     * Store multiple vectors in batch.
     */
    void storeVectors(Map<String, List<Float>> vectors, Map<String, Map<String, Object>> metadata);
    
    /**
     * Search for similar vectors.
     */
    List<SearchResult> searchSimilar(List<Float> queryVector, String organizationId, int limit, double threshold);
    
    /**
     * Delete a vector by ID.
     */
    void deleteVector(String id);
    
    /**
     * Delete vectors by metadata filter.
     */
    void deleteVectorsByFilter(Map<String, Object> filter);
    
    /**
     * Create or update collection for an organization.
     */
    void ensureCollection(String organizationId);
}