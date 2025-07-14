package com.zamaz.mcp.controller.repository;

import com.zamaz.mcp.controller.entity.Response;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResponseRepository extends JpaRepository<Response, UUID> {
    
    List<Response> findByRoundId(UUID roundId);
    
    List<Response> findByParticipantId(UUID participantId);
    
    boolean existsByRoundIdAndParticipantId(UUID roundId, UUID participantId);
}