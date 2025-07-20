package com.zamaz.workflow.infrastructure.adapter.mapper;

import com.zamaz.workflow.domain.entity.Workflow;
import com.zamaz.workflow.domain.entity.WorkflowConnection;
import com.zamaz.workflow.domain.entity.WorkflowNode;
import com.zamaz.workflow.domain.entity.WorkflowNodeType;
import com.zamaz.workflow.domain.valueobject.OrganizationId;
import com.zamaz.workflow.domain.valueobject.WorkflowId;
import com.zamaz.workflow.domain.valueobject.WorkflowStatus;
import com.zamaz.workflow.infrastructure.entity.WorkflowConnectionEntity;
import com.zamaz.workflow.infrastructure.entity.WorkflowEntity;
import com.zamaz.workflow.infrastructure.entity.WorkflowNodeEntity;
import org.mapstruct.*;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WorkflowMapper {
    
    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "organizationId", source = "organizationId.value")
    @Mapping(target = "status", expression = "java(workflow.getStatus().name())")
    WorkflowEntity toEntity(Workflow workflow);
    
    @Named("toDomain")
    default Workflow toDomain(WorkflowEntity entity) {
        if (entity == null) {
            return null;
        }
        
        Workflow.WorkflowBuilder builder = Workflow.builder()
                .id(WorkflowId.of(entity.getId()))
                .name(entity.getName())
                .description(entity.getDescription())
                .status(WorkflowStatus.valueOf(entity.getStatus()))
                .organizationId(OrganizationId.of(entity.getOrganizationId()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt());
        
        // Map nodes
        if (entity.getNodes() != null) {
            List<WorkflowNode> nodes = entity.getNodes().stream()
                    .map(this::nodeEntityToDomain)
                    .collect(Collectors.toList());
            builder.nodes(nodes);
        }
        
        // Note: Connections would need to be loaded separately in Neo4j
        // This is a simplified mapping
        
        return builder.build();
    }
    
    default WorkflowNodeEntity nodeDomainToEntity(WorkflowNode node) {
        if (node == null) {
            return null;
        }
        
        return WorkflowNodeEntity.builder()
                .id(node.getId())
                .type(node.getType().name())
                .name(node.getName())
                .description(node.getDescription())
                .configuration(node.getConfiguration())
                .positionX(node.getPosition().getX())
                .positionY(node.getPosition().getY())
                .build();
    }
    
    default WorkflowNode nodeEntityToDomain(WorkflowNodeEntity entity) {
        if (entity == null) {
            return null;
        }
        
        WorkflowNode node = WorkflowNode.builder()
                .id(entity.getId())
                .type(WorkflowNodeType.valueOf(entity.getType()))
                .name(entity.getName())
                .description(entity.getDescription())
                .configuration(entity.getConfiguration())
                .build();
        
        node.updatePosition(entity.getPositionX(), entity.getPositionY());
        
        return node;
    }
    
    default WorkflowConnectionEntity connectionDomainToEntity(WorkflowConnection connection, WorkflowNodeEntity targetNode) {
        if (connection == null) {
            return null;
        }
        
        return WorkflowConnectionEntity.builder()
                .connectionId(connection.getId())
                .label(connection.getLabel())
                .order(connection.getOrder())
                .metadata(connection.getMetadata())
                .targetNode(targetNode)
                .build();
    }
    
    default WorkflowConnection connectionEntityToDomain(String sourceNodeId, WorkflowConnectionEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return WorkflowConnection.builder()
                .id(entity.getConnectionId())
                .sourceNodeId(sourceNodeId)
                .targetNodeId(entity.getTargetNode().getId())
                .label(entity.getLabel())
                .order(entity.getOrder())
                .metadata(entity.getMetadata())
                .build();
    }
}