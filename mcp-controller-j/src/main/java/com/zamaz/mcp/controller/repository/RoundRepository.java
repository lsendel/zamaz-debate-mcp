package com.zamaz.mcp.controller.repository;

import com.zamaz.mcp.controller.entity.Round;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoundRepository extends JpaRepository<Round, UUID> {
    
    List<Round> findByDebateIdOrderByRoundNumber(UUID debateId);
    
    Optional<Round> findByDebateIdAndRoundNumber(UUID debateId, Integer roundNumber);
}