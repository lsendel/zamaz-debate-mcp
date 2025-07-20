package com.zamaz.mcp.rag.repository;

import com.zamaz.mcp.rag.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for DocumentChunk entities.
 */
@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, String> {
    
    List<DocumentChunk> findByDocumentId(String documentId);
    
    List<DocumentChunk> findByOrganizationIdAndVectorIdIn(String organizationId, List<String> vectorIds);
    
    @Query("SELECT dc FROM DocumentChunk dc WHERE dc.document.id = :documentId ORDER BY dc.chunkIndex")
    List<DocumentChunk> findChunksByDocumentId(@Param("documentId") String documentId);
    
    void deleteByDocumentId(String documentId);
    
    long countByDocumentId(String documentId);
}