package com.zamaz.mcp.controller.repository;

import com.zamaz.mcp.controller.entity.Debate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DebateRepository extends JpaRepository<Debate, UUID> {
    
    Page<Debate> findByOrganizationId(UUID organizationId, Pageable pageable);
    
    Page<Debate> findByStatus(String status, Pageable pageable);
    
    Page<Debate> findByOrganizationIdAndStatus(UUID organizationId, String status, Pageable pageable);
}