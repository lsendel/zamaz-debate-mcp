package com.zamaz.mcp.debateengine.adapter.persistence;

import com.zamaz.mcp.debateengine.adapter.persistence.entity.DebateEntity;
import com.zamaz.mcp.debateengine.adapter.persistence.repository.DebateJpaRepository;
import com.zamaz.mcp.debateengine.domain.model.*;
import com.zamaz.mcp.debateengine.domain.port.DebateRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adapter implementation of DebateRepository using JPA.
 */
@Repository
@Transactional
public class DebateRepositoryAdapter implements DebateRepository {
    
    private final DebateJpaRepository jpaRepository;
    private final DebateEntityMapper mapper;
    
    public DebateRepositoryAdapter(
            DebateJpaRepository jpaRepository,
            DebateEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    public Debate save(Debate debate) {
        DebateEntity entity = mapper.toEntity(debate);
        
        // Save participants if present
        if (!debate.getParticipants().isEmpty()) {
            entity.setParticipants(
                debate.getParticipants().stream()
                    .map(p -> mapper.toParticipantEntity(p, entity))
                    .collect(Collectors.toList())
            );
        }
        
        DebateEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }
    
    @Override
    public Optional<Debate> findById(DebateId id) {
        return jpaRepository.findById(UUID.fromString(id.toString()))
            .map(mapper::toDomain);
    }
    
    @Override
    public Optional<Debate> findByIdAndOrganization(DebateId id, OrganizationId organizationId) {
        return jpaRepository.findByIdAndOrganizationId(
                UUID.fromString(id.toString()),
                UUID.fromString(organizationId.toString())
            )
            .map(mapper::toDomain);
    }
    
    @Override
    public List<Debate> findByOrganization(OrganizationId organizationId) {
        return jpaRepository.findByOrganizationIdOrderByCreatedAtDesc(
                UUID.fromString(organizationId.toString())
            )
            .stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Debate> findByUser(UUID userId) {
        return jpaRepository.findByUserInvolvement(userId)
            .stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Debate> findByStatus(DebateStatus status) {
        DebateEntity.DebateStatusEnum statusEnum = switch (status) {
            case DRAFT -> DebateEntity.DebateStatusEnum.DRAFT;
            case ACTIVE -> DebateEntity.DebateStatusEnum.ACTIVE;
            case COMPLETED -> DebateEntity.DebateStatusEnum.COMPLETED;
            case CANCELLED -> DebateEntity.DebateStatusEnum.CANCELLED;
        };
        
        return jpaRepository.findByStatusOrderByCreatedAtDesc(statusEnum)
            .stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Debate> findActiveDebates() {
        List<DebateEntity.DebateStatusEnum> activeStatuses = List.of(
            DebateEntity.DebateStatusEnum.ACTIVE
        );
        
        return jpaRepository.findByStatusInOrderByStartedAtDesc(activeStatuses)
            .stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public boolean exists(DebateId id) {
        return jpaRepository.existsById(UUID.fromString(id.toString()));
    }
    
    @Override
    public void delete(DebateId id) {
        jpaRepository.deleteById(UUID.fromString(id.toString()));
    }
}