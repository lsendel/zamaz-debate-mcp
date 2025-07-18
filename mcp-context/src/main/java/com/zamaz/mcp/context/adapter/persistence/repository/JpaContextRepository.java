package com.zamaz.mcp.context.adapter.persistence.repository;

import com.zamaz.mcp.common.architecture.adapter.persistence.PersistenceAdapter;
import com.zamaz.mcp.common.domain.model.OrganizationId;
import com.zamaz.mcp.common.domain.model.UserId;
import com.zamaz.mcp.context.adapter.persistence.mapper.ContextPersistenceMapper;
import com.zamaz.mcp.context.application.port.outbound.ContextRepository;
import com.zamaz.mcp.context.domain.model.Context;
import com.zamaz.mcp.context.domain.model.ContextId;
import com.zamaz.mcp.context.domain.model.ContextStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA implementation of ContextRepository.
 * This is a persistence adapter in hexagonal architecture.
 */
@Component
@Transactional(readOnly = true)
public class JpaContextRepository implements ContextRepository, PersistenceAdapter {
    
    private final SpringDataContextRepository jpaRepository;
    private final ContextPersistenceMapper mapper;
    
    public JpaContextRepository(
            SpringDataContextRepository jpaRepository,
            ContextPersistenceMapper mapper
    ) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    @Transactional
    public Context save(Context context) {
        var entity = mapper.toEntity(context);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }
    
    @Override
    public Optional<Context> findById(ContextId id) {
        return jpaRepository.findByIdWithMessages(id.asString())
            .map(mapper::toDomain);
    }
    
    @Override
    @Transactional
    public void deleteById(ContextId id) {
        jpaRepository.deleteById(id.asString());
    }
    
    @Override
    public boolean existsById(ContextId id) {
        return jpaRepository.existsById(id.asString());
    }
    
    @Override
    public Page<Context> findByOrganizationId(OrganizationId organizationId, Pageable pageable) {
        return jpaRepository.findByOrganizationId(organizationId.value(), pageable)
            .map(mapper::toDomain);
    }
    
    @Override
    public Page<Context> findByOrganizationIdAndUserId(
            OrganizationId organizationId, 
            UserId userId, 
            Pageable pageable
    ) {
        return jpaRepository.findByOrganizationIdAndUserId(
                organizationId.value(), 
                userId.value(), 
                pageable
            )
            .map(mapper::toDomain);
    }
    
    @Override
    public List<Context> findByOrganizationIdAndStatus(
            OrganizationId organizationId, 
            ContextStatus status
    ) {
        var entityStatus = mapper.toEntityStatus(status);
        return jpaRepository.findByOrganizationIdAndStatus(organizationId.value(), entityStatus)
            .stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Context> findInactiveContexts(Instant inactiveSince) {
        return jpaRepository.findInactiveContexts(inactiveSince)
            .stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public Page<Context> searchByName(
            OrganizationId organizationId,
            String namePattern,
            Pageable pageable
    ) {
        return jpaRepository.searchByName(organizationId.value(), namePattern, pageable)
            .map(mapper::toDomain);
    }
    
    @Override
    public boolean existsByIdAndOrganizationId(ContextId contextId, OrganizationId organizationId) {
        return jpaRepository.existsByIdAndOrganizationId(
            contextId.asString(), 
            organizationId.value()
        );
    }
    
    @Override
    public long countByOrganizationIdAndStatus(OrganizationId organizationId, ContextStatus status) {
        var entityStatus = mapper.toEntityStatus(status);
        return jpaRepository.countByOrganizationIdAndStatus(organizationId.value(), entityStatus);
    }
    
    @Override
    @Transactional
    public void deleteByOrganizationId(OrganizationId organizationId) {
        jpaRepository.deleteByOrganizationId(organizationId.value());
    }
}