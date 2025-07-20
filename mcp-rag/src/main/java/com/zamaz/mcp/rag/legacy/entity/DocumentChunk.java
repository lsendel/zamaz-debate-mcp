package com.zamaz.mcp.rag.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity representing a chunk of a document with its embedding.
 */
@Entity
@Table(name = "document_chunks", indexes = {
        @Index(name = "idx_doc_chunk", columnList = "document_id, chunk_index"),
        @Index(name = "idx_org_chunk", columnList = "organization_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class DocumentChunk {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(name = "organization_id", nullable = false)
    private String organizationId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;
    
    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;
    
    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Column(name = "start_position")
    private Integer startPosition;
    
    @Column(name = "end_position")
    private Integer endPosition;
    
    @Column(name = "token_count")
    private Integer tokenCount;
    
    // Store embedding vector ID from Qdrant
    @Column(name = "vector_id")
    private String vectorId;
    
    @ElementCollection
    @CollectionTable(name = "chunk_embedding", 
                    joinColumns = @JoinColumn(name = "chunk_id"))
    @Column(name = "value")
    private List<Float> embedding;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}