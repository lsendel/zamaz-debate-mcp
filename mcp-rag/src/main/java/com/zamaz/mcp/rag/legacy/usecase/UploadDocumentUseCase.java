package com.zamaz.mcp.rag.application.usecase;

import com.zamaz.mcp.rag.application.command.UploadDocumentCommand;
import com.zamaz.mcp.rag.domain.model.DocumentId;

/**
 * Use case for uploading a new document.
 */
public interface UploadDocumentUseCase {
    
    /**
     * Upload a new document to the RAG system.
     * 
     * @param command the upload document command
     * @return the ID of the created document
     */
    DocumentId execute(UploadDocumentCommand command);
}