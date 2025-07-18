package com.zamaz.mcp.rag.domain.event;

import com.zamaz.mcp.common.domain.event.DomainEvent;
import com.zamaz.mcp.rag.domain.model.DocumentId;
import com.zamaz.mcp.rag.domain.model.OrganizationId;
import java.time.Instant;
import java.util.Objects;

/**
 * Domain event fired when document processing fails.
 */
public class DocumentProcessingFailedEvent implements DomainEvent {
    
    private final DocumentId documentId;
    private final OrganizationId organizationId;
    private final String errorMessage;
    private final Instant occurredAt;
    
    public DocumentProcessingFailedEvent(DocumentId documentId, OrganizationId organizationId, String errorMessage) {
        this.documentId = Objects.requireNonNull(documentId, "Document ID cannot be null");
        this.organizationId = Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        this.errorMessage = Objects.requireNonNull(errorMessage, "Error message cannot be null");
        this.occurredAt = Instant.now();
    }
    
    public DocumentId getDocumentId() {
        return documentId;
    }
    
    public OrganizationId getOrganizationId() {
        return organizationId;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    @Override
    public Instant getOccurredAt() {
        return occurredAt;
    }
    
    @Override
    public String getEventType() {
        return "DocumentProcessingFailed";
    }
    
    @Override
    public String getAggregateId() {
        return documentId.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DocumentProcessingFailedEvent that = (DocumentProcessingFailedEvent) obj;
        return Objects.equals(documentId, that.documentId) && 
               Objects.equals(occurredAt, that.occurredAt);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(documentId, occurredAt);
    }
    
    @Override
    public String toString() {
        return String.format("DocumentProcessingFailedEvent{documentId=%s, organizationId=%s, error='%s', occurredAt=%s}",
            documentId, organizationId, errorMessage, occurredAt);
    }
}