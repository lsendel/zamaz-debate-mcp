package com.zamaz.mcp.debateengine.domain.port;

import com.zamaz.mcp.debateengine.domain.model.Context;
import com.zamaz.mcp.debateengine.domain.model.ContextId;
import com.zamaz.mcp.debateengine.domain.model.DebateId;
import com.zamaz.mcp.debateengine.domain.model.OrganizationId;

import java.util.List;
import java.util.Optional;

/**
 * Repository port for Context entity.
 */
public interface ContextRepository {
    
    /**
     * Save a context.
     */
    Context save(Context context);
    
    /**
     * Find context by ID.
     */
    Optional<Context> findById(ContextId id);
    
    /**
     * Find context by debate ID.
     */
    Optional<Context> findByDebateId(DebateId debateId);
    
    /**
     * Find contexts by organization.
     */
    List<Context> findByOrganization(OrganizationId organizationId);
    
    /**
     * Find active contexts by organization.
     */
    List<Context> findActiveByOrganization(OrganizationId organizationId);
    
    /**
     * Delete a context.
     */
    void delete(ContextId id);
    
    /**
     * Check if context exists.
     */
    boolean exists(ContextId id);
}