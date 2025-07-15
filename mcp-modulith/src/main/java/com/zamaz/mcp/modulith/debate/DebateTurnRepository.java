package com.zamaz.mcp.modulith.debate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for DebateTurn entities.
 */
@Repository
public interface DebateTurnRepository extends JpaRepository<DebateTurn, UUID> {
    
    List<DebateTurn> findByDebateOrderByCreatedAtAsc(Debate debate);
    
    int countByDebate(Debate debate);
    
    int countByDebateAndRoundNumber(Debate debate, Integer roundNumber);
}