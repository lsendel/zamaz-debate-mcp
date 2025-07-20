package com.zamaz.mcp.rag.application.command;

import com.zamaz.mcp.rag.domain.model.DocumentMetadata;
import com.zamaz.mcp.rag.domain.model.OrganizationId;
import java.util.Objects;

/**
 * Command to upload a new document to the RAG system.
 */
public record UploadDocumentCommand(
    OrganizationId organizationId,
    String title,
    String fileName,
    String fileType,
    long fileSize,
    byte[] content,
    DocumentMetadata metadata
) {
    
    public UploadDocumentCommand {
        Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        Objects.requireNonNull(title, "Title cannot be null");
        Objects.requireNonNull(fileName, "File name cannot be null");
        Objects.requireNonNull(fileType, "File type cannot be null");
        Objects.requireNonNull(content, "Content cannot be null");
        
        if (title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }
        
        if (fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("File name cannot be empty");  
        }
        
        if (fileType.trim().isEmpty()) {
            throw new IllegalArgumentException("File type cannot be empty");
        }
        
        if (fileSize <= 0) {
            throw new IllegalArgumentException("File size must be positive");
        }
        
        if (content.length == 0) {
            throw new IllegalArgumentException("Content cannot be empty");
        }
        
        // Default to empty metadata if null
        this.metadata = metadata != null ? metadata : DocumentMetadata.empty();
    }
    
    public static UploadDocumentCommand of(OrganizationId organizationId, String title, 
                                          String fileName, String fileType, long fileSize, 
                                          byte[] content) {
        return new UploadDocumentCommand(
            organizationId, 
            title.trim(), 
            fileName.trim(), 
            fileType.trim(), 
            fileSize, 
            content, 
            DocumentMetadata.empty()
        );
    }
    
    public static UploadDocumentCommand withMetadata(OrganizationId organizationId, String title,
                                                    String fileName, String fileType, long fileSize,
                                                    byte[] content, DocumentMetadata metadata) {
        return new UploadDocumentCommand(
            organizationId,
            title.trim(),
            fileName.trim(), 
            fileType.trim(),
            fileSize,
            content,
            metadata
        );
    }
    
    public String getContentAsString() {
        return new String(content, java.nio.charset.StandardCharsets.UTF_8);
    }
    
    public boolean isLargeFile() {
        return fileSize > 10 * 1024 * 1024; // 10MB
    }
}