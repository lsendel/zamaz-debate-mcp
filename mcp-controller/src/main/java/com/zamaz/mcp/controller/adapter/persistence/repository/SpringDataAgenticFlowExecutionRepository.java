package com.zamaz.mcp.controller.adapter.persistence.repository;

import com.zamaz.mcp.controller.adapter.persistence.entity.AgenticFlowExecutionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for agentic flow execution history.
 */
@Repository
public interface SpringDataAgenticFlowExecutionRepository extends JpaRepository<AgenticFlowExecutionEntity, UUID> {

    /**
     * Finds executions by flow ID.
     *
     * @param flowId   The flow ID
     * @param pageable Pagination information
     * @return Page of executions
     */
    Page<AgenticFlowExecutionEntity> findByFlowId(UUID flowId, Pageable pageable);

    /**
     * Finds executions by debate ID.
     *
     * @param debateId The debate ID
     * @return List of executions
     */
    List<AgenticFlowExecutionEntity> findByDebateId(UUID debateId);

    /**
     * Finds executions by participant ID.
     *
     * @param participantId The participant ID
     * @return List of executions
     */
    List<AgenticFlowExecutionEntity> findByParticipantId(UUID participantId);

    /**
     * Finds executions within a time range.
     *
     * @param flowId    The flow ID
     * @param startTime Start time
     * @param endTime   End time
     * @return List of executions
     */
    List<AgenticFlowExecutionEntity> findByFlowIdAndCreatedAtBetween(
            UUID flowId,
            Instant startTime,
            Instant endTime
    );

    /**
     * Counts executions by flow ID.
     *
     * @param flowId The flow ID
     * @return Count of executions
     */
    long countByFlowId(UUID flowId);

    /**
     * Counts executions that changed the response.
     *
     * @param flowId The flow ID
     * @return Count of executions that changed response
     */
    long countByFlowIdAndResponseChangedTrue(UUID flowId);

    /**
     * Calculates average processing time for a flow.
     *
     * @param flowId The flow ID
     * @return Average processing time in milliseconds
     */
    @Query("SELECT AVG(e.processingTimeMs) FROM AgenticFlowExecutionEntity e WHERE e.flow.id = :flowId")
    Double calculateAverageProcessingTime(@Param("flowId") UUID flowId);

    /**
     * Calculates response change rate for a flow.
     *
     * @param flowId The flow ID
     * @return Response change rate (0.0 to 1.0)
     */
    @Query("SELECT COUNT(CASE WHEN e.responseChanged = true THEN 1 END) * 1.0 / COUNT(*) " +
            "FROM AgenticFlowExecutionEntity e WHERE e.flow.id = :flowId")
    Double calculateResponseChangeRate(@Param("flowId") UUID flowId);

    /**
     * Finds executions with errors.
     *
     * @param flowId The flow ID
     * @return List of executions with errors
     */
    @Query("SELECT e FROM AgenticFlowExecutionEntity e WHERE e.flow.id = :flowId AND e.errorMessage IS NOT NULL")
    List<AgenticFlowExecutionEntity> findExecutionsWithErrors(@Param("flowId") UUID flowId);

    /**
     * Gets execution statistics by flow type.
     *
     * @param organizationId The organization ID
     * @return List of statistics
     */
    @Query("SELECT f.flowType as flowType, " +
            "COUNT(e.id) as executionCount, " +
            "AVG(e.processingTimeMs) as avgProcessingTime, " +
            "COUNT(CASE WHEN e.responseChanged = true THEN 1 END) * 1.0 / COUNT(*) as changeRate " +
            "FROM AgenticFlowExecutionEntity e " +
            "JOIN e.flow f " +
            "WHERE f.organizationId = :orgId " +
            "GROUP BY f.flowType")
    List<FlowTypeStatistics> getStatisticsByFlowType(@Param("orgId") UUID organizationId);

    /**
     * Finds slowest executions.
     *
     * @param flowId   The flow ID
     * @param limit    Maximum number of results
     * @return List of slowest executions
     */
    @Query("SELECT e FROM AgenticFlowExecutionEntity e WHERE e.flow.id = :flowId " +
            "ORDER BY e.processingTimeMs DESC")
    List<AgenticFlowExecutionEntity> findSlowestExecutions(@Param("flowId") UUID flowId, Pageable pageable);

    /**
     * Deletes old executions for cleanup.
     *
     * @param cutoffTime Cutoff time
     */
    void deleteByCreatedAtBefore(Instant cutoffTime);

    /**
     * Interface for flow type statistics projection.
     */
    interface FlowTypeStatistics {
        String getFlowType();
        Long getExecutionCount();
        Double getAvgProcessingTime();
        Double getChangeRate();
    }
}