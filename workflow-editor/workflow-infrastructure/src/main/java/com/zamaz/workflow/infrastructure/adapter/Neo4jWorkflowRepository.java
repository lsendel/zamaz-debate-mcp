package com.zamaz.workflow.infrastructure.adapter;

import com.zamaz.workflow.domain.entity.Workflow;
import com.zamaz.workflow.domain.repository.WorkflowRepository;
import com.zamaz.workflow.domain.valueobject.OrganizationId;
import com.zamaz.workflow.domain.valueobject.WorkflowId;
import com.zamaz.workflow.infrastructure.adapter.mapper.WorkflowMapper;
import com.zamaz.workflow.infrastructure.entity.WorkflowEntity;
import lombok.RequiredArgsConstructor;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Result;
import org.neo4j.driver.Value;
import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class Neo4jWorkflowRepository implements WorkflowRepository {
    private final Neo4jOperations neo4jOperations;
    private final WorkflowMapper workflowMapper;
    private final Driver neo4jDriver;
    
    @Override
    public void save(Workflow workflow) {
        WorkflowEntity entity = workflowMapper.toEntity(workflow);
        
        // Save workflow and nodes
        neo4jOperations.save(entity);
        
        // Save connections using custom Cypher query
        saveConnections(workflow);
    }
    
    @Override
    public Optional<Workflow> findById(WorkflowId id) {
        String cypher = """
            MATCH (w:Workflow {id: $workflowId})
            OPTIONAL MATCH (w)-[:CONTAINS]->(n:WorkflowNode)
            OPTIONAL MATCH (n)-[c:CONNECTS_TO]->(target:WorkflowNode)
            RETURN w, collect(DISTINCT n) as nodes, 
                   collect(DISTINCT {source: n, connection: c, target: target}) as connections
            """;
        
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, Map.of("workflowId", id.getValue()));
            
            if (result.hasNext()) {
                org.neo4j.driver.Record record = result.next();
                WorkflowEntity workflowEntity = mapToWorkflowEntity(record.get("w"));
                
                if (workflowEntity != null) {
                    // Load nodes and connections
                    loadNodesAndConnections(workflowEntity, record);
                    return Optional.of(workflowMapper.toDomain(workflowEntity));
                }
            }
        }
        
        return Optional.empty();
    }
    
    @Override
    public List<Workflow> findByOrganization(OrganizationId organizationId) {
        String cypher = """
            MATCH (w:Workflow {organizationId: $orgId})
            OPTIONAL MATCH (w)-[:CONTAINS]->(n:WorkflowNode)
            RETURN w, collect(DISTINCT n) as nodes
            ORDER BY w.createdAt DESC
            """;
        
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, Map.of("orgId", organizationId.getValue()));
            
            List<Workflow> workflows = new ArrayList<>();
            while (result.hasNext()) {
                org.neo4j.driver.Record record = result.next();
                WorkflowEntity entity = mapToWorkflowEntity(record.get("w"));
                if (entity != null) {
                    workflows.add(workflowMapper.toDomain(entity));
                }
            }
            
            return workflows;
        }
    }
    
    @Override
    public List<Workflow> findActiveWorkflows(OrganizationId organizationId) {
        String cypher = """
            MATCH (w:Workflow {organizationId: $orgId, status: 'ACTIVE'})
            OPTIONAL MATCH (w)-[:CONTAINS]->(n:WorkflowNode)
            RETURN w, collect(DISTINCT n) as nodes
            ORDER BY w.createdAt DESC
            """;
        
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, Map.of("orgId", organizationId.getValue()));
            
            List<Workflow> workflows = new ArrayList<>();
            while (result.hasNext()) {
                org.neo4j.driver.Record record = result.next();
                WorkflowEntity entity = mapToWorkflowEntity(record.get("w"));
                if (entity != null) {
                    workflows.add(workflowMapper.toDomain(entity));
                }
            }
            
            return workflows;
        }
    }
    
    @Override
    public void delete(WorkflowId id) {
        String cypher = """
            MATCH (w:Workflow {id: $workflowId})
            OPTIONAL MATCH (w)-[:CONTAINS]->(n:WorkflowNode)
            OPTIONAL MATCH (n)-[c:CONNECTS_TO]->()
            DETACH DELETE w, n, c
            """;
        
        try (Session session = neo4jDriver.session()) {
            session.run(cypher, Map.of("workflowId", id.getValue()));
        }
    }
    
    @Override
    public boolean exists(WorkflowId id) {
        String cypher = "MATCH (w:Workflow {id: $workflowId}) RETURN count(w) > 0 as exists";
        
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, Map.of("workflowId", id.getValue()));
            
            if (result.hasNext()) {
                return result.next().get("exists").asBoolean();
            }
        }
        
        return false;
    }
    
    private void saveConnections(Workflow workflow) {
        if (workflow.getConnections().isEmpty()) {
            return;
        }
        
        String cypher = """
            MATCH (source:WorkflowNode {id: $sourceId})
            MATCH (target:WorkflowNode {id: $targetId})
            MERGE (source)-[c:CONNECTS_TO {id: $connectionId}]->(target)
            SET c.label = $label,
                c.order = $order
            """;
        
        try (Session session = neo4jDriver.session()) {
            workflow.getConnections().forEach(connection -> {
                session.run(cypher, Map.of(
                    "sourceId", connection.getSourceNodeId(),
                    "targetId", connection.getTargetNodeId(),
                    "connectionId", connection.getId(),
                    "label", connection.getLabel() != null ? connection.getLabel() : "",
                    "order", connection.getOrder()
                ));
            });
        }
    }
    
    private WorkflowEntity mapToWorkflowEntity(Value value) {
        if (value.isNull()) {
            return null;
        }
        
        var node = value.asNode();
        
        return WorkflowEntity.builder()
                .id(node.get("id").asString())
                .name(node.get("name").asString())
                .description(node.get("description").asString(null))
                .status(node.get("status").asString())
                .organizationId(node.get("organizationId").asString())
                .createdAt(java.time.Instant.parse(node.get("createdAt").asString()))
                .updatedAt(node.get("updatedAt").isNull() ? null : 
                           java.time.Instant.parse(node.get("updatedAt").asString()))
                .build();
    }
    
    private void loadNodesAndConnections(WorkflowEntity workflow, org.neo4j.driver.Record record) {
        // Load nodes
        Value nodesValue = record.get("nodes");
        if (!nodesValue.isNull()) {
            List<WorkflowNodeEntity> nodes = nodesValue.asList(value -> {
                if (value.isNull()) return null;
                var node = value.asNode();
                
                return WorkflowNodeEntity.builder()
                        .id(node.get("id").asString())
                        .type(node.get("type").asString())
                        .name(node.get("name").asString())
                        .description(node.get("description").asString(null))
                        .positionX(node.get("positionX").asDouble(0.0))
                        .positionY(node.get("positionY").asDouble(0.0))
                        .build();
            }).stream()
            .filter(node -> node != null)
            .collect(Collectors.toList());
            
            workflow.setNodes(nodes);
        }
        
        // Connections would be loaded similarly
    }
}