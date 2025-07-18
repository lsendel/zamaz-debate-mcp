package com.zamaz.mcp.context.adapter.persistence.repository;

import com.zamaz.mcp.context.adapter.persistence.entity.ContextEntity;
import com.zamaz.mcp.context.adapter.persistence.entity.ContextStatusEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for ContextEntity.
 */
@Repository
public interface SpringDataContextRepository extends JpaRepository<ContextEntity, String> {
    
    /**
     * Find all contexts for a specific organization.
     */
    Page<ContextEntity> findByOrganizationId(String organizationId, Pageable pageable);
    
    /**
     * Find all contexts for a specific user within an organization.
     */
    Page<ContextEntity> findByOrganizationIdAndUserId(
        String organizationId, 
        String userId, 
        Pageable pageable
    );
    
    /**
     * Find contexts by organization and status.
     */
    List<ContextEntity> findByOrganizationIdAndStatus(
        String organizationId, 
        ContextStatusEntity status
    );
    
    /**
     * Find contexts that haven't been updated since the given date.
     */
    @Query("SELECT c FROM ContextEntity c WHERE c.updatedAt < :inactiveSince AND c.status = 'ACTIVE'")
    List<ContextEntity> findInactiveContexts(@Param("inactiveSince") Instant inactiveSince);
    
    /**
     * Search contexts by name within an organization.
     */
    @Query("SELECT c FROM ContextEntity c WHERE c.organizationId = :organizationId " +
           "AND LOWER(c.name) LIKE LOWER(CONCAT('%', :namePattern, '%'))")
    Page<ContextEntity> searchByName(
        @Param("organizationId") String organizationId,
        @Param("namePattern") String namePattern,
        Pageable pageable
    );
    
    /**
     * Check if a context exists for an organization.
     */
    boolean existsByIdAndOrganizationId(String id, String organizationId);
    
    /**
     * Count contexts by organization and status.
     */
    long countByOrganizationIdAndStatus(String organizationId, ContextStatusEntity status);
    
    /**
     * Delete all contexts for an organization.
     */
    void deleteByOrganizationId(String organizationId);
    
    /**
     * Find a context by ID with messages eagerly loaded.
     */
    @Query("SELECT c FROM ContextEntity c LEFT JOIN FETCH c.messages WHERE c.id = :id")
    Optional<ContextEntity> findByIdWithMessages(@Param("id") String id);
}