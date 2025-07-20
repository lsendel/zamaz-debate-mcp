package com.zamaz.mcp.rag.application.usecase;

import com.zamaz.mcp.rag.application.query.GetDocumentQuery;
import com.zamaz.mcp.rag.domain.model.Document;
import java.util.Optional;

/**
 * Use case for retrieving a document by ID.
 */
public interface GetDocumentUseCase {
    
    /**
     * Get a document by ID for an organization.
     * 
     * @param query the get document query
     * @return the document if found and belongs to organization
     */
    Optional<Document> execute(GetDocumentQuery query);
}