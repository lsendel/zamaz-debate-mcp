package com.zamaz.mcp.rag.repository;

import com.zamaz.mcp.rag.entity.Document;
import com.zamaz.mcp.rag.entity.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Document entities.
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, String> {
    
    Optional<Document> findByOrganizationIdAndDocumentId(String organizationId, String documentId);
    
    Page<Document> findByOrganizationId(String organizationId, Pageable pageable);
    
    Page<Document> findByOrganizationIdAndStatus(String organizationId, DocumentStatus status, Pageable pageable);
    
    List<Document> findByStatusAndProcessedAtIsNull(DocumentStatus status);
    
    @Query("SELECT d FROM Document d WHERE d.organizationId = :orgId AND " +
           "(LOWER(d.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.content) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Document> searchDocuments(@Param("orgId") String organizationId, 
                                  @Param("query") String query, 
                                  Pageable pageable);
    
    long countByOrganizationId(String organizationId);
    
    long countByOrganizationIdAndStatus(String organizationId, DocumentStatus status);
    
    void deleteByOrganizationIdAndDocumentId(String organizationId, String documentId);
}