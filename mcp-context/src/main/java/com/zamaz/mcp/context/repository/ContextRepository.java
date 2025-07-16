package com.zamaz.mcp.context.repository;

import com.zamaz.mcp.context.entity.Context;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Context entity operations.
 * Provides multi-tenant aware queries for context management.
 */
@Repository
public interface ContextRepository extends JpaRepository<Context, UUID> {
    
    /**
     * Find a context by ID and organization ID (multi-tenant safety).
     */
    Optional<Context> findByIdAndOrganizationId(UUID id, UUID organizationId);
    
    /**
     * Find all contexts for an organization.
     */
    Page<Context> findByOrganizationIdAndStatus(UUID organizationId, Context.ContextStatus status, Pageable pageable);
    
    /**
     * Find contexts by user within an organization.
     */
    Page<Context> findByOrganizationIdAndUserIdAndStatus(UUID organizationId, UUID userId, Context.ContextStatus status, Pageable pageable);
    
    /**
     * Search contexts by name or description.
     */
    @Query("SELECT c FROM Context c WHERE c.organizationId = :orgId AND c.status = :status AND " +
           "(LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Context> searchContexts(@Param("orgId") UUID organizationId, 
                                @Param("status") Context.ContextStatus status,
                                @Param("searchTerm") String searchTerm, 
                                Pageable pageable);
    
    /**
     * Find recently accessed contexts.
     */
    @Query("SELECT c FROM Context c WHERE c.organizationId = :orgId AND c.status = :status " +
           "ORDER BY c.lastAccessedAt DESC")
    List<Context> findRecentContexts(@Param("orgId") UUID organizationId, 
                                    @Param("status") Context.ContextStatus status,
                                    Pageable pageable);
    
    /**
     * Update last accessed timestamp.
     */
    @Modifying
    @Query("UPDATE Context c SET c.lastAccessedAt = :timestamp WHERE c.id = :contextId")
    void updateLastAccessedAt(@Param("contextId") UUID contextId, @Param("timestamp") Instant timestamp);
    
    /**
     * Find contexts that need archiving (not accessed for specified days).
     */
    @Query("SELECT c FROM Context c WHERE c.status = 'ACTIVE' AND c.lastAccessedAt < :cutoffDate")
    List<Context> findContextsForArchiving(@Param("cutoffDate") Instant cutoffDate);
    
    /**
     * Count total tokens for an organization.
     */
    @Query("SELECT SUM(c.totalTokens) FROM Context c WHERE c.organizationId = :orgId AND c.status = 'ACTIVE'")
    Long countTotalTokensForOrganization(@Param("orgId") UUID organizationId);
    
    /**
     * Soft delete a context.
     */
    @Modifying
    @Query("UPDATE Context c SET c.status = 'DELETED', c.updatedAt = :timestamp WHERE c.id = :contextId")
    void softDelete(@Param("contextId") UUID contextId, @Param("timestamp") Instant timestamp);
}