package com.zamaz.workflow.domain.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class WorkflowConnection {
    @NonNull
    @Builder.Default
    private final String id = UUID.randomUUID().toString();
    
    @NonNull
    private final String sourceNodeId;
    
    @NonNull
    private final String targetNodeId;
    
    private final String label;
    
    @Builder.Default
    private final int order = 0;
    
    @NonNull
    @Builder.Default
    private final Map<String, Object> metadata = new HashMap<>();
    
    public static WorkflowConnection connect(String sourceNodeId, String targetNodeId) {
        return WorkflowConnection.builder()
                .sourceNodeId(sourceNodeId)
                .targetNodeId(targetNodeId)
                .build();
    }
    
    public static WorkflowConnection connectWithLabel(String sourceNodeId, String targetNodeId, String label) {
        return WorkflowConnection.builder()
                .sourceNodeId(sourceNodeId)
                .targetNodeId(targetNodeId)
                .label(label)
                .build();
    }
    
    public void addMetadata(String key, Object value) {
        metadata.put(key, value);
    }
}