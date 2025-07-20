package com.zamaz.mcp.rag.application.command;

import com.zamaz.mcp.rag.domain.model.DocumentId;
import com.zamaz.mcp.rag.domain.model.OrganizationId;
import java.util.Objects;

/**
 * Command to archive a document.
 */
public record ArchiveDocumentCommand(
    DocumentId documentId,
    OrganizationId organizationId
) {
    
    public ArchiveDocumentCommand {
        Objects.requireNonNull(documentId, "Document ID cannot be null");
        Objects.requireNonNull(organizationId, "Organization ID cannot be null");
    }
    
    public static ArchiveDocumentCommand of(DocumentId documentId, OrganizationId organizationId) {
        return new ArchiveDocumentCommand(documentId, organizationId);
    }
}