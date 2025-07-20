package com.zamaz.mcp.rag.domain.model.search;

import com.zamaz.mcp.rag.domain.model.document.OrganizationId;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Value Object representing a search query.
 * Encapsulates all parameters needed to perform a vector similarity search.
 */
public record SearchQuery(
    String queryText,
    OrganizationId organizationId,
    SearchParameters parameters,
    Set<String> documentTags
) {
    public SearchQuery {
        Objects.requireNonNull(queryText, "Query text cannot be null");
        Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        Objects.requireNonNull(parameters, "Search parameters cannot be null");
        Objects.requireNonNull(documentTags, "Document tags cannot be null");
        
        if (queryText.trim().isEmpty()) {
            throw new IllegalArgumentException("Query text cannot be empty");
        }
        
        // Make defensive copy to ensure immutability
        documentTags = Set.copyOf(documentTags);
    }
    
    /**
     * Create a simple search query with default parameters
     */
    public static SearchQuery simple(String queryText, OrganizationId organizationId) {
        return new SearchQuery(
            queryText,
            organizationId,
            SearchParameters.defaults(),
            Set.of()
        );
    }
    
    /**
     * Create a search query with custom parameters
     */
    public static SearchQuery withParameters(
            String queryText,
            OrganizationId organizationId,
            int topK,
            double minScore) {
        return new SearchQuery(
            queryText,
            organizationId,
            new SearchParameters(topK, minScore, true, Optional.empty()),
            Set.of()
        );
    }
    
    /**
     * Add document tags to filter results
     */
    public SearchQuery withTags(Set<String> tags) {
        return new SearchQuery(queryText, organizationId, parameters, tags);
    }
    
    /**
     * Check if this query has tag filters
     */
    public boolean hasTagFilters() {
        return !documentTags.isEmpty();
    }
}

/**
 * Parameters for search execution
 */
record SearchParameters(
    int topK,
    double minScore,
    boolean includeMetadata,
    Optional<String> embeddingModel
) {
    public SearchParameters {
        if (topK <= 0 || topK > 100) {
            throw new IllegalArgumentException("TopK must be between 1 and 100");
        }
        
        if (minScore < 0.0 || minScore > 1.0) {
            throw new IllegalArgumentException("Minimum score must be between 0.0 and 1.0");
        }
        
        Objects.requireNonNull(embeddingModel, "Embedding model optional cannot be null");
    }
    
    /**
     * Default search parameters
     */
    public static SearchParameters defaults() {
        return new SearchParameters(10, 0.7, true, Optional.empty());
    }
    
    /**
     * Create parameters for strict search
     */
    public static SearchParameters strict() {
        return new SearchParameters(5, 0.85, true, Optional.empty());
    }
    
    /**
     * Create parameters for broad search
     */
    public static SearchParameters broad() {
        return new SearchParameters(20, 0.6, true, Optional.empty());
    }
}