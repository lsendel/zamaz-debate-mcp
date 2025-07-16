package com.zamaz.mcp.context.repository;

import com.zamaz.mcp.context.entity.SharedContext;
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
 * Repository interface for SharedContext entity operations.
 */
@Repository
public interface SharedContextRepository extends JpaRepository<SharedContext, UUID> {
    
    /**
     * Find shared context by ID ensuring it belongs to the target organization or user.
     */
    @Query("SELECT sc FROM SharedContext sc WHERE sc.id = :id AND sc.isActive = true AND " +
           "(sc.targetOrganizationId = :orgId OR sc.targetUserId = :userId)")
    Optional<SharedContext> findByIdAndTarget(@Param("id") UUID id, 
                                             @Param("orgId") UUID organizationId,
                                             @Param("userId") UUID userId);
    
    /**
     * Find all contexts shared with an organization.
     */
    List<SharedContext> findByTargetOrganizationIdAndIsActiveTrue(UUID targetOrganizationId);
    
    /**
     * Find all contexts shared with a user.
     */
    List<SharedContext> findByTargetUserIdAndIsActiveTrue(UUID targetUserId);
    
    /**
     * Find all contexts shared by an organization.
     */
    List<SharedContext> findBySourceOrganizationIdAndIsActiveTrue(UUID sourceOrganizationId);
    
    /**
     * Check if a context is already shared with a target.
     */
    @Query("SELECT COUNT(sc) > 0 FROM SharedContext sc WHERE sc.context.id = :contextId AND " +
           "sc.isActive = true AND " +
           "(sc.targetOrganizationId = :targetOrgId OR sc.targetUserId = :targetUserId)")
    boolean isContextSharedWithTarget(@Param("contextId") UUID contextId,
                                     @Param("targetOrgId") UUID targetOrganizationId,
                                     @Param("targetUserId") UUID targetUserId);
    
    /**
     * Deactivate expired shares.
     */
    @Modifying
    @Query("UPDATE SharedContext sc SET sc.isActive = false WHERE sc.isActive = true AND " +
           "sc.expiresAt IS NOT NULL AND sc.expiresAt < :now")
    int deactivateExpiredShares(@Param("now") Instant now);
    
    /**
     * Revoke a share.
     */
    @Modifying
    @Query("UPDATE SharedContext sc SET sc.isActive = false WHERE sc.id = :shareId")
    void revokeShare(@Param("shareId") UUID shareId);
}