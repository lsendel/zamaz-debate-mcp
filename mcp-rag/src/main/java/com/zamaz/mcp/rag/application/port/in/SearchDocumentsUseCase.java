package com.zamaz.mcp.rag.application.port.in;

import com.zamaz.mcp.rag.domain.model.document.OrganizationId;
import com.zamaz.mcp.rag.domain.model.search.SearchResult;

import java.util.List;
import java.util.Set;

/**
 * Inbound port for searching documents using vector similarity.
 * This interface defines the contract for document search use case.
 */
public interface SearchDocumentsUseCase {
    
    /**
     * Search for documents based on query
     * 
     * @param query The search query
     * @return List of search results ordered by relevance
     */
    List<SearchResult> search(SearchQuery query);
    
    /**
     * Query object for document search
     */
    record SearchQuery(
        String queryText,
        OrganizationId organizationId,
        int topK,
        double minScore,
        Set<String> tags,
        boolean includeContent
    ) {
        public SearchQuery {
            if (queryText == null || queryText.trim().isEmpty()) {
                throw new IllegalArgumentException("Query text cannot be empty");
            }
            if (organizationId == null) {
                throw new IllegalArgumentException("Organization ID cannot be null");
            }
            if (topK <= 0 || topK > 100) {
                throw new IllegalArgumentException("TopK must be between 1 and 100");
            }
            if (minScore < 0.0 || minScore > 1.0) {
                throw new IllegalArgumentException("Min score must be between 0.0 and 1.0");
            }
            if (tags == null) {
                tags = Set.of();
            } else {
                tags = Set.copyOf(tags); // Defensive copy
            }
        }
        
        /**
         * Create a simple search query with defaults
         */
        public static SearchQuery simple(String queryText, OrganizationId organizationId) {
            return new SearchQuery(queryText, organizationId, 10, 0.7, Set.of(), true);
        }
        
        /**
         * Create a strict search query with higher threshold
         */
        public static SearchQuery strict(String queryText, OrganizationId organizationId) {
            return new SearchQuery(queryText, organizationId, 5, 0.85, Set.of(), true);
        }
        
        /**
         * Create a broad search query with lower threshold
         */
        public static SearchQuery broad(String queryText, OrganizationId organizationId) {
            return new SearchQuery(queryText, organizationId, 20, 0.6, Set.of(), true);
        }
    }
}