package com.zamaz.mcp.rag.domain.event;

import com.zamaz.mcp.common.domain.event.DomainEvent;
import com.zamaz.mcp.rag.domain.model.DocumentId;
import com.zamaz.mcp.rag.domain.model.OrganizationId;
import java.time.Instant;
import java.util.Objects;

/**
 * Domain event fired when a document has been successfully processed.
 */
public class DocumentProcessedEvent implements DomainEvent {
    
    private final DocumentId documentId;
    private final OrganizationId organizationId;
    private final int chunkCount;
    private final Instant occurredAt;
    
    public DocumentProcessedEvent(DocumentId documentId, OrganizationId organizationId, int chunkCount) {
        this.documentId = Objects.requireNonNull(documentId, "Document ID cannot be null");
        this.organizationId = Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        this.chunkCount = chunkCount;
        this.occurredAt = Instant.now();
    }
    
    public DocumentId getDocumentId() {
        return documentId;
    }
    
    public OrganizationId getOrganizationId() {
        return organizationId;
    }
    
    public int getChunkCount() {
        return chunkCount;
    }
    
    @Override
    public Instant getOccurredAt() {
        return occurredAt;
    }
    
    @Override
    public String getEventType() {
        return "DocumentProcessed";
    }
    
    @Override
    public String getAggregateId() {
        return documentId.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DocumentProcessedEvent that = (DocumentProcessedEvent) obj;
        return Objects.equals(documentId, that.documentId) && 
               Objects.equals(occurredAt, that.occurredAt);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(documentId, occurredAt);
    }
    
    @Override
    public String toString() {
        return String.format("DocumentProcessedEvent{documentId=%s, organizationId=%s, chunkCount=%d, occurredAt=%s}",
            documentId, organizationId, chunkCount, occurredAt);
    }
}