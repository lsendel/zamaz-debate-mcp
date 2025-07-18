package com.zamaz.mcp.controller.repository;

import com.zamaz.mcp.controller.entity.Debate;
import com.zamaz.mcp.controller.dto.DebateWithParticipantCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface DebateRepository extends JpaRepository<Debate, UUID>, JpaSpecificationExecutor<Debate> {

    @Query("SELECT d.id as id, d.organizationId as organizationId, d.title as title, d.description as description, d.topic as topic, d.format as format, d.maxRounds as maxRounds, d.currentRound as currentRound, d.status as status, d.settings as settings, d.createdAt as createdAt, d.updatedAt as updatedAt, d.startedAt as startedAt, d.completedAt as completedAt, count(p) as participantCount FROM Debate d LEFT JOIN d.participants p GROUP BY d.id")
    Page<DebateWithParticipantCount> findAllWithParticipantCount(Pageable pageable);
    
    Page<Debate> findByOrganizationId(UUID organizationId, Pageable pageable);
    
    Page<Debate> findByStatus(String status, Pageable pageable);
    
    Page<Debate> findByOrganizationIdAndStatus(UUID organizationId, String status, Pageable pageable);
}