package com.zamaz.mcp.controller.adapter.persistence;

import com.zamaz.mcp.common.domain.agentic.AgenticFlowAnalyticsRepository;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowExecution;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowId;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowType;
import com.zamaz.mcp.common.domain.organization.OrganizationId;
import com.zamaz.mcp.controller.adapter.persistence.entity.AgenticFlowExecutionEntity;
import com.zamaz.mcp.controller.adapter.persistence.repository.SpringDataAgenticFlowExecutionRepository;
import com.zamaz.mcp.controller.adapter.persistence.repository.SpringDataAgenticFlowRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * PostgreSQL implementation of the AgenticFlowAnalyticsRepository using Spring
 * Data JPA.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PostgresAgenticFlowAnalyticsRepository implements AgenticFlowAnalyticsRepository {

    private final SpringDataAgenticFlowExecutionRepository executionRepository;
    private final SpringDataAgenticFlowRepository flowRepository;

    @Override
    public AgenticFlowExecution save(AgenticFlowExecution execution) {
        log.debug("Saving agentic flow execution: {}", execution.getId());

        AgenticFlowExecutionEntity entity = toEntity(execution);
        AgenticFlowExecutionEntity savedEntity = executionRepository.save(entity);

        log.info("Saved agentic flow execution with ID: {}", execution.getId());
        return toDomain(savedEntity);
    }

    @Override
    public Optional<AgenticFlowExecution> findById(UUID id) {
        log.debug("Finding agentic flow execution by ID: {}", id);

        return executionRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public List<AgenticFlowExecution> findByFlowId(AgenticFlowId flowId, int limit) {
        log.debug("Finding executions for flow: {} with limit: {}", flowId, limit);

        UUID uuid = UUID.fromString(flowId.getValue());
        if (limit > 0) {
            Pageable pageable = PageRequest.of(0, limit);
            return executionRepository.findByFlowId(uuid, pageable)
                    .getContent()
                    .stream()
                    .map(this::toDomain)
                    .collect(Collectors.toList());
        } else {
            return executionRepository.findByFlowId(uuid, Pageable.unpaged())
                    .getContent()
                    .stream()
                    .map(this::toDomain)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public List<AgenticFlowExecution> findByDebateId(UUID debateId) {
        log.debug("Finding executions for debate: {}", debateId);

        return executionRepository.findByDebateId(debateId)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AgenticFlowExecution> findByFlowIdAndTimeRange(
            AgenticFlowId flowId,
            Instant startTime,
            Instant endTime) {
        log.debug("Finding executions for flow: {} between {} and {}", flowId, startTime, endTime);

        UUID uuid = UUID.fromString(flowId.getValue());
        return executionRepository.findByFlowIdAndCreatedAtBetween(uuid, startTime, endTime)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countExecutions(AgenticFlowId flowId) {
        log.debug("Counting executions for flow: {}", flowId);

        UUID uuid = UUID.fromString(flowId.getValue());
        return executionRepository.countByFlowId(uuid);
    }

    @Override
    public long countResponseChanges(AgenticFlowId flowId) {
        log.debug("Counting response changes for flow: {}", flowId);

        UUID uuid = UUID.fromString(flowId.getValue());
        return executionRepository.countByFlowIdAndResponseChangedTrue(uuid);
    }

    @Override
    public Double calculateAverageProcessingTime(AgenticFlowId flowId) {
        log.debug("Calculating average processing time for flow: {}", flowId);

        UUID uuid = UUID.fromString(flowId.getValue());
        return executionRepository.calculateAverageProcessingTime(uuid);
    }

    @Override
    public Double calculateResponseChangeRate(AgenticFlowId flowId) {
        log.debug("Calculating response change rate for flow: {}", flowId);

        UUID uuid = UUID.fromString(flowId.getValue());
        return executionRepository.calculateResponseChangeRate(uuid);
    }

    @Override
    public List<AgenticFlowExecution> findExecutionsWithErrors(AgenticFlowId flowId) {
        log.debug("Finding executions with errors for flow: {}", flowId);

        UUID uuid = UUID.fromString(flowId.getValue());
        return executionRepository.findExecutionsWithErrors(uuid)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Map<AgenticFlowType, FlowTypeStatistics> getStatisticsByFlowType(OrganizationId organizationId) {
        log.debug("Getting statistics by flow type for organization: {}", organizationId);

        UUID orgUuid = UUID.fromString(organizationId.getValue());
        List<SpringDataAgenticFlowExecutionRepository.FlowTypeStatistics> stats = executionRepository
                .getStatisticsByFlowType(orgUuid);

        Map<AgenticFlowType, FlowTypeStatistics> result = new HashMap<>();
        for (SpringDataAgenticFlowExecutionRepository.FlowTypeStatistics stat : stats) {
            AgenticFlowType flowType = AgenticFlowType.valueOf(stat.getFlowType());
            FlowTypeStatistics domainStat = new FlowTypeStatistics(
                    stat.getExecutionCount(),
                    stat.getAvgProcessingTime() != null ? stat.getAvgProcessingTime() : 0.0,
                    stat.getChangeRate() != null ? stat.getChangeRate() : 0.0,
                    0L // Error count would need a separate query
            );
            result.put(flowType, domainStat);
        }

        return result;
    }

    @Override
    public List<AgenticFlowExecution> findSlowestExecutions(AgenticFlowId flowId, int limit) {
        log.debug("Finding slowest executions for flow: {} with limit: {}", flowId, limit);

        UUID uuid = UUID.fromString(flowId.getValue());
        Pageable pageable = PageRequest.of(0, limit);
        return executionRepository.findSlowestExecutions(uuid, pageable)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long deleteExecutionsBefore(Instant cutoffTime) {
        log.debug("Deleting executions before: {}", cutoffTime);

        long countBefore = executionRepository.count();
        executionRepository.deleteByCreatedAtBefore(cutoffTime);
        long countAfter = executionRepository.count();

        long deletedCount = countBefore - countAfter;
        log.info("Deleted {} old executions before {}", deletedCount, cutoffTime);
        return deletedCount;
    }

    /**
     * Converts domain object to entity
     */
    private AgenticFlowExecutionEntity toEntity(AgenticFlowExecution execution) {
        // Find the flow entity
        UUID flowUuid = UUID.fromString(execution.getFlowId().getValue());
        var flowEntity = flowRepository.findById(flowUuid)
                .orElseThrow(() -> new IllegalArgumentException("Flow not found: " + execution.getFlowId()));

        return AgenticFlowExecutionEntity.builder()
                .id(execution.getId())
                .flow(flowEntity)
                .debateId(execution.getDebateId())
                .participantId(execution.getParticipantId())
                .prompt(execution.getPrompt())
                .result(execution.getResult())
                .processingTimeMs(execution.getProcessingTimeMs())
                .responseChanged(execution.isResponseChanged())
                .errorMessage(execution.getErrorMessage())
                .createdAt(execution.getCreatedAt())
                .build();
    }

    /**
     * Converts entity to domain object
     */
    private AgenticFlowExecution toDomain(AgenticFlowExecutionEntity entity) {
        return new AgenticFlowExecution(
                entity.getId(),
                new AgenticFlowId(entity.getFlow().getId().toString()),
                entity.getDebateId(),
                entity.getParticipantId(),
                entity.getPrompt(),
                entity.getResult(),
                entity.getProcessingTimeMs(),
                entity.getResponseChanged(),
                entity.getErrorMessage(),
                entity.getCreatedAt());
    }
}