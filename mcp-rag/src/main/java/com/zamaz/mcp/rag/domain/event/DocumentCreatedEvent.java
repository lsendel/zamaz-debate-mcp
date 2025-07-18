package com.zamaz.mcp.rag.domain.event;

import com.zamaz.mcp.common.domain.event.DomainEvent;
import com.zamaz.mcp.rag.domain.model.DocumentId;
import com.zamaz.mcp.rag.domain.model.FileInfo;
import com.zamaz.mcp.rag.domain.model.OrganizationId;
import java.time.Instant;
import java.util.Objects;

/**
 * Domain event fired when a document is created.
 */
public class DocumentCreatedEvent implements DomainEvent {
    
    private final DocumentId documentId;
    private final OrganizationId organizationId;
    private final String title;
    private final FileInfo fileInfo;
    private final Instant occurredAt;
    
    public DocumentCreatedEvent(DocumentId documentId, OrganizationId organizationId, 
                              String title, FileInfo fileInfo) {
        this.documentId = Objects.requireNonNull(documentId, "Document ID cannot be null");
        this.organizationId = Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        this.title = Objects.requireNonNull(title, "Title cannot be null");
        this.fileInfo = Objects.requireNonNull(fileInfo, "File info cannot be null");
        this.occurredAt = Instant.now();
    }
    
    public DocumentId getDocumentId() {
        return documentId;
    }
    
    public OrganizationId getOrganizationId() {
        return organizationId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public FileInfo getFileInfo() {
        return fileInfo;
    }
    
    @Override
    public Instant getOccurredAt() {
        return occurredAt;
    }
    
    @Override
    public String getEventType() {
        return "DocumentCreated";
    }
    
    @Override
    public String getAggregateId() {
        return documentId.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DocumentCreatedEvent that = (DocumentCreatedEvent) obj;
        return Objects.equals(documentId, that.documentId) && 
               Objects.equals(occurredAt, that.occurredAt);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(documentId, occurredAt);
    }
    
    @Override
    public String toString() {
        return String.format("DocumentCreatedEvent{documentId=%s, title='%s', fileType='%s', organizationId=%s, occurredAt=%s}",
            documentId, title, fileInfo.fileType(), organizationId, occurredAt);
    }
}