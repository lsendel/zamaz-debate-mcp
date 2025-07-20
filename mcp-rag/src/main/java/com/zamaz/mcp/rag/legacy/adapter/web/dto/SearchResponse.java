package com.zamaz.mcp.rag.adapter.web.dto;

import java.util.List;

/**
 * Response DTO for document search results.
 */
public record SearchResponse(
    String query,
    Integer totalResults,
    List<SearchResult> results
) {
    
    /**
     * Individual search result.
     */
    public record SearchResult(
        String chunkId,
        String documentId,
        String documentTitle,
        String content,
        Double similarity,
        Integer chunkNumber,
        String highlight
    ) {}
}