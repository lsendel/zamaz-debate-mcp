package com.zamaz.mcp.debateengine.adapter.persistence.repository;

import com.zamaz.mcp.debateengine.adapter.persistence.entity.ContextEntity;
import com.zamaz.mcp.debateengine.adapter.persistence.entity.ContextEntity.ContextStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for contexts.
 */
@Repository
public interface ContextJpaRepository extends JpaRepository<ContextEntity, UUID> {
    
    /**
     * Find context by debate ID.
     */
    Optional<ContextEntity> findByDebateId(UUID debateId);
    
    /**
     * Find contexts by organization.
     */
    List<ContextEntity> findByOrganizationIdOrderByLastActivityAtDesc(UUID organizationId);
    
    /**
     * Find active contexts by organization.
     */
    List<ContextEntity> findByOrganizationIdAndStatusOrderByLastActivityAtDesc(
        UUID organizationId, 
        ContextStatusEnum status
    );
}