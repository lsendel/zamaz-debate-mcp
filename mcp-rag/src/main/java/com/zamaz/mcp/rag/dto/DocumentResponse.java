package com.zamaz.mcp.rag.dto;

import com.zamaz.mcp.rag.entity.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response DTO for document information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentResponse {
    
    private String id;
    private String organizationId;
    private String documentId;
    private String title;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private Map<String, String> metadata;
    private DocumentStatus status;
    private Integer chunkCount;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime processedAt;
}