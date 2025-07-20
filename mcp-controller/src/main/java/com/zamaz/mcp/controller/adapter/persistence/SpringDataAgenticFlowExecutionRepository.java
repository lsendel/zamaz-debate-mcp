package com.zamaz.mcp.controller.adapter.persistence;

import com.zamaz.mcp.controller.adapter.persistence.entity.AgenticFlowExecutionEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for AgenticFlowExecutionEntity.
 */
@Repository
public interface SpringDataAgenticFlowExecutionRepository extends JpaRepository<AgenticFlowExecutionEntity, UUID> {

    /**
     * Finds executions by flow ID with pagination.
     */
    List<AgenticFlowExecutionEntity> findByFlowIdOrderByCreatedAtDesc(UUID flowId, Pageable pageable);

    /**
     * Finds executions by debate ID.
     */
    List<AgenticFlowExecutionEntity> findByDebateIdOrderByCreatedAtDesc(UUID debateId);

    /**
     * Finds executions by flow ID within a time range.
     */
    List<AgenticFlowExecutionEntity> findByFlowIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            UUID flowId, Instant startTime, Instant endTime);

    /**
     * Counts executions for a flow.
     */
    long countByFlowId(UUID flowId);

    /**
     * Counts executions that changed the response.
     */
    long countByFlowIdAndResponseChangedTrue(UUID flowId);

    /**
     * Calculates average processing time for a flow.
     */
    @Query("SELECT AVG(e.processingTimeMs) FROM AgenticFlowExecutionEntity e WHERE e.flow.id = :flowId")
    Double calculateAverageProcessingTime(@Param("flowId") UUID flowId);

    /**
     * Finds executions with errors.
     */
    List<AgenticFlowExecutionEntity> findByFlowIdAndErrorMessageIsNotNullOrderByCreatedAtDesc(UUID flowId);

    /**
     * Finds slowest executions for a flow.
     */
    List<AgenticFlowExecutionEntity> findByFlowIdOrderByProcessingTimeMsDesc(UUID flowId, Pageable pageable);

    /**
     * Deletes executions before a certain time.
     */
    long deleteByCreatedAtBefore(Instant cutoffTime);

    /**
     * Gets flow type statistics for an organization.
     */
    @Query("""
        SELECT 
            f.flowType as flowType,
            COUNT(e) as executionCount,
            AVG(e.processingTimeMs) as avgProcessingTime,
            SUM(CASE WHEN e.responseChanged = true THEN 1 ELSE 0 END) as changeCount,
            SUM(CASE WHEN e.errorMessage IS NOT NULL THEN 1 ELSE 0 END) as errorCount
        FROM AgenticFlowExecutionEntity e
        JOIN e.flow f
        WHERE f.organizationId = :orgId
        GROUP BY f.flowType
    """)
    List<FlowTypeStatisticsProjection> getStatisticsByFlowType(@Param("orgId") UUID organizationId);

    /**
     * Projection interface for flow type statistics.
     */
    interface FlowTypeStatisticsProjection {
        String getFlowType();
        Long getExecutionCount();
        Double getAvgProcessingTime();
        Long getChangeCount();
        Long getErrorCount();
    }
}