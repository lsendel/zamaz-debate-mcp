package com.zamaz.mcp.context.repository;

import com.zamaz.mcp.context.entity.ContextVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for ContextVersion entity operations.
 */
@Repository
public interface ContextVersionRepository extends JpaRepository<ContextVersion, UUID> {
    
    /**
     * Find the latest version for a context.
     */
    Optional<ContextVersion> findTopByContextIdOrderByVersionDesc(UUID contextId);
    
    /**
     * Find all versions for a context.
     */
    List<ContextVersion> findByContextIdOrderByVersionDesc(UUID contextId);
    
    /**
     * Find a specific version of a context.
     */
    Optional<ContextVersion> findByContextIdAndVersion(UUID contextId, Integer version);
    
    /**
     * Get the next version number for a context.
     */
    @Query("SELECT COALESCE(MAX(cv.version), 0) + 1 FROM ContextVersion cv WHERE cv.context.id = :contextId")
    Integer getNextVersionNumber(@Param("contextId") UUID contextId);
    
    /**
     * Find versions created within a time range.
     */
    List<ContextVersion> findByContextIdAndCreatedAtBetween(UUID contextId, Instant startTime, Instant endTime);
    
    /**
     * Delete old versions based on retention policy.
     */
    void deleteByContextIdAndCreatedAtBefore(UUID contextId, Instant cutoffTime);
    
    /**
     * Count versions for a context.
     */
    long countByContextId(UUID contextId);
}