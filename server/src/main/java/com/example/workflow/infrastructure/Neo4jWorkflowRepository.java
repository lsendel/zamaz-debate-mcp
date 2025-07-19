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
        List<WorkflowEntity> entities = entityRepository.searchWorkflows(
            query.organizationId(),
            query.namePattern(),
            query.statuses() != null && !query.statuses().isEmpty() ? 
                query.statuses().get(0).name() : null,
            query.offset(),
            query.limit()
        );
        
        long totalCount = entityRepository.countSearchResults(
            query.organizationId(),
            query.namePattern(),
            query.statuses() != null && !query.statuses().isEmpty() ? 
                query.statuses().get(0).name() : null
        );
        
        List<Workflow> workflows = mapper.toDomains(entities);
        
        return new WorkflowSearchResult(workflows, totalCount, query.offset(), query.limit());
    }
    
    @Override
    public List<Workflow> findAll(String organizationId, WorkflowFilter filter) {
        // For simplicity, using basic organization query and filtering in memory
        // In production, this should be optimized with custom Cypher queries
        List<Workflow> workflows = findByOrganization(organizationId);
        
        return workflows.stream()
            .filter(w -> filter.statuses() == null || filter.statuses().contains(w.getStatus()))
            .filter(w -> filter.minNodes() == null || w.getNodes().size() >= filter.minNodes())
            .filter(w -> filter.maxNodes() == null || w.getNodes().size() <= filter.maxNodes())
            .filter(w -> filter.createdAfter() == null || w.getCreatedAt().isAfter(filter.createdAfter()))
            .filter(w -> filter.updatedAfter() == null || w.getUpdatedAt().isAfter(filter.updatedAfter()))
            .collect(Collectors.toList());
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
}