package com.zamaz.mcp.rag.application.command;

import com.zamaz.mcp.rag.domain.model.DocumentId;
import java.util.Objects;

/**
 * Command to process a document (chunk and generate embeddings).
 */
public record ProcessDocumentCommand(DocumentId documentId) {
    
    public ProcessDocumentCommand {
        Objects.requireNonNull(documentId, "Document ID cannot be null");
    }
    
    public static ProcessDocumentCommand of(DocumentId documentId) {
        return new ProcessDocumentCommand(documentId);
    }
}