package com.zamaz.workflow.api.websocket;

import com.zamaz.workflow.api.graphql.WorkflowGraphQLController.WorkflowExecutionEvent;
import com.zamaz.workflow.api.graphql.TelemetryGraphQLController.NodeStatusEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class WorkflowEventPublisher {
    private final Map<String, Sinks.Many<WorkflowExecutionEvent>> workflowExecutionSinks = new ConcurrentHashMap<>();
    private final Map<String, Sinks.Many<NodeStatusEvent>> nodeStatusSinks = new ConcurrentHashMap<>();
    
    public Flux<WorkflowExecutionEvent> subscribeToWorkflowExecution(String workflowId) {
        log.info("Creating subscription for workflow execution: {}", workflowId);
        
        Sinks.Many<WorkflowExecutionEvent> sink = workflowExecutionSinks.computeIfAbsent(
                workflowId,
                k -> Sinks.many().multicast().onBackpressureBuffer()
        );
        
        return sink.asFlux()
                .doOnSubscribe(sub -> log.debug("Client subscribed to workflow: {}", workflowId))
                .doOnCancel(() -> {
                    log.debug("Client unsubscribed from workflow: {}", workflowId);
                    // Clean up if no more subscribers
                    if (sink.currentSubscriberCount() == 0) {
                        workflowExecutionSinks.remove(workflowId);
                    }
                });
    }
    
    public Flux<NodeStatusEvent> subscribeToNodeStatus(String workflowId) {
        log.info("Creating subscription for node status: {}", workflowId);
        
        Sinks.Many<NodeStatusEvent> sink = nodeStatusSinks.computeIfAbsent(
                workflowId,
                k -> Sinks.many().multicast().onBackpressureBuffer()
        );
        
        return sink.asFlux()
                .doOnSubscribe(sub -> log.debug("Client subscribed to node status: {}", workflowId))
                .doOnCancel(() -> {
                    log.debug("Client unsubscribed from node status: {}", workflowId);
                    if (sink.currentSubscriberCount() == 0) {
                        nodeStatusSinks.remove(workflowId);
                    }
                });
    }
    
    public void publishWorkflowExecutionEvent(String workflowId, String nodeId, String status, Object data) {
        Sinks.Many<WorkflowExecutionEvent> sink = workflowExecutionSinks.get(workflowId);
        
        if (sink != null) {
            WorkflowExecutionEvent event = WorkflowExecutionEvent.builder()
                    .workflowId(workflowId)
                    .nodeId(nodeId)
                    .status(status)
                    .timestamp(Instant.now().toString())
                    .data(data)
                    .build();
            
            sink.tryEmitNext(event);
            log.debug("Published workflow execution event for workflow: {}, node: {}", workflowId, nodeId);
        }
    }
    
    public void publishNodeStatusEvent(String workflowId, String nodeId, String status) {
        Sinks.Many<NodeStatusEvent> sink = nodeStatusSinks.get(workflowId);
        
        if (sink != null) {
            NodeStatusEvent event = NodeStatusEvent.builder()
                    .nodeId(nodeId)
                    .status(status)
                    .timestamp(Instant.now().toString())
                    .build();
            
            sink.tryEmitNext(event);
            log.debug("Published node status event for workflow: {}, node: {}", workflowId, nodeId);
        }
    }
    
    public void completeWorkflowExecution(String workflowId) {
        Sinks.Many<WorkflowExecutionEvent> sink = workflowExecutionSinks.remove(workflowId);
        if (sink != null) {
            sink.tryEmitComplete();
            log.info("Completed workflow execution stream for: {}", workflowId);
        }
    }
}