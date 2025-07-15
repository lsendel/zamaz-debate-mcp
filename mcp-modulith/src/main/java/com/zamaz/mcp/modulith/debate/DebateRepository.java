package com.zamaz.mcp.modulith.debate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Debate entities.
 */
@Repository
public interface DebateRepository extends JpaRepository<Debate, UUID> {
    
    List<Debate> findByOrganizationId(UUID organizationId);
    
    List<Debate> findByStatus(Debate.DebateStatus status);
}