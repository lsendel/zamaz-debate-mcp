package com.zamaz.mcp.common.domain.agentic;

import com.zamaz.mcp.common.domain.organization.OrganizationId;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for agentic flow analytics and execution history.
 */
public interface AgenticFlowAnalyticsRepository {

    /**
     * Saves an agentic flow execution record.
     *
     * @param execution The execution to save
     * @return The saved execution
     */
    AgenticFlowExecution save(AgenticFlowExecution execution);

    /**
     * Finds an execution by its ID.
     *
     * @param id The execution ID
     * @return An Optional containing the execution, or empty if not found
     */
    Optional<AgenticFlowExecution> findById(UUID id);

    /**
     * Finds all executions for a specific flow.
     *
     * @param flowId The flow ID
     * @param limit  Maximum number of results (0 for no limit)
     * @return List of executions
     */
    List<AgenticFlowExecution> findByFlowId(AgenticFlowId flowId, int limit);

    /**
     * Finds executions for a specific debate.
     *
     * @param debateId The debate ID
     * @return List of executions
     */
    List<AgenticFlowExecution> findByDebateId(UUID debateId);

    /**
     * Finds executions within a time range.
     *
     * @param flowId    The flow ID
     * @param startTime Start time
     * @param endTime   End time
     * @return List of executions
     */
    List<AgenticFlowExecution> findByFlowIdAndTimeRange(
            AgenticFlowId flowId,
            Instant startTime,
            Instant endTime);

    /**
     * Counts total executions for a flow.
     *
     * @param flowId The flow ID
     * @return Total execution count
     */
    long countExecutions(AgenticFlowId flowId);

    /**
     * Counts executions that changed the response.
     *
     * @param flowId The flow ID
     * @return Count of executions that changed response
     */
    long countResponseChanges(AgenticFlowId flowId);

    /**
     * Calculates average processing time for a flow.
     *
     * @param flowId The flow ID
     * @return Average processing time in milliseconds, or null if no executions
     */
    Double calculateAverageProcessingTime(AgenticFlowId flowId);

    /**
     * Calculates response change rate for a flow.
     *
     * @param flowId The flow ID
     * @return Response change rate (0.0 to 1.0), or null if no executions
     */
    Double calculateResponseChangeRate(AgenticFlowId flowId);

    /**
     * Finds executions with errors.
     *
     * @param flowId The flow ID
     * @return List of executions with errors
     */
    List<AgenticFlowExecution> findExecutionsWithErrors(AgenticFlowId flowId);

    /**
     * Gets execution statistics by flow type for an organization.
     *
     * @param organizationId The organization ID
     * @return Map of flow type to statistics
     */
    Map<AgenticFlowType, FlowTypeStatistics> getStatisticsByFlowType(OrganizationId organizationId);

    /**
     * Finds the slowest executions for a flow.
     *
     * @param flowId The flow ID
     * @param limit  Maximum number of results
     * @return List of slowest executions
     */
    List<AgenticFlowExecution> findSlowestExecutions(AgenticFlowId flowId, int limit);

    /**
     * Deletes old executions for cleanup.
     *
     * @param cutoffTime Cutoff time - executions before this time will be deleted
     * @return Number of deleted executions
     */
    long deleteExecutionsBefore(Instant cutoffTime);

    /**
     * Statistics for a flow type.
     */
    class FlowTypeStatistics {
        private final long executionCount;
        private final double avgProcessingTime;
        private final double changeRate;
        private final long errorCount;

        public FlowTypeStatistics(long executionCount, double avgProcessingTime, double changeRate, long errorCount) {
            this.executionCount = executionCount;
            this.avgProcessingTime = avgProcessingTime;
            this.changeRate = changeRate;
            this.errorCount = errorCount;
        }

        public long getExecutionCount() {
            return executionCount;
        }

        public double getAvgProcessingTime() {
            return avgProcessingTime;
        }

        public double getChangeRate() {
            return changeRate;
        }

        public long getErrorCount() {
            return errorCount;
        }

        public double getErrorRate() {
            return executionCount > 0 ? (double) errorCount / executionCount : 0.0;
        }
    }
}