package com.zamaz.mcp.rag.adapter.persistence.entity;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity for document chunks.
 */
@Entity
@Table(name = "document_chunks", indexes = {
    @Index(name = "idx_chunk_document", columnList = "document_id"),
    @Index(name = "idx_chunk_number", columnList = "chunk_number")
})
public class DocumentChunkEntity {
    
    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private DocumentEntity document;
    
    @Column(name = "chunk_number", nullable = false)
    private Integer chunkNumber;
    
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Column(name = "start_offset")
    private Integer startOffset;
    
    @Column(name = "end_offset")
    private Integer endOffset;
    
    @Column(name = "embedding", columnDefinition = "vector(1536)")
    private String embedding; // Will be stored as PostgreSQL vector type
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Constructors
    public DocumentChunkEntity() {}
    
    public DocumentChunkEntity(UUID id, DocumentEntity document, Integer chunkNumber, String content) {
        this.id = id;
        this.document = document;
        this.chunkNumber = chunkNumber;
        this.content = content;
    }
    
    // Getters and setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public DocumentEntity getDocument() {
        return document;
    }
    
    public void setDocument(DocumentEntity document) {
        this.document = document;
    }
    
    public Integer getChunkNumber() {
        return chunkNumber;
    }
    
    public void setChunkNumber(Integer chunkNumber) {
        this.chunkNumber = chunkNumber;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public Integer getStartOffset() {
        return startOffset;
    }
    
    public void setStartOffset(Integer startOffset) {
        this.startOffset = startOffset;
    }
    
    public Integer getEndOffset() {
        return endOffset;
    }
    
    public void setEndOffset(Integer endOffset) {
        this.endOffset = endOffset;
    }
    
    public String getEmbedding() {
        return embedding;
    }
    
    public void setEmbedding(String embedding) {
        this.embedding = embedding;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}