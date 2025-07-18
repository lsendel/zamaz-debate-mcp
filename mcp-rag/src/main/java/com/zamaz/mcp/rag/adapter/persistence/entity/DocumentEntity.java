package com.zamaz.mcp.rag.adapter.persistence.entity;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity for documents.
 */
@Entity
@Table(name = "documents", indexes = {
    @Index(name = "idx_document_org", columnList = "organization_id"),
    @Index(name = "idx_document_status", columnList = "status"),
    @Index(name = "idx_document_created", columnList = "created_at")
})
public class DocumentEntity {
    
    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;
    
    @Column(name = "organization_id", nullable = false, columnDefinition = "uuid")
    private UUID organizationId;
    
    @Column(name = "title", nullable = false)
    private String title;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private DocumentStatusEnum status;
    
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "content_type")
    private String contentType;
    
    @Column(name = "file_name")
    private String fileName;
    
    @Column(name = "file_type")
    private String fileType;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;
    
    @Column(name = "chunk_count")
    private Integer chunkCount = 0;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentChunkEntity> chunks = new ArrayList<>();
    
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
    public DocumentEntity() {}
    
    public DocumentEntity(UUID id, UUID organizationId, String title, DocumentStatusEnum status) {
        this.id = id;
        this.organizationId = organizationId;
        this.title = title;
        this.status = status;
    }
    
    // Getters and setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getOrganizationId() {
        return organizationId;
    }
    
    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public DocumentStatusEnum getStatus() {
        return status;
    }
    
    public void setStatus(DocumentStatusEnum status) {
        this.status = status;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getContentType() {
        return contentType;
    }
    
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getFileType() {
        return fileType;
    }
    
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getMetadata() {
        return metadata;
    }
    
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
    
    public Integer getChunkCount() {
        return chunkCount;
    }
    
    public void setChunkCount(Integer chunkCount) {
        this.chunkCount = chunkCount;
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
    
    public List<DocumentChunkEntity> getChunks() {
        return chunks;
    }
    
    public void setChunks(List<DocumentChunkEntity> chunks) {
        this.chunks = chunks;
    }
    
    /**
     * Status enum for JPA.
     */
    public enum DocumentStatusEnum {
        UPLOADED,
        PROCESSING,
        PROCESSED,
        FAILED,
        ARCHIVED
    }
}