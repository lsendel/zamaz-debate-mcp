package com.zamaz.mcp.rag.application.query;

import com.zamaz.mcp.rag.domain.model.DocumentId;
import com.zamaz.mcp.rag.domain.model.OrganizationId;
import java.util.Objects;

/**
 * Query to retrieve a document by ID.
 */
public record GetDocumentQuery(
    DocumentId documentId,
    OrganizationId organizationId
) {
    
    public GetDocumentQuery {
        Objects.requireNonNull(documentId, "Document ID cannot be null");
        Objects.requireNonNull(organizationId, "Organization ID cannot be null");
    }
    
    public static GetDocumentQuery of(DocumentId documentId, OrganizationId organizationId) {
        return new GetDocumentQuery(documentId, organizationId);
    }
}