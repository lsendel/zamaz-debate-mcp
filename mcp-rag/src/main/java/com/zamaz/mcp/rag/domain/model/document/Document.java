package com.zamaz.mcp.rag.domain.model.document;

import com.zamaz.mcp.rag.domain.event.DocumentCreatedEvent;
import com.zamaz.mcp.rag.domain.event.DocumentProcessedEvent;
import com.zamaz.mcp.rag.domain.event.DocumentProcessingFailedEvent;
import com.zamaz.mcp.rag.domain.event.DocumentStatusChangedEvent;
import com.zamaz.mcp.rag.domain.model.embedding.EmbeddingVector;
import com.zamaz.mcp.rag.domain.service.ChunkingStrategy;

import java.time.Instant;
import java.util.*;

/**
 * Document Aggregate Root - Core domain entity representing a document in the RAG system.
 * This is a pure domain object with no framework dependencies.
 */
public class Document {
    
    private final DocumentId id;
    private final DocumentName name;
    private final DocumentContent content;
    private final DocumentMetadata metadata;
    private final OrganizationId organizationId;
    private DocumentStatus status;
    private EmbeddingStatus embeddingStatus;
    private final List<DocumentChunk> chunks;
    private ProcessingHistory processingHistory;
    private final Instant createdAt;
    private Instant updatedAt;
    private final List<Object> domainEvents;
    
    // Private constructor enforces creation through factory methods
    private Document(
            DocumentId id,
            DocumentName name,
            DocumentContent content,
            DocumentMetadata metadata,
            OrganizationId organizationId) {
        this.id = Objects.requireNonNull(id, "Document ID cannot be null");
        this.name = Objects.requireNonNull(name, "Document name cannot be null");
        this.content = Objects.requireNonNull(content, "Document content cannot be null");
        this.metadata = Objects.requireNonNull(metadata, "Document metadata cannot be null");
        this.organizationId = Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        this.status = DocumentStatus.UPLOADED;
        this.embeddingStatus = EmbeddingStatus.PENDING;
        this.chunks = new ArrayList<>();
        this.processingHistory = ProcessingHistory.empty();
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.domainEvents = new ArrayList<>();
    }
    
    // Factory method for creating new documents
    public static Document create(
            DocumentName name,
            DocumentContent content,
            DocumentMetadata metadata,
            OrganizationId organizationId) {
        DocumentId id = DocumentId.generate();
        Document document = new Document(id, name, content, metadata, organizationId);
        // Note: DocumentCreatedEvent expects title and FileInfo, but we have DocumentName
        // This will need to be resolved in the event refactoring
        document.addDomainEvent(new DocumentCreatedEvent(
            id, 
            organizationId, 
            name.value(), 
            metadata.getFileInfo()
        ));
        return document;
    }
    
    // Factory method for reconstitution from persistence
    public static Document reconstitute(
            DocumentId id,
            DocumentName name,
            DocumentContent content,
            DocumentMetadata metadata,
            OrganizationId organizationId,
            DocumentStatus status,
            EmbeddingStatus embeddingStatus,
            List<DocumentChunk> chunks,
            ProcessingHistory processingHistory,
            Instant createdAt,
            Instant updatedAt) {
        Document document = new Document(id, name, content, metadata, organizationId);
        document.status = status;
        document.embeddingStatus = embeddingStatus;
        document.chunks.addAll(chunks);
        document.processingHistory = processingHistory;
        document.updatedAt = updatedAt;
        // Note: createdAt is final, so we can't set it here - would need reflection or different approach
        return document;
    }
    
    // Business method - Process document into chunks
    public ProcessingResult processIntoChunks(ChunkingStrategy strategy) {
        if (!canBeProcessed()) {
            throw new IllegalStateException("Document cannot be processed in current state: " + status);
        }
        
        try {
            // Update status
            this.status = DocumentStatus.PROCESSING;
            this.updatedAt = Instant.now();
            
            // Apply chunking strategy
            List<DocumentChunk> newChunks = strategy.chunk(this);
            
            // Validate chunks
            if (newChunks.isEmpty()) {
                throw new ProcessingException("No chunks generated from document");
            }
            
            // Update state
            this.chunks.clear();
            this.chunks.addAll(newChunks);
            this.status = DocumentStatus.CHUNKED;
            
            // Record in history
            ProcessingStep step = ProcessingStep.chunking(newChunks.size(), Instant.now());
            this.processingHistory = this.processingHistory.addStep(step);
            
            // Create result
            ProcessingResult result = ProcessingResult.success(newChunks.size());
            
            // Raise event
            addDomainEvent(new DocumentStatusChangedEvent(id, organizationId, DocumentStatus.PROCESSING, DocumentStatus.CHUNKED));
            
            return result;
            
        } catch (Exception e) {
            this.status = DocumentStatus.FAILED;
            this.processingHistory = this.processingHistory.addError(e.getMessage(), Instant.now());
            addDomainEvent(new DocumentProcessingFailedEvent(id, organizationId, e.getMessage()));
            throw new ProcessingException("Failed to process document: " + e.getMessage(), e);
        }
    }
    
    // Business method - Update embeddings for chunks
    public void updateChunkEmbeddings(Map<ChunkId, EmbeddingVector> embeddings) {
        if (status != DocumentStatus.CHUNKED && status != DocumentStatus.EMBEDDED) {
            throw new IllegalStateException("Cannot update embeddings for document in state: " + status);
        }
        
        int updatedCount = 0;
        for (DocumentChunk chunk : chunks) {
            EmbeddingVector embedding = embeddings.get(chunk.getId());
            if (embedding != null) {
                chunk.updateEmbedding(embedding);
                updatedCount++;
            }
        }
        
        if (updatedCount == chunks.size()) {
            this.status = DocumentStatus.EMBEDDED;
            this.embeddingStatus = EmbeddingStatus.COMPLETE;
            addDomainEvent(new DocumentProcessedEvent(id, organizationId, chunks.size()));
        } else {
            this.embeddingStatus = EmbeddingStatus.PARTIAL;
        }
        
        this.updatedAt = Instant.now();
    }
    
    // Business method - Check if document is ready for search
    public boolean isReadyForSearch() {
        return status == DocumentStatus.EMBEDDED && 
               embeddingStatus == EmbeddingStatus.COMPLETE &&
               !chunks.isEmpty() &&
               chunks.stream().allMatch(DocumentChunk::hasEmbedding);
    }
    
    // Business method - Archive document
    public void archive() {
        if (status == DocumentStatus.ARCHIVED) {
            return; // Already archived
        }
        
        this.status = DocumentStatus.ARCHIVED;
        this.updatedAt = Instant.now();
        addDomainEvent(new DocumentStatusChangedEvent(id, organizationId, status, DocumentStatus.ARCHIVED));
    }
    
    // Business method - Get searchable chunks
    public List<DocumentChunk> getSearchableChunks() {
        if (!isReadyForSearch()) {
            return Collections.emptyList();
        }
        
        return chunks.stream()
                .filter(DocumentChunk::hasEmbedding)
                .toList();
    }
    
    // Business rules
    private boolean canBeProcessed() {
        return status == DocumentStatus.UPLOADED || status == DocumentStatus.FAILED;
    }
    
    // Domain event handling
    private void addDomainEvent(Object event) {
        domainEvents.add(event);
    }
    
    public List<Object> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }
    
    public void clearDomainEvents() {
        domainEvents.clear();
    }
    
    // Getters (no setters - immutability except for specific state changes)
    public DocumentId getId() {
        return id;
    }
    
    public DocumentName getName() {
        return name;
    }
    
    public DocumentContent getContent() {
        return content;
    }
    
    public DocumentMetadata getMetadata() {
        return metadata;
    }
    
    public OrganizationId getOrganizationId() {
        return organizationId;
    }
    
    public DocumentStatus getStatus() {
        return status;
    }
    
    public EmbeddingStatus getEmbeddingStatus() {
        return embeddingStatus;
    }
    
    public List<DocumentChunk> getChunks() {
        return Collections.unmodifiableList(chunks);
    }
    
    public ProcessingHistory getProcessingHistory() {
        return processingHistory;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    // Value object for processing results
    public record ProcessingResult(boolean success, int chunksGenerated, String errorMessage) {
        public static ProcessingResult success(int chunksGenerated) {
            return new ProcessingResult(true, chunksGenerated, null);
        }
        
        public static ProcessingResult failure(String errorMessage) {
            return new ProcessingResult(false, 0, errorMessage);
        }
    }
    
    // Domain exception
    public static class ProcessingException extends RuntimeException {
        public ProcessingException(String message) {
            super(message);
        }
        
        public ProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}