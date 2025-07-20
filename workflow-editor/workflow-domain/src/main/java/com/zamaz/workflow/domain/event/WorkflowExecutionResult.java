package com.zamaz.workflow.domain.event;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Getter
@Builder
public class WorkflowExecutionResult {
    @NonNull
    private final String nodeId;
    
    @NonNull
    private final ExecutionStatus status;
    
    @NonNull
    @Builder.Default
    private final Map<String, Object> outputs = new HashMap<>();
    
    @NonNull
    private final Instant startTime;
    
    @NonNull
    private final Instant endTime;
    
    private final String errorMessage;
    
    private final String nextNodeId;
    
    public Duration getExecutionDuration() {
        return Duration.between(startTime, endTime);
    }
    
    public boolean isSuccess() {
        return status == ExecutionStatus.SUCCESS;
    }
    
    public enum ExecutionStatus {
        SUCCESS,
        FAILED,
        SKIPPED,
        TIMEOUT
    }
}