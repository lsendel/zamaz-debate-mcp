package com.zamaz.mcp.controller.adapter.persistence;

import com.zamaz.mcp.common.domain.agentic.AgenticFlow;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowConfiguration;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowId;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowRepository;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowStatus;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowType;
import com.zamaz.mcp.common.domain.organization.OrganizationId;
import com.zamaz.mcp.controller.adapter.persistence.entity.AgenticFlowEntity;
import com.zamaz.mcp.controller.adapter.persistence.repository.SpringDataAgenticFlowRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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

    @Override
    public void save(AgenticFlow flow) {
        log.debug("Saving agentic flow: {}", flow.getId());
        
        AgenticFlowEntity entity = toEntity(flow);
        springDataRepository.save(entity);
        
        log.info("Saved agentic flow with ID: {}", flow.getId());
    }

    @Override
    public Optional<AgenticFlow> findById(AgenticFlowId id) {
        log.debug("Finding agentic flow by ID: {}", id);
        
        return springDataRepository.findById(UUID.fromString(id.getValue()))
                .map(this::toDomain);
    }

    @Override
    public List<AgenticFlow> findByOrganization(OrganizationId organizationId) {
        log.debug("Finding agentic flows for organization: {}", organizationId);
        
        return springDataRepository.findByOrganizationId(UUID.fromString(organizationId.getValue()))
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AgenticFlow> findByType(AgenticFlowType type) {
        log.debug("Finding agentic flows by type: {}", type);
        
        return springDataRepository.findByFlowType(type.name())
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AgenticFlow> findByOrganizationAndType(OrganizationId organizationId, AgenticFlowType type) {
        log.debug("Finding agentic flows for organization {} and type {}", organizationId, type);
        
        return springDataRepository.findByOrganizationIdAndFlowType(
                        UUID.fromString(organizationId.getValue()),
                        type.name())
                .stream()
                .map(this::toDomain)
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
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * Converts domain object to entity
     */
    private AgenticFlowEntity toEntity(AgenticFlow flow) {
        return AgenticFlowEntity.builder()
                .id(UUID.fromString(flow.getId()))
                .flowType(flow.getFlowType().name())
                .name(flow.getName())
                .description(flow.getDescription())
                .configuration(flow.getConfiguration().getParameters())
                .organizationId(UUID.fromString(flow.getOrganizationId()))
                .status(flow.getStatus().name())
                .createdAt(flow.getCreatedAt())
                .updatedAt(flow.getUpdatedAt())
                .version(flow.getVersion())
                .build();
    }

    /**
     * Converts entity to domain object
     */
    private AgenticFlow toDomain(AgenticFlowEntity entity) {
        return AgenticFlow.builder()
                .id(entity.getId().toString())
                .flowType(AgenticFlowType.valueOf(entity.getFlowType()))
                .name(entity.getName())
                .description(entity.getDescription())
                .configuration(new AgenticFlowConfiguration(entity.getConfiguration()))
                .organizationId(entity.getOrganizationId().toString())
                .status(AgenticFlowStatus.valueOf(entity.getStatus()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion())
                .build();
    }
}