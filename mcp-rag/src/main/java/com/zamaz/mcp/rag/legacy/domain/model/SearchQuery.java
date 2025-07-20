package com.zamaz.mcp.rag.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Value object representing a search query for document retrieval.
 */
public record SearchQuery(
    String text,
    int maxResults,
    double minSimilarity,
    List<String> filters,
    Optional<OrganizationId> organizationId
) implements ValueObject {
    
    private static final int MIN_TEXT_LENGTH = 1;
    private static final int MAX_TEXT_LENGTH = 1000;
    private static final int MIN_RESULTS = 1;
    private static final int MAX_RESULTS = 100;
    private static final double MIN_SIMILARITY = 0.0;
    private static final double MAX_SIMILARITY = 1.0;
    
    public SearchQuery {
        Objects.requireNonNull(text, "Search text cannot be null");
        Objects.requireNonNull(filters, "Filters cannot be null");
        Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        
        if (text.trim().length() < MIN_TEXT_LENGTH) {
            throw new IllegalArgumentException("Search text cannot be empty");
        }
        
        if (text.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException(
                "Search text cannot exceed " + MAX_TEXT_LENGTH + " characters"
            );
        }
        
        if (maxResults < MIN_RESULTS || maxResults > MAX_RESULTS) {
            throw new IllegalArgumentException(
                "Max results must be between " + MIN_RESULTS + " and " + MAX_RESULTS
            );
        }
        
        if (minSimilarity < MIN_SIMILARITY || minSimilarity > MAX_SIMILARITY) {
            throw new IllegalArgumentException(
                "Min similarity must be between " + MIN_SIMILARITY + " and " + MAX_SIMILARITY
            );
        }
        
        // Create immutable copy of filters
        this.filters = List.copyOf(filters);
    }
    
    public static SearchQuery of(String text) {
        return new SearchQuery(text.trim(), 10, 0.0, List.of(), Optional.empty());
    }
    
    public static SearchQuery of(String text, int maxResults) {
        return new SearchQuery(text.trim(), maxResults, 0.0, List.of(), Optional.empty());
    }
    
    public static SearchQuery of(String text, int maxResults, double minSimilarity) {
        return new SearchQuery(text.trim(), maxResults, minSimilarity, List.of(), Optional.empty());
    }
    
    public static SearchQuery forOrganization(String text, OrganizationId organizationId) {
        Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        return new SearchQuery(text.trim(), 10, 0.0, List.of(), Optional.of(organizationId));
    }
    
    public SearchQuery withMaxResults(int maxResults) {
        return new SearchQuery(this.text, maxResults, this.minSimilarity, this.filters, this.organizationId);
    }
    
    public SearchQuery withMinSimilarity(double minSimilarity) {
        return new SearchQuery(this.text, this.maxResults, minSimilarity, this.filters, this.organizationId);
    }
    
    public SearchQuery withFilters(List<String> filters) {
        Objects.requireNonNull(filters, "Filters cannot be null");
        return new SearchQuery(this.text, this.maxResults, this.minSimilarity, filters, this.organizationId);
    }
    
    public SearchQuery withOrganization(OrganizationId organizationId) {
        Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        return new SearchQuery(this.text, this.maxResults, this.minSimilarity, this.filters, Optional.of(organizationId));
    }
    
    public SearchQuery withoutOrganization() {
        return new SearchQuery(this.text, this.maxResults, this.minSimilarity, this.filters, Optional.empty());
    }
    
    public boolean hasFilters() {
        return !filters.isEmpty();
    }
    
    public boolean hasOrganizationFilter() {
        return organizationId.isPresent();
    }
    
    public boolean isStrictSimilarity() {
        return minSimilarity > 0.5;
    }
    
    public boolean isLooserSimilarity() {
        return minSimilarity < 0.3;
    }
    
    public String getNormalizedText() {
        return text.toLowerCase().trim();
    }
    
    public int getTextLength() {
        return text.length();
    }
    
    public int getWordCount() {
        return text.trim().split("\\s+").length;
    }
    
    public boolean isShortQuery() {
        return getWordCount() <= 3;
    }
    
    public boolean isLongQuery() {
        return getWordCount() > 10;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SearchQuery{text='").append(text.length() > 50 ? text.substring(0, 47) + "..." : text).append("'");
        sb.append(", maxResults=").append(maxResults);
        sb.append(", minSimilarity=").append(minSimilarity);
        if (hasFilters()) {
            sb.append(", filters=").append(filters.size());
        }
        if (hasOrganizationFilter()) {
            sb.append(", org=").append(organizationId.get());
        }
        sb.append("}");
        return sb.toString();
    }
}