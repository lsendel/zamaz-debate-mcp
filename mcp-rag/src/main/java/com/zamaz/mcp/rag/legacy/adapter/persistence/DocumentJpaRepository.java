package com.zamaz.mcp.rag.adapter.persistence;

import com.zamaz.mcp.rag.adapter.persistence.entity.DocumentEntity;
import com.zamaz.mcp.rag.adapter.persistence.entity.DocumentEntity.DocumentStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for documents.
 */
@Repository
public interface DocumentJpaRepository extends JpaRepository<DocumentEntity, UUID> {
    
    /**
     * Find document by ID and organization.
     */
    Optional<DocumentEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
    
    /**
     * Find documents by organization with filters.
     */
    @Query("SELECT d FROM DocumentEntity d WHERE d.organizationId = :orgId " +
           "AND (:statuses IS NULL OR d.status IN :statuses) " +
           "AND (:title IS NULL OR LOWER(d.title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
           "ORDER BY d.createdAt DESC")
    Page<DocumentEntity> findByFilters(
        @Param("orgId") UUID organizationId,
        @Param("statuses") List<DocumentStatusEnum> statuses,
        @Param("title") String title,
        Pageable pageable
    );
    
    /**
     * Find all documents by organization.
     */
    List<DocumentEntity> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    
    /**
     * Find documents by status.
     */
    List<DocumentEntity> findAllByStatus(DocumentStatusEnum status);
    
    /**
     * Check if document exists for organization.
     */
    boolean existsByIdAndOrganizationId(UUID id, UUID organizationId);
    
    /**
     * Count documents by organization and status.
     */
    long countByOrganizationIdAndStatus(UUID organizationId, DocumentStatusEnum status);
}