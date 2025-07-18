package com.zamaz.mcp.rag.adapter.web.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response DTO for document details.
 */
public record DocumentResponse(
    String id,
    String organizationId,
    String title,
    String status,
    String fileName,
    String fileType,
    Long fileSize,
    Map<String, String> metadata,
    Integer chunkCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}