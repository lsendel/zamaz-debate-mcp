package com.zamaz.mcp.rag.adapter.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zamaz.mcp.rag.adapter.web.dto.DocumentResponse;
import com.zamaz.mcp.rag.adapter.web.dto.SearchResponse;
import com.zamaz.mcp.rag.application.command.UploadDocumentCommand;
import com.zamaz.mcp.rag.application.query.ListDocumentsQuery;
import com.zamaz.mcp.rag.domain.model.*;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Mapper for converting between domain models and DTOs.
 */
@Component
public class DocumentMapper {
    
    private final ObjectMapper objectMapper;
    
    public DocumentMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * Convert upload request to command.
     */
    public UploadDocumentCommand toUploadCommand(
            OrganizationId organizationId,
            String title,
            MultipartFile file,
            String metadataJson) throws IOException {
        
        // Parse metadata if provided
        Map<String, String> metadata = new HashMap<>();
        if (metadataJson != null && !metadataJson.isBlank()) {
            try {
                metadata = objectMapper.readValue(metadataJson, Map.class);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Invalid metadata JSON", e);
            }
        }
        
        // Create file info
        FileInfo fileInfo = FileInfo.of(
            file.getOriginalFilename(),
            file.getContentType(),
            file.getSize()
        );
        
        // Create document content
        DocumentContent content = DocumentContent.of(
            new String(file.getBytes()),
            file.getContentType()
        );
        
        // Create document metadata
        DocumentMetadata documentMetadata = DocumentMetadata.of(title, metadata);
        
        return UploadDocumentCommand.of(
            organizationId,
            content,
            documentMetadata,
            fileInfo
        );
    }
    
    /**
     * Convert parameters to list query.
     */
    public ListDocumentsQuery toListQuery(
            OrganizationId organizationId,
            String status,
            String title,
            Integer limit,
            Integer offset) {
        
        // Parse status filter
        List<DocumentStatus> statuses = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            try {
                statuses.add(DocumentStatus.valueOf(status.toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Invalid status, ignore filter
            }
        }
        
        return ListDocumentsQuery.withFilters(
            organizationId,
            statuses.isEmpty() ? null : statuses,
            title,
            limit,
            offset
        );
    }
    
    /**
     * Convert domain document to response DTO.
     */
    public DocumentResponse toResponse(Document document) {
        FileInfo fileInfo = document.getFileInfo();
        
        return new DocumentResponse(
            document.getId().toString(),
            document.getOrganizationId().toString(),
            document.getMetadata().title(),
            document.getStatus().name().toLowerCase(),
            fileInfo.fileName(),
            fileInfo.fileType(),
            fileInfo.fileSize(),
            new HashMap<>(document.getMetadata().properties()),
            document.getChunkCount(),
            document.getCreatedAt(),
            document.getUpdatedAt()
        );
    }
    
    /**
     * Convert document chunks to search response.
     */
    public SearchResponse toSearchResponse(List<DocumentChunk> chunks, String query) {
        List<SearchResponse.SearchResult> results = chunks.stream()
            .map(chunk -> new SearchResponse.SearchResult(
                chunk.getId().toString(),
                chunk.getDocumentId().toString(),
                chunk.getDocumentTitle(),
                chunk.getContent().text(),
                chunk.getSimilarityScore(),
                chunk.getChunkNumber(),
                highlightMatch(chunk.getContent().text(), query)
            ))
            .collect(Collectors.toList());
        
        return new SearchResponse(
            query,
            results.size(),
            results
        );
    }
    
    /**
     * Create a simple highlight of matching terms.
     */
    private String highlightMatch(String content, String query) {
        // Simple implementation - in production would use more sophisticated highlighting
        String[] terms = query.toLowerCase().split("\\s+");
        String highlighted = content;
        
        for (String term : terms) {
            // Find the term case-insensitively and wrap in highlight tags
            highlighted = highlighted.replaceAll(
                "(?i)(" + Pattern.quote(term) + ")",
                "<mark>$1</mark>"
            );
        }
        
        // Return first 200 characters with highlights
        if (highlighted.length() > 200) {
            highlighted = highlighted.substring(0, 200) + "...";
        }
        
        return highlighted;
    }
}