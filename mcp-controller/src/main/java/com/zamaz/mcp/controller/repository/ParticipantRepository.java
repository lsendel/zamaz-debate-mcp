package com.zamaz.mcp.controller.repository;

import com.zamaz.mcp.controller.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, UUID> {
    
    List<Participant> findByDebateId(UUID debateId);
    
    List<Participant> findByDebateIdAndType(UUID debateId, String type);
}