package com.zamaz.mcp.rag.infrastructure.adapter.in.web;

import com.zamaz.mcp.rag.application.port.in.SearchDocumentsUseCase;
import com.zamaz.mcp.rag.domain.model.document.OrganizationId;
import com.zamaz.mcp.rag.domain.model.search.SearchResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST controller for document search operations.
 * Inbound adapter implementing the web interface for search use cases.
 */
@RestController
@RequestMapping("/api/v1/search")
@Tag(name = "Search", description = "Document search endpoints")
public class SearchController {
    
    private static final Logger log = LoggerFactory.getLogger(SearchController.class);
    
    private final SearchDocumentsUseCase searchDocumentsUseCase;
    
    public SearchController(SearchDocumentsUseCase searchDocumentsUseCase) {
        this.searchDocumentsUseCase = Objects.requireNonNull(searchDocumentsUseCase);
    }
    
    @PostMapping
    @Operation(summary = "Search documents using vector similarity")
    public ResponseEntity<SearchResponse> searchDocuments(@RequestBody SearchRequest request) {
        log.info("Searching documents with query: '{}' for organization: {}", 
                request.query(), request.organizationId());
        
        // Create search query
        SearchDocumentsUseCase.SearchQuery query = new SearchDocumentsUseCase.SearchQuery(
            request.query(),
            OrganizationId.of(request.organizationId()),
            request.topK(),
            request.minScore(),
            request.tags() != null ? request.tags() : Set.of(),
            request.includeContent()
        );
        
        // Execute search
        List<SearchResult> results = searchDocumentsUseCase.search(query);
        
        // Convert to response DTOs
        List<SearchResultDto> resultDtos = results.stream()
                .map(this::toSearchResultDto)
                .collect(Collectors.toList());
        
        SearchResponse response = new SearchResponse(
            request.query(),
            resultDtos.size(),
            resultDtos
        );
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/simple")
    @Operation(summary = "Simple search with query parameters")
    public ResponseEntity<SearchResponse> simpleSearch(
            @RequestParam("q") String query,
            @RequestParam("organizationId") String organizationId,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        
        log.info("Simple search: '{}' for organization: {}", query, organizationId);
        
        // Create simple search request
        SearchRequest request = new SearchRequest(
            query,
            organizationId,
            limit,
            0.7,
            null,
            true
        );
        
        return searchDocuments(request);
    }
    
    private SearchResultDto toSearchResultDto(SearchResult result) {
        return new SearchResultDto(
            result.documentId().value(),
            result.chunkId().value(),
            result.metadata().documentTitle(),
            result.metadata().chunkIndex(),
            result.relevanceScore(),
            result.content().text(),
            result.getContentPreview(200)
        );
    }
    
    /**
     * Request DTO for document search
     */
    record SearchRequest(
        String query,
        String organizationId,
        int topK,
        double minScore,
        Set<String> tags,
        boolean includeContent
    ) {
        public SearchRequest {
            // Validation
            Objects.requireNonNull(query, "Query cannot be null");
            Objects.requireNonNull(organizationId, "Organization ID cannot be null");
            
            // Defaults
            if (topK <= 0 || topK > 100) topK = 10;
            if (minScore <= 0 || minScore > 1.0) minScore = 0.7;
        }
    }
    
    /**
     * Response DTO for search results
     */
    record SearchResponse(
        String query,
        int totalResults,
        List<SearchResultDto> results
    ) {}
    
    /**
     * DTO for individual search result
     */
    record SearchResultDto(
        String documentId,
        String chunkId,
        String documentTitle,
        int chunkIndex,
        double relevanceScore,
        String content,
        String contentPreview
    ) {}
}