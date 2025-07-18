package com.zamaz.mcp.rag.domain.event;

import com.zamaz.mcp.common.domain.event.DomainEvent;
import com.zamaz.mcp.rag.domain.model.DocumentId;
import com.zamaz.mcp.rag.domain.model.DocumentStatus;
import com.zamaz.mcp.rag.domain.model.OrganizationId;
import java.time.Instant;
import java.util.Objects;

/**
 * Domain event fired when a document status changes.
 */
public class DocumentStatusChangedEvent implements DomainEvent {
    
    private final DocumentId documentId;
    private final OrganizationId organizationId;
    private final DocumentStatus oldStatus;
    private final DocumentStatus newStatus;
    private final Instant occurredAt;
    
    public DocumentStatusChangedEvent(DocumentId documentId, OrganizationId organizationId,
                                    DocumentStatus oldStatus, DocumentStatus newStatus) {
        this.documentId = Objects.requireNonNull(documentId, "Document ID cannot be null");
        this.organizationId = Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        this.oldStatus = Objects.requireNonNull(oldStatus, "Old status cannot be null");
        this.newStatus = Objects.requireNonNull(newStatus, "New status cannot be null");
        this.occurredAt = Instant.now();
    }
    
    public DocumentId getDocumentId() {
        return documentId;
    }
    
    public OrganizationId getOrganizationId() {
        return organizationId;
    }
    
    public DocumentStatus getOldStatus() {
        return oldStatus;
    }
    
    public DocumentStatus getNewStatus() {
        return newStatus;
    }
    
    @Override
    public Instant getOccurredAt() {
        return occurredAt;
    }
    
    @Override
    public String getEventType() {
        return "DocumentStatusChanged";
    }
    
    @Override
    public String getAggregateId() {
        return documentId.toString();
    }
    
    public boolean isTransitionToProcessing() {
        return newStatus == DocumentStatus.PROCESSING;
    }
    
    public boolean isTransitionToCompleted() {
        return newStatus == DocumentStatus.COMPLETED;
    }
    
    public boolean isTransitionToFailed() {
        return newStatus == DocumentStatus.FAILED;
    }
    
    public boolean isTransitionToArchived() {
        return newStatus == DocumentStatus.ARCHIVED;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DocumentStatusChangedEvent that = (DocumentStatusChangedEvent) obj;
        return Objects.equals(documentId, that.documentId) && 
               Objects.equals(occurredAt, that.occurredAt);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(documentId, occurredAt);
    }
    
    @Override
    public String toString() {
        return String.format("DocumentStatusChangedEvent{documentId=%s, organizationId=%s, %s -> %s, occurredAt=%s}",
            documentId, organizationId, oldStatus, newStatus, occurredAt);
    }
}