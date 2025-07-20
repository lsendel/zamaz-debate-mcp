package com.zamaz.mcp.rag.domain.exception;

import com.zamaz.mcp.rag.domain.model.document.DocumentId;

/**
 * Exception thrown when a document cannot be found.
 */
public class DocumentNotFoundException extends DomainException {
    
    private final DocumentId documentId;
    
    public DocumentNotFoundException(DocumentId documentId) {
        super("Document not found: " + documentId, "DOCUMENT_NOT_FOUND");
        this.documentId = documentId;
    }
    
    public DocumentId getDocumentId() {
        return documentId;
    }
}