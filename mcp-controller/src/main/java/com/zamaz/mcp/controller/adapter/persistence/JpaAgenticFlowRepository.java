package com.zamaz.mcp.controller.adapter.persistence;

import com.zamaz.mcp.common.domain.agentic.AgenticFlow;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowId;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowRepository;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowType;
import com.zamaz.mcp.common.domain.organization.OrganizationId;
import com.zamaz.mcp.controller.adapter.persistence.entity.AgenticFlowEntity;
import com.zamaz.mcp.controller.adapter.persistence.mapper.AgenticFlowEntityMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA implementation of AgenticFlowRepository.
 */
@Repository
@Transactional
public class JpaAgenticFlowRepository implements AgenticFlowRepository {

    private final SpringDataAgenticFlowRepository springRepository;
    private final AgenticFlowEntityMapper mapper;

    public JpaAgenticFlowRepository(SpringDataAgenticFlowRepository springRepository, 
                                   AgenticFlowEntityMapper mapper) {
        this.springRepository = Objects.requireNonNull(springRepository);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public AgenticFlow save(AgenticFlow flow) {
        Objects.requireNonNull(flow, "AgenticFlow cannot be null");
        
        AgenticFlowEntity entity = mapper.toEntity(flow);
        AgenticFlowEntity saved = springRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<AgenticFlow> findById(AgenticFlowId id) {
        Objects.requireNonNull(id, "AgenticFlow ID cannot be null");
        
        return springRepository.findById(UUID.fromString(id.getValue()))
                .map(mapper::toDomain);
    }

    @Override
    public List<AgenticFlow> findByOrganization(OrganizationId organizationId) {
        Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        
        return springRepository.findByOrganizationId(UUID.fromString(organizationId.getValue()))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<AgenticFlow> findByType(AgenticFlowType type) {
        Objects.requireNonNull(type, "AgenticFlow type cannot be null");
        
        return springRepository.findByFlowType(type.name())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<AgenticFlow> findByOrganizationAndType(OrganizationId organizationId, AgenticFlowType type) {
        Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        Objects.requireNonNull(type, "AgenticFlow type cannot be null");
        
        return springRepository.findByOrganizationIdAndFlowType(
                        UUID.fromString(organizationId.getValue()), 
                        type.name())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean delete(AgenticFlowId id) {
        Objects.requireNonNull(id, "AgenticFlow ID cannot be null");
        
        UUID uuid = UUID.fromString(id.getValue());
        if (springRepository.existsById(uuid)) {
            springRepository.deleteById(uuid);
            return true;
        }
        return false;
    }
}