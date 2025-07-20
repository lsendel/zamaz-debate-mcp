package com.zamaz.mcp.rag.application.port.in;

import com.zamaz.mcp.rag.domain.model.document.DocumentId;
import com.zamaz.mcp.rag.domain.model.document.OrganizationId;

/**
 * Inbound port for creating documents in the RAG system.
 * This interface defines the contract for document creation use case.
 */
public interface CreateDocumentUseCase {
    
    /**
     * Create a new document in the system
     * 
     * @param command The command containing document creation details
     * @return The ID of the created document
     */
    DocumentId createDocument(CreateDocumentCommand command);
    
    /**
     * Command object for document creation
     */
    record CreateDocumentCommand(
        String fileName,
        byte[] content,
        String contentType,
        OrganizationId organizationId,
        String source
    ) {
        public CreateDocumentCommand {
            if (fileName == null || fileName.trim().isEmpty()) {
                throw new IllegalArgumentException("File name cannot be empty");
            }
            if (content == null || content.length == 0) {
                throw new IllegalArgumentException("Content cannot be empty");
            }
            if (contentType == null || contentType.trim().isEmpty()) {
                throw new IllegalArgumentException("Content type cannot be empty");
            }
            if (organizationId == null) {
                throw new IllegalArgumentException("Organization ID cannot be null");
            }
            if (source == null) {
                source = "manual_upload";
            }
        }
        
        /**
         * Get file size in bytes
         */
        public long getFileSize() {
            return content.length;
        }
    }
}