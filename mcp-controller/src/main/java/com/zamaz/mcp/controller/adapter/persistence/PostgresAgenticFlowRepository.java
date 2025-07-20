package com.zamaz.mcp.controller.adapter.persistence;

import com.zamaz.mcp.common.domain.agentic.AgenticFlow;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowId;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowRepository;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowStatus;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowType;
import com.zamaz.mcp.common.domain.organization.OrganizationId;
import com.zamaz.mcp.controller.adapter.persistence.entity.AgenticFlowEntity;
import com.zamaz.mcp.controller.adapter.persistence.mapper.AgenticFlowMapper;
import com.zamaz.mcp.controller.adapter.persistence.repository.SpringDataAgenticFlowRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * PostgreSQL implementation of the AgenticFlowRepository using Spring Data JPA.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PostgresAgenticFlowRepository implements AgenticFlowRepository {

    private final SpringDataAgenticFlowRepository springDataRepository;
    private final AgenticFlowMapper mapper;

    @Override
    public AgenticFlow save(AgenticFlow flow) {
        log.debug("Saving agentic flow: {}", flow.getId());

        UUID flowUuid = UUID.fromString(flow.getId().getValue());
        Optional<AgenticFlowEntity> existingEntity = springDataRepository.findById(flowUuid);

        AgenticFlowEntity entity;
        if (existingEntity.isPresent()) {
            // Update existing entity
            entity = existingEntity.get();
            mapper.updateEntity(entity, flow);
        } else {
            // Create new entity
            entity = mapper.toEntity(flow);
        }

        AgenticFlowEntity savedEntity = springDataRepository.save(entity);

        log.info("Saved agentic flow with ID: {}", flow.getId());
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<AgenticFlow> findById(AgenticFlowId id) {
        log.debug("Finding agentic flow by ID: {}", id);

        return springDataRepository.findById(UUID.fromString(id.getValue()))
                .map(mapper::toDomain);
    }

    @Override
    public List<AgenticFlow> findByOrganization(OrganizationId organizationId) {
        log.debug("Finding agentic flows for organization: {}", organizationId);

        return springDataRepository.findByOrganizationId(UUID.fromString(organizationId.getValue()))
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AgenticFlow> findByType(AgenticFlowType type) {
        log.debug("Finding agentic flows by type: {}", type);

        return springDataRepository.findByFlowType(type.name())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AgenticFlow> findByOrganizationAndType(OrganizationId organizationId, AgenticFlowType type) {
        log.debug("Finding agentic flows for organization {} and type {}", organizationId, type);

        return springDataRepository.findByOrganizationIdAndFlowType(
                UUID.fromString(organizationId.getValue()),
                type.name())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean delete(AgenticFlowId id) {
        log.debug("Deleting agentic flow: {}", id);

        UUID uuid = UUID.fromString(id.getValue());
        if (springDataRepository.existsById(uuid)) {
            springDataRepository.deleteById(uuid);
            log.info("Deleted agentic flow with ID: {}", id);
            return true;
        }

        log.warn("Agentic flow not found for deletion: {}", id);
        return false;
    }

    /**
     * Additional method to find flows by status
     */
    public List<AgenticFlow> findByOrganizationAndStatus(OrganizationId organizationId, AgenticFlowStatus status) {
        log.debug("Finding agentic flows for organization {} with status {}", organizationId, status);

        return springDataRepository.findByOrganizationIdAndStatus(
                UUID.fromString(organizationId.getValue()),
                status.name())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}