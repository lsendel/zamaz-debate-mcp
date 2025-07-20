package com.zamaz.workflow.domain.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class WorkflowNode {
    @NonNull
    @Builder.Default
    private final String id = UUID.randomUUID().toString();
    
    @NonNull
    private final WorkflowNodeType type;
    
    @NonNull
    private final String name;
    
    private final String description;
    
    @NonNull
    @Builder.Default
    private final Map<String, Object> configuration = new HashMap<>();
    
    @NonNull
    @Builder.Default
    private final NodePosition position = new NodePosition(0, 0);
    
    public static WorkflowNode createStartNode(String name) {
        return WorkflowNode.builder()
                .type(WorkflowNodeType.START)
                .name(name)
                .build();
    }
    
    public static WorkflowNode createEndNode(String name) {
        return WorkflowNode.builder()
                .type(WorkflowNodeType.END)
                .name(name)
                .build();
    }
    
    public static WorkflowNode createTaskNode(String name, Map<String, Object> configuration) {
        return WorkflowNode.builder()
                .type(WorkflowNodeType.TASK)
                .name(name)
                .configuration(configuration)
                .build();
    }
    
    public static WorkflowNode createDecisionNode(String name, Map<String, Object> conditions) {
        Map<String, Object> config = new HashMap<>();
        config.put("conditions", conditions);
        
        return WorkflowNode.builder()
                .type(WorkflowNodeType.DECISION)
                .name(name)
                .configuration(config)
                .build();
    }
    
    public void updatePosition(double x, double y) {
        position.update(x, y);
    }
    
    public void updateConfiguration(String key, Object value) {
        configuration.put(key, value);
    }
    
    @Getter
    public static class NodePosition {
        private double x;
        private double y;
        
        public NodePosition(double x, double y) {
            this.x = x;
            this.y = y;
        }
        
        public void update(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}