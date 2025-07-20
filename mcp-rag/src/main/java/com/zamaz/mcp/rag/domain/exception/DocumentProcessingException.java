package com.zamaz.mcp.rag.domain.exception;

import com.zamaz.mcp.rag.domain.model.document.DocumentId;

/**
 * Exception thrown when document processing fails.
 */
public class DocumentProcessingException extends DomainException {
    
    private final DocumentId documentId;
    
    public DocumentProcessingException(DocumentId documentId, String message) {
        super(message, "DOCUMENT_PROCESSING_ERROR");
        this.documentId = documentId;
    }
    
    public DocumentProcessingException(DocumentId documentId, String message, Throwable cause) {
        super(message, "DOCUMENT_PROCESSING_ERROR", cause);
        this.documentId = documentId;
    }
    
    public DocumentId getDocumentId() {
        return documentId;
    }
}