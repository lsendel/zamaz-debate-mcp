package com.example.workflow.infrastructure.neo4j;

import com.example.workflow.domain.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mapper for converting between domain objects and Neo4j entities
 * Uses MapStruct for efficient mapping with custom conversion methods
 */
@Mapper(componentModel = "spring")
@Component
public interface WorkflowEntityMapper {
    
    // Workflow mappings
    @Mapping(source = "id.value", target = "id")
    @Mapping(source = "status", target = "status", qualifiedByName = "workflowStatusToString")
    @Mapping(source = "nodes", target = "nodes", qualifiedByName = "nodesToEntities")
    @Mapping(source = "edges", target = "edges", qualifiedByName = "edgesToEntities")
    WorkflowEntity toEntity(Workflow workflow);
    
    @Mapping(source = "id", target = "id", qualifiedByName = "stringToWorkflowId")
    @Mapping(source = "status", target = "status", qualifiedByName = "stringToWorkflowStatus")
    @Mapping(source = "nodes", target = "nodes", qualifiedByName = "entitiesToNodes")
    @Mapping(source = "edges", target = "edges", qualifiedByName = "entitiesToEdges")
    Workflow toDomain(WorkflowEntity entity);
    
    List<WorkflowEntity> toEntities(List<Workflow> workflows);
    List<Workflow> toDomains(List<WorkflowEntity> entities);
    
    // WorkflowNode mappings
    @Mapping(source = "id.value", target = "id")
    @Mapping(source = "type", target = "type", qualifiedByName = "nodeTypeToString")
    @Mapping(source = "position.x", target = "positionX")
    @Mapping(source = "position.y", target = "positionY")
    WorkflowNodeEntity toNodeEntity(WorkflowNode node);
    
    @Mapping(source = "id", target = "id", qualifiedByName = "stringToNodeId")
    @Mapping(source = "type", target = "type", qualifiedByName = "stringToNodeType")
    @Mapping(source = "positionX", target = "position.x")
    @Mapping(source = "positionY", target = "position.y")
    WorkflowNode toNodeDomain(WorkflowNodeEntity entity);
    
    // WorkflowEdge mappings
    @Mapping(source = "id.value", target = "id")
    @Mapping(source = "type", target = "type", qualifiedByName = "edgeTypeToString")
    WorkflowEdgeEntity toEdgeEntity(WorkflowEdge edge);
    
    @Mapping(source = "id", target = "id", qualifiedByName = "stringToEdgeId")
    @Mapping(source = "type", target = "type", qualifiedByName = "stringToEdgeType")
    WorkflowEdge toEdgeDomain(WorkflowEdgeEntity entity);
    
    // Custom conversion methods
    @Named("workflowStatusToString")
    default String workflowStatusToString(WorkflowStatus status) {
        return status != null ? status.name() : null;
    }
    
    @Named("stringToWorkflowStatus")
    default WorkflowStatus stringToWorkflowStatus(String status) {
        return status != null ? WorkflowStatus.valueOf(status) : null;
    }
    
    @Named("nodeTypeToString")
    default String nodeTypeToString(NodeType type) {
        return type != null ? type.name() : null;
    }
    
    @Named("stringToNodeType")
    default NodeType stringToNodeType(String type) {
        return type != null ? NodeType.valueOf(type) : null;
    }
    
    @Named("edgeTypeToString")
    default String edgeTypeToString(EdgeType type) {
        return type != null ? type.name() : null;
    }
    
    @Named("stringToEdgeType")
    default EdgeType stringToEdgeType(String type) {
        return type != null ? EdgeType.valueOf(type) : null;
    }
    
    @Named("stringToWorkflowId")
    default WorkflowId stringToWorkflowId(String id) {
        return id != null ? new WorkflowId(id) : null;
    }
    
    @Named("stringToNodeId")
    default NodeId stringToNodeId(String id) {
        return id != null ? new NodeId(id) : null;
    }
    
    @Named("stringToEdgeId")
    default EdgeId stringToEdgeId(String id) {
        return id != null ? new EdgeId(id) : null;
    }
    
    @Named("nodesToEntities")
    default List<WorkflowNodeEntity> nodesToEntities(List<WorkflowNode> nodes) {
        return nodes != null ? nodes.stream()
            .map(this::toNodeEntity)
            .collect(Collectors.toList()) : null;
    }
    
    @Named("entitiesToNodes")
    default List<WorkflowNode> entitiesToNodes(List<WorkflowNodeEntity> entities) {
        return entities != null ? entities.stream()
            .map(this::toNodeDomain)
            .collect(Collectors.toList()) : null;
    }
    
    @Named("edgesToEntities")
    default List<WorkflowEdgeEntity> edgesToEntities(List<WorkflowEdge> edges) {
        return edges != null ? edges.stream()
            .map(this::toEdgeEntity)
            .collect(Collectors.toList()) : null;
    }
    
    @Named("entitiesToEdges")
    default List<WorkflowEdge> entitiesToEdges(List<WorkflowEdgeEntity> entities) {
        return entities != null ? entities.stream()
            .map(this::toEdgeDomain)
            .collect(Collectors.toList()) : null;
    }
}