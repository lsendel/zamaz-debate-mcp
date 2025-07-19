package com.example.workflow.infrastructure;

import com.example.workflow.domain.*;
import com.example.workflow.domain.ports.WorkflowRepository;
import com.example.workflow.infrastructure.neo4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Neo4j implementation of WorkflowRepository
 * Provides graph-based storage for workflow structures with Cypher queries
 */
@Repository
@Transactional
public class Neo4jWorkflowRepository implements WorkflowRepository {
    
    private final Neo4jWorkflowEntityRepository entityRepository;
    private final WorkflowEntityMapper mapper;
    
    @Autowired
    public Neo4jWorkflowRepository(Neo4jWorkflowEntityRepository entityRepository, 
                                  WorkflowEntityMapper mapper) {
        this.entityRepository = entityRepository;
        this.mapper = mapper;
    }
    
    @Override
    public void save(Workflow workflow) {
        WorkflowEntity entity = mapper.toEntity(workflow);
        entityRepository.save(entity);
    }
    
    @Override
    public Optional<Workflow> findById(WorkflowId id) {
        return entityRepository.findById(id.getValue())
            .map(mapper::toDomain);
    }
    
    @Override
    public List<Workflow> findByOrganization(String organizationId) {
        List<WorkflowEntity> entities = entityRepository.findByOrganizationId(organizationId);
        return mapper.toDomains(entities);
    }
    
    @Override
    public List<Workflow> findByStatus(WorkflowStatus status) {
        List<WorkflowEntity> entities = entityRepository.findByStatus(status.name());
        return mapper.toDomains(entities);
    }
    
    @Override
    public void delete(WorkflowId id) {
        entityRepository.deleteById(id.getValue());
    }
    
    @Override
    public boolean exists(WorkflowId id) {
        return entityRepository.existsById(id.getValue());
    }
    
    // Advanced query operations
    @Override
    public List<Workflow> findByOrganizationAndStatus(String organizationId, WorkflowStatus status) {
        List<WorkflowEntity> entities = entityRepository.findByOrganizationIdAndStatus(organizationId, status.name());
        return mapper.toDomains(entities);
    }
    
    @Override
    public List<Workflow> findByNameContaining(String organizationId, String namePattern) {
        List<WorkflowEntity> entities = entityRepository.findByNameContainingIgnoreCase(namePattern);
        return mapper.toDomains(entities).stream()
            .filter(w -> w.getOrganizationId().equals(organizationId))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Workflow> findCreatedBetween(String organizationId, Instant fromTime, Instant toTime) {
        List<WorkflowEntity> entities = entityRepository.findCreatedBetween(organizationId, fromTime, toTime);
        return mapper.toDomains(entities);
    }
    
    @Override
    public List<Workflow> findUpdatedAfter(String organizationId, Instant afterTime) {
        List<WorkflowEntity> entities = entityRepository.findUpdatedAfter(organizationId, afterTime);
        return mapper.toDomains(entities);
    }
    
    // Workflow structure queries
    @Override
    public List<Workflow> findByNodeType(String organizationId, NodeType nodeType) {
        List<WorkflowEntity> entities = entityRepository.findByNodeType(organizationId, nodeType.name());
        return mapper.toDomains(entities);
    }
    
    @Override
    public List<Workflow> findByNodeCount(String organizationId, int minNodes, int maxNodes) {
        List<WorkflowEntity> entities = entityRepository.findByNodeCount(organizationId, minNodes, maxNodes);
        return mapper.toDomains(entities);
    }
    
    @Override
    public Optional<Workflow> findByNodeId(NodeId nodeId) {
        return entityRepository.findByNodeId(nodeId.getValue())
            .map(mapper::toDomain);
    }
    
    // Search and filtering
    @Override
    public WorkflowSearchResult search(WorkflowSearchQuery query) {
        List<WorkflowEntity> entities;
        long totalCount;
        
        // Use full-text search if search term is provided
        if (query.namePattern() != null && !query.namePattern().trim().isEmpty()) {
            entities = entityRepository.fullTextSearch(
                query.organizationId(),
                query.namePattern(),
                query.offset(),
                query.limit()
            );
            totalCount = entityRepository.countFullTextSearch(
                query.organizationId(),
                query.namePattern()
            );
        } else {
            // Use complex filter query
            List<String> statusStrings = query.statuses() != null ? 
                query.statuses().stream().map(Enum::name).collect(Collectors.toList()) : null;
            List<String> nodeTypeStrings = query.nodeTypes() != null ? 
                query.nodeTypes().stream().map(Enum::name).collect(Collectors.toList()) : null;
                
            entities = entityRepository.findWithComplexFilter(
                query.organizationId(),
                statusStrings,
                nodeTypeStrings,
                query.minNodes(),
                query.maxNodes(),
                query.createdAfter(),
                query.createdBefore(),
                query.offset(),
                query.limit()
            );
            
            totalCount = entityRepository.countWithComplexFilter(
                query.organizationId(),
                statusStrings,
                nodeTypeStrings,
                query.minNodes(),
                query.maxNodes(),
                query.createdAfter(),
                query.createdBefore()
            );
        }
        
        List<Workflow> workflows = mapper.toDomains(entities);
        return new WorkflowSearchResult(workflows, totalCount, query.offset(), query.limit());
    }
    
    @Override
    public List<Workflow> findAll(String organizationId, WorkflowFilter filter) {
        // Use optimized Cypher query instead of in-memory filtering
        List<String> statusStrings = filter.statuses() != null ? 
            filter.statuses().stream().map(Enum::name).collect(Collectors.toList()) : null;
        List<String> nodeTypeStrings = filter.requiredNodeTypes() != null ? 
            filter.requiredNodeTypes().stream().map(Enum::name).collect(Collectors.toList()) : null;
            
        List<WorkflowEntity> entities = entityRepository.findWithComplexFilter(
            organizationId,
            statusStrings,
            nodeTypeStrings,
            filter.minNodes(),
            filter.maxNodes(),
            filter.createdAfter(),
            filter.updatedAfter(),
            0,
            Integer.MAX_VALUE
        );
        
        return mapper.toDomains(entities);
    }
    
    // Statistics and analytics
    @Override
    public long countByOrganization(String organizationId) {
        return entityRepository.countByOrganizationId(organizationId);
    }
    
    @Override
    public long countByStatus(String organizationId, WorkflowStatus status) {
        return entityRepository.countByOrganizationIdAndStatus(organizationId, status.name());
    }
    
    @Override
    public WorkflowStatistics getStatistics(String organizationId) {
        long totalWorkflows = countByOrganization(organizationId);
        long activeWorkflows = countByStatus(organizationId, WorkflowStatus.ACTIVE);
        long completedWorkflows = countByStatus(organizationId, WorkflowStatus.COMPLETED);
        long draftWorkflows = countByStatus(organizationId, WorkflowStatus.DRAFT);
        
        Double avgNodes = entityRepository.getAverageNodeCount(organizationId);
        Double avgEdges = entityRepository.getAverageEdgeCount(organizationId);
        
        Optional<Instant> lastCreated = entityRepository.getLastCreatedTime(organizationId);
        Optional<Instant> lastUpdated = entityRepository.getLastUpdatedTime(organizationId);
        
        return new WorkflowStatistics(
            totalWorkflows,
            activeWorkflows,
            completedWorkflows,
            draftWorkflows,
            avgNodes != null ? avgNodes : 0.0,
            avgEdges != null ? avgEdges : 0.0,
            lastCreated.orElse(null),
            lastUpdated.orElse(null)
        );
    }
    
    // Batch operations
    @Override
    public void saveAll(List<Workflow> workflows) {
        List<WorkflowEntity> entities = mapper.toEntities(workflows);
        entityRepository.saveAll(entities);
    }
    
    @Override
    public void deleteAll(List<WorkflowId> workflowIds) {
        List<String> ids = workflowIds.stream()
            .map(WorkflowId::getValue)
            .collect(Collectors.toList());
        entityRepository.deleteAllByIds(ids);
    }
    
    // Additional workflow analysis methods
    
    /**
     * Check if workflow has valid structure (no orphaned edges, cycles, etc.)
     */
    public boolean isWorkflowValid(WorkflowId workflowId) {
        String id = workflowId.getValue();
        return !entityRepository.hasOrphanedEdges(id) && 
               !entityRepository.hasIsolatedNodes(id) && 
               !entityRepository.hasSelfLoops(id);
    }
    
    /**
     * Check if workflow has cycles
     */
    public boolean hasWorkflowCycles(WorkflowId workflowId) {
        return entityRepository.hasCycles(workflowId.getValue());
    }
    
    /**
     * Get maximum path length in workflow
     */
    public int getMaxPathLength(WorkflowId workflowId) {
        Integer maxLength = entityRepository.findMaxPathLength(workflowId.getValue());
        return maxLength != null ? maxLength : 0;
    }
    
    /**
     * Check if there's a path between two nodes
     */
    public boolean hasPathBetweenNodes(WorkflowId workflowId, NodeId startNodeId, NodeId endNodeId) {
        return entityRepository.hasPathBetweenNodes(
            workflowId.getValue(), 
            startNodeId.getValue(), 
            endNodeId.getValue()
        );
    }
    
    /**
     * Count nodes by type in a workflow
     */
    public long countNodesByType(WorkflowId workflowId, NodeType nodeType) {
        return entityRepository.countNodesByType(workflowId.getValue(), nodeType.name());
    }
    
    /**
     * Get workflow with all nodes and edges loaded (optimized query)
     */
    public Optional<Workflow> findByIdWithFullStructure(WorkflowId workflowId) {
        return entityRepository.findByIdWithNodesAndEdges(workflowId.getValue())
            .map(mapper::toDomain);
    }
    
    /**
     * Get workflows for organization with nodes pre-loaded (optimized query)
     */
    public List<Workflow> findByOrganizationWithNodes(String organizationId) {
        List<WorkflowEntity> entities = entityRepository.findByOrganizationWithNodes(organizationId);
        return mapper.toDomains(entities);
    }
    
    /**
     * Batch upsert workflows for bulk operations
     */
    public void batchUpsertWorkflows(List<Workflow> workflows) {
        List<Map<String, Object>> workflowMaps = workflows.stream()
            .map(this::workflowToMap)
            .collect(Collectors.toList());
        entityRepository.batchUpsertWorkflows(workflowMaps);
    }
    
    /**
     * Convert workflow to map for batch operations
     */
    private Map<String, Object> workflowToMap(Workflow workflow) {
        Map<String, Object> workflowMap = new HashMap<>();
        workflowMap.put("id", workflow.getId().getValue());
        workflowMap.put("name", workflow.getName());
        workflowMap.put("organizationId", workflow.getOrganizationId());
        workflowMap.put("status", workflow.getStatus().name());
        workflowMap.put("updatedAt", workflow.getUpdatedAt());
        
        List<Map<String, Object>> nodeMaps = workflow.getNodes().stream()
            .map(node -> {
                Map<String, Object> nodeMap = new HashMap<>();
                nodeMap.put("id", node.getId().getValue());
                nodeMap.put("type", node.getType().name());
                nodeMap.put("name", node.getLabel());
                return nodeMap;
            })
            .collect(Collectors.toList());
        workflowMap.put("nodes", nodeMaps);
        
        return workflowMap;
    }
}