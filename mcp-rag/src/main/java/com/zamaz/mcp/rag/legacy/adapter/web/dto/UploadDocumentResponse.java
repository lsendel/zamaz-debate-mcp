package com.zamaz.mcp.rag.adapter.web.dto;

/**
 * Response DTO for document upload operations.
 */
public record UploadDocumentResponse(
    String documentId,
    String message,
    String status
) {
}