package com.zamaz.mcp.rag.application.usecase;

import com.zamaz.mcp.rag.application.command.ProcessDocumentCommand;

/**
 * Use case for processing a document (chunking and embedding generation).
 */
public interface ProcessDocumentUseCase {
    
    /**
     * Process a document by chunking it and generating embeddings.
     * 
     * @param command the process document command
     */
    void execute(ProcessDocumentCommand command);
}