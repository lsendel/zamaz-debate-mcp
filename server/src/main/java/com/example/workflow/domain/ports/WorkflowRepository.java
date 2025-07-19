package com.example.workflow.domain.ports;

import com.example.workflow.domain.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository port for workflow persistence operations
 * Supports CRUD operations, search, and filtering capabilities
 */
public interface WorkflowRepository {
    
    // Basic CRUD operations
    void save(Workflow workflow);
    Optional<Workflow> findById(WorkflowId id);
    List<Workflow> findByOrganization(String organizationId);
    List<Workflow> findByStatus(WorkflowStatus status);
    void delete(WorkflowId id);
    boolean exists(WorkflowId id);
    
    // Advanced query operations
    List<Workflow> findByOrganizationAndStatus(String organizationId, WorkflowStatus status);
    List<Workflow> findByNameContaining(String organizationId, String namePattern);
    List<Workflow> findCreatedBetween(String organizationId, Instant fromTime, Instant toTime);
    List<Workflow> findUpdatedAfter(String organizationId, Instant afterTime);
    
    // Workflow structure queries
    List<Workflow> findByNodeType(String organizationId, NodeType nodeType);
    List<Workflow> findByNodeCount(String organizationId, int minNodes, int maxNodes);
    Optional<Workflow> findByNodeId(NodeId nodeId);
    
    // Search and filtering
    WorkflowSearchResult search(WorkflowSearchQuery query);
    List<Workflow> findAll(String organizationId, WorkflowFilter filter);
    
    // Statistics and analytics
    long countByOrganization(String organizationId);
    long countByStatus(String organizationId, WorkflowStatus status);
    WorkflowStatistics getStatistics(String organizationId);
    
    // Batch operations
    void saveAll(List<Workflow> workflows);
    void deleteAll(List<WorkflowId> workflowIds);
    
    /**
     * Workflow search query specification
     */
    record WorkflowSearchQuery(
        String organizationId,
        String namePattern,
        List<WorkflowStatus> statuses,
        List<NodeType> nodeTypes,
        Instant createdAfter,
        Instant createdBefore,
        Integer minNodes,
        Integer maxNodes,
        int offset,
        int limit,
        WorkflowSortBy sortBy,
        SortDirection sortDirection
    ) {}
    
    /**
     * Workflow filter specification
     */
    record WorkflowFilter(
        List<WorkflowStatus> statuses,
        List<NodeType> requiredNodeTypes,
        Integer minNodes,
        Integer maxNodes,
        Instant createdAfter,
        Instant updatedAfter
    ) {}
    
    /**
     * Workflow search result
     */
    record WorkflowSearchResult(
        List<Workflow> workflows,
        long totalCount,
        int offset,
        int limit
    ) {}
    
    /**
     * Workflow statistics
     */
    record WorkflowStatistics(
        long totalWorkflows,
        long activeWorkflows,
        long completedWorkflows,
        long draftWorkflows,
        double averageNodes,
        double averageEdges,
        Instant lastCreated,
        Instant lastUpdated
    ) {}
    
    /**
     * Sort options for workflows
     */
    enum WorkflowSortBy {
        NAME, CREATED_AT, UPDATED_AT, STATUS, NODE_COUNT
    }
    
    /**
     * Sort direction
     */
    enum SortDirection {
        ASC, DESC
    }
}