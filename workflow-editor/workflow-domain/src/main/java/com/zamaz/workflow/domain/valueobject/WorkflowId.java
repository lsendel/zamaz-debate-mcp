package com.zamaz.workflow.domain.valueobject;

import lombok.NonNull;
import lombok.Value;

import java.util.UUID;

@Value
public class WorkflowId {
    @NonNull
    String value;
    
    private WorkflowId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("WorkflowId cannot be null or empty");
        }
        this.value = value;
    }
    
    public static WorkflowId of(String value) {
        return new WorkflowId(value);
    }
    
    public static WorkflowId generate() {
        return new WorkflowId("wf-" + UUID.randomUUID().toString());
    }
    
    @Override
    public String toString() {
        return value;
    }
}