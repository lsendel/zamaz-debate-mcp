package com.zamaz.mcp.controller.adapter.persistence;

import com.zamaz.mcp.common.domain.agentic.*;
import com.zamaz.mcp.common.domain.organization.OrganizationId;
import com.zamaz.mcp.controller.adapter.persistence.entity.AgenticFlowEntity;
import com.zamaz.mcp.controller.adapter.persistence.entity.AgenticFlowExecutionEntity;
import com.zamaz.mcp.controller.adapter.persistence.mapper.AgenticFlowExecutionEntityMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JPA implementation of AgenticFlowAnalyticsRepository.
 */
@Repository
@Transactional
public class JpaAgenticFlowAnalyticsRepository implements AgenticFlowAnalyticsRepository {

    private final SpringDataAgenticFlowExecutionRepository executionRepository;
    private final SpringDataAgenticFlowRepository flowRepository;
    private final AgenticFlowExecutionEntityMapper mapper;

    public JpaAgenticFlowAnalyticsRepository(
            SpringDataAgenticFlowExecutionRepository executionRepository,
            SpringDataAgenticFlowRepository flowRepository,
            AgenticFlowExecutionEntityMapper mapper) {
        this.executionRepository = Objects.requireNonNull(executionRepository);
        this.flowRepository = Objects.requireNonNull(flowRepository);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public AgenticFlowExecution save(AgenticFlowExecution execution) {
        Objects.requireNonNull(execution, "Execution cannot be null");
        
        UUID flowId = UUID.fromString(execution.getFlowId().getValue());
        AgenticFlowEntity flowEntity = flowRepository.findById(flowId)
                .orElseThrow(() -> new IllegalArgumentException("Flow not found: " + flowId));
        
        AgenticFlowExecutionEntity entity = mapper.toEntity(execution, flowEntity);
        AgenticFlowExecutionEntity saved = executionRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<AgenticFlowExecution> findById(UUID id) {
        Objects.requireNonNull(id, "Execution ID cannot be null");
        
        return executionRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<AgenticFlowExecution> findByFlowId(AgenticFlowId flowId, int limit) {
        Objects.requireNonNull(flowId, "Flow ID cannot be null");
        
        UUID uuid = UUID.fromString(flowId.getValue());
        Pageable pageable = limit > 0 ? PageRequest.of(0, limit) : Pageable.unpaged();
        
        return executionRepository.findByFlowIdOrderByCreatedAtDesc(uuid, pageable)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<AgenticFlowExecution> findByDebateId(UUID debateId) {
        Objects.requireNonNull(debateId, "Debate ID cannot be null");
        
        return executionRepository.findByDebateIdOrderByCreatedAtDesc(debateId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<AgenticFlowExecution> findByFlowIdAndTimeRange(
            AgenticFlowId flowId, Instant startTime, Instant endTime) {
        Objects.requireNonNull(flowId, "Flow ID cannot be null");
        Objects.requireNonNull(startTime, "Start time cannot be null");
        Objects.requireNonNull(endTime, "End time cannot be null");
        
        UUID uuid = UUID.fromString(flowId.getValue());
        return executionRepository.findByFlowIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                        uuid, startTime, endTime)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countExecutions(AgenticFlowId flowId) {
        Objects.requireNonNull(flowId, "Flow ID cannot be null");
        
        return executionRepository.countByFlowId(UUID.fromString(flowId.getValue()));
    }

    @Override
    public long countResponseChanges(AgenticFlowId flowId) {
        Objects.requireNonNull(flowId, "Flow ID cannot be null");
        
        return executionRepository.countByFlowIdAndResponseChangedTrue(
                UUID.fromString(flowId.getValue()));
    }

    @Override
    public Double calculateAverageProcessingTime(AgenticFlowId flowId) {
        Objects.requireNonNull(flowId, "Flow ID cannot be null");
        
        return executionRepository.calculateAverageProcessingTime(
                UUID.fromString(flowId.getValue()));
    }

    @Override
    public Double calculateResponseChangeRate(AgenticFlowId flowId) {
        Objects.requireNonNull(flowId, "Flow ID cannot be null");
        
        UUID uuid = UUID.fromString(flowId.getValue());
        long totalCount = executionRepository.countByFlowId(uuid);
        if (totalCount == 0) {
            return null;
        }
        
        long changeCount = executionRepository.countByFlowIdAndResponseChangedTrue(uuid);
        return (double) changeCount / totalCount;
    }

    @Override
    public List<AgenticFlowExecution> findExecutionsWithErrors(AgenticFlowId flowId) {
        Objects.requireNonNull(flowId, "Flow ID cannot be null");
        
        return executionRepository.findByFlowIdAndErrorMessageIsNotNullOrderByCreatedAtDesc(
                        UUID.fromString(flowId.getValue()))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Map<AgenticFlowType, FlowTypeStatistics> getStatisticsByFlowType(OrganizationId organizationId) {
        Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        
        List<SpringDataAgenticFlowExecutionRepository.FlowTypeStatisticsProjection> projections = 
                executionRepository.getStatisticsByFlowType(UUID.fromString(organizationId.getValue()));
        
        Map<AgenticFlowType, FlowTypeStatistics> result = new HashMap<>();
        
        for (SpringDataAgenticFlowExecutionRepository.FlowTypeStatisticsProjection projection : projections) {
            try {
                AgenticFlowType flowType = AgenticFlowType.valueOf(projection.getFlowType());
                double changeRate = projection.getExecutionCount() > 0 
                        ? (double) projection.getChangeCount() / projection.getExecutionCount() 
                        : 0.0;
                
                FlowTypeStatistics stats = new FlowTypeStatistics(
                        projection.getExecutionCount(),
                        projection.getAvgProcessingTime() != null ? projection.getAvgProcessingTime() : 0.0,
                        changeRate,
                        projection.getErrorCount()
                );
                
                result.put(flowType, stats);
            } catch (IllegalArgumentException e) {
                // Skip unknown flow types
            }
        }
        
        return result;
    }

    @Override
    public List<AgenticFlowExecution> findSlowestExecutions(AgenticFlowId flowId, int limit) {
        Objects.requireNonNull(flowId, "Flow ID cannot be null");
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }
        
        Pageable pageable = PageRequest.of(0, limit);
        return executionRepository.findByFlowIdOrderByProcessingTimeMsDesc(
                        UUID.fromString(flowId.getValue()), pageable)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long deleteExecutionsBefore(Instant cutoffTime) {
        Objects.requireNonNull(cutoffTime, "Cutoff time cannot be null");
        
        return executionRepository.deleteByCreatedAtBefore(cutoffTime);
    }
}