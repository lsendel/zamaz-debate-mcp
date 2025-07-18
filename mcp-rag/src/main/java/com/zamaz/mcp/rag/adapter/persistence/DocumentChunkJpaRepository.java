package com.zamaz.mcp.rag.adapter.persistence;

import com.zamaz.mcp.rag.adapter.persistence.entity.DocumentChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * JPA repository for document chunks.
 */
@Repository
public interface DocumentChunkJpaRepository extends JpaRepository<DocumentChunkEntity, UUID> {
    
    /**
     * Find all chunks for a document.
     */
    List<DocumentChunkEntity> findAllByDocumentIdOrderByChunkNumber(UUID documentId);
    
    /**
     * Delete all chunks for a document.
     */
    void deleteAllByDocumentId(UUID documentId);
    
    /**
     * Find chunks by embedding similarity using PostgreSQL pgvector.
     * Note: This is a simplified query - in production would use pgvector extension.
     */
    @Query(value = "SELECT c.*, " +
           "1 - (c.embedding <=> CAST(:queryEmbedding AS vector)) as similarity " +
           "FROM document_chunks c " +
           "JOIN documents d ON c.document_id = d.id " +
           "WHERE d.organization_id = :orgId " +
           "AND 1 - (c.embedding <=> CAST(:queryEmbedding AS vector)) >= :minSimilarity " +
           "ORDER BY similarity DESC " +
           "LIMIT :limit", 
           nativeQuery = true)
    List<Object[]> findSimilarChunks(
        @Param("orgId") UUID organizationId,
        @Param("queryEmbedding") String queryEmbedding,
        @Param("minSimilarity") double minSimilarity,
        @Param("limit") int limit
    );
    
    /**
     * Count chunks for a document.
     */
    long countByDocumentId(UUID documentId);
}