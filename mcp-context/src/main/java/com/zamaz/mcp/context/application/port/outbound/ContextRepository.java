package com.zamaz.mcp.context.application.port.outbound;

import com.zamaz.mcp.common.application.port.outbound.Repository;
import com.zamaz.mcp.common.domain.model.OrganizationId;
import com.zamaz.mcp.common.domain.model.UserId;
import com.zamaz.mcp.context.domain.model.Context;
import com.zamaz.mcp.context.domain.model.ContextId;
import com.zamaz.mcp.context.domain.model.ContextStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Repository interface for Context aggregate persistence.
 * This is an outbound port in hexagonal architecture.
 */
public interface ContextRepository extends Repository<Context, ContextId> {
    
    /**
     * Find all contexts for a specific organization.
     */
    Page<Context> findByOrganizationId(OrganizationId organizationId, Pageable pageable);
    
    /**
     * Find all contexts for a specific user within an organization.
     */
    Page<Context> findByOrganizationIdAndUserId(
        OrganizationId organizationId, 
        UserId userId, 
        Pageable pageable
    );
    
    /**
     * Find contexts by organization and status.
     */
    List<Context> findByOrganizationIdAndStatus(
        OrganizationId organizationId, 
        ContextStatus status
    );
    
    /**
     * Find contexts that haven't been updated since the given date.
     */
    List<Context> findInactiveContexts(Instant inactiveSince);
    
    /**
     * Search contexts by name within an organization.
     */
    Page<Context> searchByName(
        OrganizationId organizationId,
        String namePattern,
        Pageable pageable
    );
    
    /**
     * Check if a context exists for an organization.
     */
    boolean existsByIdAndOrganizationId(ContextId contextId, OrganizationId organizationId);
    
    /**
     * Count contexts by organization and status.
     */
    long countByOrganizationIdAndStatus(OrganizationId organizationId, ContextStatus status);
    
    /**
     * Delete all contexts for an organization (for cleanup/testing).
     */
    void deleteByOrganizationId(OrganizationId organizationId);
}