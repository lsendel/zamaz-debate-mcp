package com.zamaz.mcp.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Search result item.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResult {
    
    private String documentId;
    private String chunkId;
    private String title;
    private String content;
    private Double score;
    private Integer chunkIndex;
    private Map<String, Object> metadata;
    private String highlightedContent;
}