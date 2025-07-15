package com.zamaz.mcp.modulith.debate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for DebateParticipant entities.
 */
@Repository
public interface DebateParticipantRepository extends JpaRepository<DebateParticipant, UUID> {
}