package com.zamaz.mcp.debateengine.domain.port;

import com.zamaz.mcp.debateengine.domain.model.Debate;
import com.zamaz.mcp.debateengine.domain.model.DebateId;
import com.zamaz.mcp.debateengine.domain.model.DebateStatus;
import com.zamaz.mcp.debateengine.domain.model.OrganizationId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository port for Debate aggregate.
 */
public interface DebateRepository {
    
    /**
     * Save a debate.
     */
    Debate save(Debate debate);
    
    /**
     * Find debate by ID.
     */
    Optional<Debate> findById(DebateId id);
    
    /**
     * Find debate by ID and organization.
     */
    Optional<Debate> findByIdAndOrganization(DebateId id, OrganizationId organizationId);
    
    /**
     * Find debates by organization.
     */
    List<Debate> findByOrganization(OrganizationId organizationId);
    
    /**
     * Find debates by user.
     */
    List<Debate> findByUser(UUID userId);
    
    /**
     * Find debates by status.
     */
    List<Debate> findByStatus(DebateStatus status);
    
    /**
     * Find active debates.
     */
    List<Debate> findActiveDebates();
    
    /**
     * Check if debate exists.
     */
    boolean exists(DebateId id);
    
    /**
     * Delete a debate.
     */
    void delete(DebateId id);
}