package com.zamaz.mcp.rag.application.query;

import com.zamaz.mcp.rag.domain.model.OrganizationId;
import com.zamaz.mcp.rag.domain.model.SearchQuery;
import java.util.List;
import java.util.Objects;

/**
 * Query to search documents semantically.
 */
public record SearchDocumentsQuery(
    OrganizationId organizationId,
    String searchText,
    int maxResults,
    double minSimilarity,
    List<String> documentFilters
) {
    
    public SearchDocumentsQuery {
        Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        Objects.requireNonNull(searchText, "Search text cannot be null");
        Objects.requireNonNull(documentFilters, "Document filters cannot be null");
        
        if (searchText.trim().isEmpty()) {
            throw new IllegalArgumentException("Search text cannot be empty");
        }
        
        if (maxResults < 1 || maxResults > 100) {
            throw new IllegalArgumentException("Max results must be between 1 and 100");
        }
        
        if (minSimilarity < 0.0 || minSimilarity > 1.0) {
            throw new IllegalArgumentException("Min similarity must be between 0.0 and 1.0");
        }
    }
    
    public static SearchDocumentsQuery simple(OrganizationId organizationId, String searchText) {
        return new SearchDocumentsQuery(organizationId, searchText.trim(), 10, 0.0, List.of());
    }
    
    public static SearchDocumentsQuery withLimit(OrganizationId organizationId, String searchText, int maxResults) {
        return new SearchDocumentsQuery(organizationId, searchText.trim(), maxResults, 0.0, List.of());
    }
    
    public static SearchDocumentsQuery withSimilarity(OrganizationId organizationId, String searchText, 
                                                    int maxResults, double minSimilarity) {
        return new SearchDocumentsQuery(organizationId, searchText.trim(), maxResults, minSimilarity, List.of());
    }
    
    public SearchQuery toDomainSearchQuery() {
        return SearchQuery.forOrganization(searchText, organizationId)
            .withMaxResults(maxResults)
            .withMinSimilarity(minSimilarity)
            .withFilters(documentFilters);
    }
    
    public boolean hasFilters() {
        return !documentFilters.isEmpty();
    }
    
    public boolean isStrictSearch() {
        return minSimilarity > 0.7;
    }
    
    public boolean isLooseSearch() {
        return minSimilarity < 0.3;
    }
}