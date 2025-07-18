package com.zamaz.mcp.debateengine.adapter.persistence.repository;

import com.zamaz.mcp.debateengine.adapter.persistence.entity.DebateEntity;
import com.zamaz.mcp.debateengine.adapter.persistence.entity.DebateEntity.DebateStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for debates.
 */
@Repository
public interface DebateJpaRepository extends JpaRepository<DebateEntity, UUID> {
    
    /**
     * Find debate by ID and organization.
     */
    Optional<DebateEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
    
    /**
     * Find debates by organization.
     */
    List<DebateEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    
    /**
     * Find debates by user.
     */
    @Query("SELECT d FROM DebateEntity d LEFT JOIN d.participants p " +
           "WHERE d.createdByUserId = :userId OR p.userId = :userId " +
           "ORDER BY d.createdAt DESC")
    List<DebateEntity> findByUserInvolvement(@Param("userId") UUID userId);
    
    /**
     * Find debates by status.
     */
    List<DebateEntity> findByStatusOrderByCreatedAtDesc(DebateStatusEnum status);
    
    /**
     * Find active debates.
     */
    List<DebateEntity> findByStatusInOrderByStartedAtDesc(List<DebateStatusEnum> statuses);
}