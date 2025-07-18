package com.zamaz.mcp.debateengine.adapter.persistence;

import com.zamaz.mcp.debateengine.adapter.persistence.entity.ContextEntity;
import com.zamaz.mcp.debateengine.adapter.persistence.repository.ContextJpaRepository;
import com.zamaz.mcp.debateengine.domain.model.*;
import com.zamaz.mcp.debateengine.domain.port.ContextRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adapter implementation of ContextRepository using JPA.
 */
@Repository
@Transactional
public class ContextRepositoryAdapter implements ContextRepository {
    
    private final ContextJpaRepository jpaRepository;
    private final DebateEntityMapper mapper;
    
    public ContextRepositoryAdapter(
            ContextJpaRepository jpaRepository,
            DebateEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    public Context save(Context context) {
        ContextEntity entity = mapper.toContextEntity(context);
        ContextEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomainContext(savedEntity);
    }
    
    @Override
    public Optional<Context> findById(ContextId id) {
        return jpaRepository.findById(UUID.fromString(id.toString()))
            .map(mapper::toDomainContext);
    }
    
    @Override
    public Optional<Context> findByDebateId(DebateId debateId) {
        return jpaRepository.findByDebateId(UUID.fromString(debateId.toString()))
            .map(mapper::toDomainContext);
    }
    
    @Override
    public List<Context> findByOrganization(OrganizationId organizationId) {
        return jpaRepository.findByOrganizationIdOrderByLastActivityAtDesc(
                UUID.fromString(organizationId.toString())
            )
            .stream()
            .map(mapper::toDomainContext)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Context> findActiveByOrganization(OrganizationId organizationId) {
        return jpaRepository.findByOrganizationIdAndStatusOrderByLastActivityAtDesc(
                UUID.fromString(organizationId.toString()),
                ContextEntity.ContextStatusEnum.ACTIVE
            )
            .stream()
            .map(mapper::toDomainContext)
            .collect(Collectors.toList());
    }
    
    @Override
    public void delete(ContextId id) {
        jpaRepository.deleteById(UUID.fromString(id.toString()));
    }
    
    @Override
    public boolean exists(ContextId id) {
        return jpaRepository.existsById(UUID.fromString(id.toString()));
    }
}