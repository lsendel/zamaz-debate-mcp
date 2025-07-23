package com.zamaz.mcp.common.patterns;

import com.zamaz.mcp.common.audit.Auditable;
import com.zamaz.mcp.common.audit.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Base repository interface providing common query methods for all entities.
 * Extends JpaRepository with additional standardized methods and audit logging.
 * 
 * This interface provides:
 * - Common CRUD operations with audit logging
 * - Organization-scoped queries
 * - Soft delete functionality
 * - Search capabilities
 * - Audit trail support
 */
@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID> {

       /**
        * Find entities by organization ID.
        * This method should be implemented by repositories that have
        * organization-scoped entities.
        */
       List<T> findByOrganizationId(String organizationId);

       /**
        * Find entities by organization ID with pagination.
        */
       Page<T> findByOrganizationId(String organizationId, Pageable pageable);

       /**
        * Find entities created after a specific date.
        */
       @Query("SELECT e FROM #{#entityName} e WHERE e.createdAt > :date")
       List<T> findCreatedAfter(@Param("date") LocalDateTime date);

       /**
        * Find entities created between two dates.
        */
       @Query("SELECT e FROM #{#entityName} e WHERE e.createdAt BETWEEN :startDate AND :endDate")
       List<T> findCreatedBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

       /**
        * Find entities updated after a specific date.
        */
       @Query("SELECT e FROM #{#entityName} e WHERE e.updatedAt > :date")
       List<T> findUpdatedAfter(@Param("date") LocalDateTime date);

       /**
        * Find active entities (not deleted).
        * This assumes entities have a 'deleted' or 'active' field.
        */
       @Query("SELECT e FROM #{#entityName} e WHERE e.deleted = false")
       List<T> findActive();

       /**
        * Find active entities with pagination.
        */
       @Query("SELECT e FROM #{#entityName} e WHERE e.deleted = false")
       Page<T> findActive(Pageable pageable);

       /**
        * Count active entities.
        */
       @Query("SELECT COUNT(e) FROM #{#entityName} e WHERE e.deleted = false")
       long countActive();

       /**
        * Count entities by organization.
        */
       long countByOrganizationId(String organizationId);

       /**
        * Find entities by multiple IDs.
        */
       @Query("SELECT e FROM #{#entityName} e WHERE e.id IN :ids")
       List<T> findByIds(@Param("ids") List<ID> ids);

       /**
        * Soft delete an entity by ID.
        * This assumes entities have a 'deleted' field.
        */
       @Modifying
       @Auditable(action = AuditEvent.AuditAction.DELETE, resourceType = "Entity", description = "Soft delete entity by ID", riskLevel = AuditEvent.RiskLevel.MEDIUM)
       @Query("UPDATE #{#entityName} e SET e.deleted = true, e.deletedAt = CURRENT_TIMESTAMP WHERE e.id = :id")
       void softDeleteById(@Param("id") ID id);

       /**
        * Restore a soft-deleted entity by ID.
        */
       @Modifying
       @Auditable(action = AuditEvent.AuditAction.UPDATE, resourceType = "Entity", description = "Restore soft-deleted entity by ID", riskLevel = AuditEvent.RiskLevel.MEDIUM)
       @Query("UPDATE #{#entityName} e SET e.deleted = false, e.deletedAt = null WHERE e.id = :id")
       void restoreById(@Param("id") ID id);

       /**
        * Find entities by a text search.
        * This is a generic method that can be overridden by specific repositories.
        */
       @Query("SELECT e FROM #{#entityName} e WHERE " +
                     "LOWER(CAST(e.name AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
                     "LOWER(CAST(e.description AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
       List<T> findBySearchTerm(@Param("searchTerm") String searchTerm);

       /**
        * Find entities by search term with pagination.
        */
       @Query("SELECT e FROM #{#entityName} e WHERE " +
                     "LOWER(CAST(e.name AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
                     "LOWER(CAST(e.description AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
       Page<T> findBySearchTerm(@Param("searchTerm") String searchTerm, Pageable pageable);

       /**
        * Find entities by organization and search term.
        */
       @Query("SELECT e FROM #{#entityName} e WHERE e.organizationId = :organizationId AND " +
                     "(LOWER(CAST(e.name AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
                     "LOWER(CAST(e.description AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
       List<T> findByOrganizationIdAndSearchTerm(@Param("organizationId") String organizationId,
                     @Param("searchTerm") String searchTerm);

       /**
        * Find entities by organization and search term with pagination.
        */
       @Query("SELECT e FROM #{#entityName} e WHERE e.organizationId = :organizationId AND " +
                     "(LOWER(CAST(e.name AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
                     "LOWER(CAST(e.description AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
       Page<T> findByOrganizationIdAndSearchTerm(@Param("organizationId") String organizationId,
                     @Param("searchTerm") String searchTerm,
                     Pageable pageable);

       /**
        * Find the most recently created entity.
        */
       @Query("SELECT e FROM #{#entityName} e ORDER BY e.createdAt DESC LIMIT 1")
       Optional<T> findMostRecentlyCreated();

       /**
        * Find the most recently updated entity.
        */
       @Query("SELECT e FROM #{#entityName} e ORDER BY e.updatedAt DESC LIMIT 1")
       Optional<T> findMostRecentlyUpdated();

       /**
        * Check if an entity exists by organization ID.
        */
       boolean existsByOrganizationId(String organizationId);

       /**
        * Check if an entity exists by organization ID and name.
        */
       boolean existsByOrganizationIdAndName(String organizationId, String name);

       /**
        * Find entities ordered by creation date.
        */
       @Query("SELECT e FROM #{#entityName} e ORDER BY e.createdAt DESC")
       List<T> findAllOrderByCreatedAtDesc();

       /**
        * Find entities ordered by creation date with pagination.
        */
       @Query("SELECT e FROM #{#entityName} e ORDER BY e.createdAt DESC")
       Page<T> findAllOrderByCreatedAtDesc(Pageable pageable);

       /**
        * Find entities ordered by update date.
        */
       @Query("SELECT e FROM #{#entityName} e ORDER BY e.updatedAt DESC")
       List<T> findAllOrderByUpdatedAtDesc();

       /**
        * Find entities ordered by update date with pagination.
        */
       @Query("SELECT e FROM #{#entityName} e ORDER BY e.updatedAt DESC")
       Page<T> findAllOrderByUpdatedAtDesc(Pageable pageable);
}