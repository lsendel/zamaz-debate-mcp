package com.zamaz.workflow.application.dto;

import com.zamaz.workflow.domain.entity.Workflow;
import com.zamaz.workflow.domain.entity.WorkflowNode;
import com.zamaz.workflow.domain.entity.WorkflowConnection;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class WorkflowResponse {
    private String id;
    private String name;
    private String description;
    private String status;
    private String organizationId;
    private List<WorkflowNodeResponse> nodes;
    private List<WorkflowConnectionResponse> connections;
    private Instant createdAt;
    private Instant updatedAt;
    
    public static WorkflowResponse from(Workflow workflow) {
        return WorkflowResponse.builder()
                .id(workflow.getId().getValue())
                .name(workflow.getName())
                .description(workflow.getDescription())
                .status(workflow.getStatus().name())
                .organizationId(workflow.getOrganizationId().getValue())
                .nodes(workflow.getNodes().stream()
                        .map(WorkflowNodeResponse::from)
                        .collect(Collectors.toList()))
                .connections(workflow.getConnections().stream()
                        .map(WorkflowConnectionResponse::from)
                        .collect(Collectors.toList()))
                .createdAt(workflow.getCreatedAt())
                .updatedAt(workflow.getUpdatedAt())
                .build();
    }
    
    @Data
    @Builder
    public static class WorkflowNodeResponse {
        private String id;
        private String type;
        private String name;
        private String description;
        private Object configuration;
        private Position position;
        
        public static WorkflowNodeResponse from(WorkflowNode node) {
            return WorkflowNodeResponse.builder()
                    .id(node.getId())
                    .type(node.getType().name())
                    .name(node.getName())
                    .description(node.getDescription())
                    .configuration(node.getConfiguration())
                    .position(Position.builder()
                            .x(node.getPosition().getX())
                            .y(node.getPosition().getY())
                            .build())
                    .build();
        }
        
        @Data
        @Builder
        public static class Position {
            private double x;
            private double y;
        }
    }
    
    @Data
    @Builder
    public static class WorkflowConnectionResponse {
        private String id;
        private String sourceNodeId;
        private String targetNodeId;
        private String label;
        private int order;
        
        public static WorkflowConnectionResponse from(WorkflowConnection connection) {
            return WorkflowConnectionResponse.builder()
                    .id(connection.getId())
                    .sourceNodeId(connection.getSourceNodeId())
                    .targetNodeId(connection.getTargetNodeId())
                    .label(connection.getLabel())
                    .order(connection.getOrder())
                    .build();
        }
    }
}