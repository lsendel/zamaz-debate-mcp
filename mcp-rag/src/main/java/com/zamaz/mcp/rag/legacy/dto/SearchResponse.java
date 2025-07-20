package com.zamaz.mcp.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for search results.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResponse {
    
    private String query;
    private Integer totalResults;
    private List<SearchResult> results;
    private Long searchTimeMs;
}