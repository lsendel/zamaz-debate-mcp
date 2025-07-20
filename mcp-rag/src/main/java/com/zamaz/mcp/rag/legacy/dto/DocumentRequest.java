package com.zamaz.mcp.rag.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for storing a document.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentRequest {
    
    @NotBlank(message = "Organization ID is required")
    private String organizationId;
    
    @NotBlank(message = "Document ID is required")
    private String documentId;
    
    @NotBlank(message = "Title is required")
    private String title;
    
    @NotBlank(message = "Content is required")
    private String content;
    
    private String fileName;
    private String fileType;
    private Long fileSize;
    private Map<String, String> metadata;
}