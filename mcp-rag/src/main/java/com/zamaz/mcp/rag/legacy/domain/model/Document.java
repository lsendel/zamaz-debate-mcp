package com.zamaz.mcp.rag.domain.model;

import com.zamaz.mcp.common.domain.model.AggregateRoot;
import com.zamaz.mcp.rag.domain.event.DocumentCreatedEvent;
import com.zamaz.mcp.rag.domain.event.DocumentProcessedEvent;
import com.zamaz.mcp.rag.domain.event.DocumentProcessingFailedEvent;
import com.zamaz.mcp.rag.domain.event.DocumentStatusChangedEvent;

import java.time.Instant;
import java.util.*;

/**
 * Aggregate root representing a document in the RAG system.
 */
public class Document extends AggregateRoot<DocumentId> {
    
    private final DocumentId id;
    private final OrganizationId organizationId;
    private final String title;
    private final FileInfo fileInfo;
    private final DocumentContent content;
    private final DocumentMetadata metadata;
    private final Instant createdAt;
    private final List<DocumentChunk> chunks;
    private DocumentStatus status;
    private Instant processedAt;
    private String errorMessage;
    
    private Document(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "Document ID cannot be null");
        this.organizationId = Objects.requireNonNull(builder.organizationId, "Organization ID cannot be null");
        this.title = Objects.requireNonNull(builder.title, "Title cannot be null");
        this.fileInfo = Objects.requireNonNull(builder.fileInfo, "File info cannot be null");
        this.content = Objects.requireNonNull(builder.content, "Content cannot be null");
        this.metadata = builder.metadata != null ? builder.metadata : DocumentMetadata.empty();
        this.createdAt = Objects.requireNonNull(builder.createdAt, "Created timestamp cannot be null");
        this.chunks = new ArrayList<>(builder.chunks);
        this.status = Objects.requireNonNull(builder.status, "Status cannot be null");
        this.processedAt = builder.processedAt;
        this.errorMessage = builder.errorMessage;
        
        validateInvariants();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static Document create(DocumentId id, OrganizationId organizationId, String title,
                                FileInfo fileInfo, DocumentContent content, DocumentMetadata metadata) {
        Document document = builder()
            .id(id)
            .organizationId(organizationId)
            .title(title.trim())
            .fileInfo(fileInfo)
            .content(content)
            .metadata(metadata)
            .createdAt(Instant.now())
            .status(DocumentStatus.PENDING)
            .build();
        
        document.addDomainEvent(new DocumentCreatedEvent(id, organizationId, title, fileInfo));
        return document;
    }
    
    @Override
    public DocumentId getId() {
        return id;
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
    
    public DocumentContent getContent() {
        return content;
    }
    
    public DocumentMetadata getMetadata() {
        return metadata;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public List<DocumentChunk> getChunks() {
        return Collections.unmodifiableList(chunks);
    }
    
    public DocumentStatus getStatus() {
        return status;
    }
    
    public Optional<Instant> getProcessedAt() {
        return Optional.ofNullable(processedAt);
    }
    
    public Optional<String> getErrorMessage() {
        return Optional.ofNullable(errorMessage);
    }
    
    public void startProcessing() {
        if (!status.canTransitionTo(DocumentStatus.PROCESSING)) {
            throw new IllegalStateException("Cannot start processing from status: " + status);
        }
        
        changeStatus(DocumentStatus.PROCESSING);
        this.errorMessage = null;
    }
    
    public void completeProcessing(List<DocumentChunk> processedChunks) {
        Objects.requireNonNull(processedChunks, "Processed chunks cannot be null");
        
        if (!status.canTransitionTo(DocumentStatus.COMPLETED)) {
            throw new IllegalStateException("Cannot complete processing from status: " + status);
        }
        
        if (processedChunks.isEmpty()) {
            throw new IllegalArgumentException("Cannot complete processing with no chunks");
        }
        
        // Validate all chunks belong to this document
        for (DocumentChunk chunk : processedChunks) {
            if (!chunk.getDocumentId().equals(this.id)) {
                throw new IllegalArgumentException("Chunk does not belong to this document: " + chunk.getId());
            }
        }
        
        this.chunks.clear();
        this.chunks.addAll(processedChunks);
        this.processedAt = Instant.now();
        this.errorMessage = null;
        
        changeStatus(DocumentStatus.COMPLETED);
        addDomainEvent(new DocumentProcessedEvent(id, organizationId, chunks.size()));
    }
    
    public void failProcessing(String errorMessage) {
        Objects.requireNonNull(errorMessage, "Error message cannot be null");
        
        if (!status.canTransitionTo(DocumentStatus.FAILED)) {
            throw new IllegalStateException("Cannot fail processing from status: " + status);
        }
        
        this.errorMessage = errorMessage.trim();
        changeStatus(DocumentStatus.FAILED);
        addDomainEvent(new DocumentProcessingFailedEvent(id, organizationId, errorMessage));
    }
    
    public void retryProcessing() {
        if (status != DocumentStatus.FAILED) {
            throw new IllegalStateException("Can only retry failed documents");
        }
        
        this.chunks.clear();
        this.processedAt = null;
        this.errorMessage = null;
        changeStatus(DocumentStatus.PENDING);
    }
    
    public void archive() {
        if (!status.canTransitionTo(DocumentStatus.ARCHIVED)) {
            throw new IllegalStateException("Cannot archive document from status: " + status);
        }
        
        changeStatus(DocumentStatus.ARCHIVED);
    }
    
    public void addChunk(DocumentChunk chunk) {
        Objects.requireNonNull(chunk, "Chunk cannot be null");
        
        if (!chunk.getDocumentId().equals(this.id)) {
            throw new IllegalArgumentException("Chunk does not belong to this document");
        }
        
        if (status != DocumentStatus.PROCESSING) {
            throw new IllegalStateException("Can only add chunks during processing");
        }
        
        chunks.add(chunk);
    }
    
    public List<DocumentChunk> searchChunks(SearchQuery query) {
        Objects.requireNonNull(query, "Search query cannot be null");
        
        if (!isSearchable()) {
            return List.of();
        }
        
        return chunks.stream()
            .filter(chunk -> chunk.containsText(query.text()))
            .filter(chunk -> chunk.getRelevanceScore() >= query.minSimilarity())
            .sorted((c1, c2) -> Double.compare(c2.getRelevanceScore(), c1.getRelevanceScore()))
            .limit(query.maxResults())
            .toList();
    }
    
    public int getChunkCount() {
        return chunks.size();
    }
    
    public boolean hasChunks() {
        return !chunks.isEmpty();
    }
    
    public boolean isSearchable() {
        return status.isSearchable() && hasChunks();
    }
    
    public boolean canBeProcessed() {
        return status.isProcessable();
    }
    
    public boolean isProcessingFailed() {
        return status.hasError();
    }
    
    public boolean belongsToOrganization(OrganizationId orgId) {
        Objects.requireNonNull(orgId, "Organization ID cannot be null");
        return this.organizationId.equals(orgId);
    }
    
    public long getFileSizeBytes() {
        return fileInfo.fileSize();
    }
    
    public String getFileExtension() {
        return fileInfo.getFileExtension();
    }
    
    public int getTotalWordCount() {
        return content.wordCount();
    }
    
    public Optional<DocumentChunk> getChunkByIndex(int index) {
        return chunks.stream()
            .filter(chunk -> chunk.getChunkIndex() == index)
            .findFirst();
    }
    
    public List<DocumentChunk> getChunksWithEmbeddings() {
        return chunks.stream()
            .filter(DocumentChunk::hasEmbedding)
            .toList();
    }
    
    public double getAverageChunkRelevance() {
        if (chunks.isEmpty()) {
            return 0.0;
        }
        
        return chunks.stream()
            .mapToDouble(DocumentChunk::getRelevanceScore)
            .average()
            .orElse(0.0);
    }
    
    private void changeStatus(DocumentStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                "Invalid status transition from " + status + " to " + newStatus
            );
        }
        
        DocumentStatus oldStatus = this.status;
        this.status = newStatus;
        addDomainEvent(new DocumentStatusChangedEvent(id, organizationId, oldStatus, newStatus));
    }
    
    private void validateInvariants() {
        if (title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }
        
        if (title.length() > 500) {
            throw new IllegalArgumentException("Title cannot exceed 500 characters");
        }
        
        if (status.hasError() && (errorMessage == null || errorMessage.trim().isEmpty())) {
            throw new IllegalArgumentException("Failed documents must have an error message");
        }
        
        if (!status.hasError() && errorMessage != null) {
            throw new IllegalArgumentException("Non-failed documents cannot have an error message");
        }
        
        if ((status == DocumentStatus.COMPLETED || status == DocumentStatus.ARCHIVED) && processedAt == null) {
            throw new IllegalArgumentException("Processed documents must have processing timestamp");
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Document document = (Document) obj;
        return Objects.equals(id, document.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("Document{id=%s, title='%s', status=%s, chunks=%d, org=%s}",
            id, title.length() > 30 ? title.substring(0, 27) + "..." : title, 
            status, chunks.size(), organizationId);
    }
    
    public static class Builder {
        private DocumentId id;
        private OrganizationId organizationId;
        private String title;
        private FileInfo fileInfo;
        private DocumentContent content;
        private DocumentMetadata metadata;
        private Instant createdAt;
        private List<DocumentChunk> chunks = new ArrayList<>();
        private DocumentStatus status = DocumentStatus.PENDING;
        private Instant processedAt;
        private String errorMessage;
        
        public Builder id(DocumentId id) {
            this.id = id;
            return this;
        }
        
        public Builder organizationId(OrganizationId organizationId) {
            this.organizationId = organizationId;
            return this;
        }
        
        public Builder title(String title) {
            this.title = title;
            return this;
        }
        
        public Builder fileInfo(FileInfo fileInfo) {
            this.fileInfo = fileInfo;
            return this;
        }
        
        public Builder content(DocumentContent content) {
            this.content = content;
            return this;
        }
        
        public Builder metadata(DocumentMetadata metadata) {
            this.metadata = metadata;
            return this;
        }
        
        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public Builder chunks(List<DocumentChunk> chunks) {
            this.chunks = new ArrayList<>(chunks);
            return this;
        }
        
        public Builder status(DocumentStatus status) {
            this.status = status;
            return this;
        }
        
        public Builder processedAt(Instant processedAt) {
            this.processedAt = processedAt;
            return this;
        }
        
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }
        
        public Document build() {
            return new Document(this);
        }
    }
}