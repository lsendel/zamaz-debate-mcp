package com.zamaz.mcp.controller.infrastructure.queue;

import com.zamaz.mcp.controller.application.OptimizedAgenticFlowApplicationService;
import com.zamaz.mcp.controller.domain.FlowExecutionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Message consumer for processing agentic flow requests from queues.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AgenticFlowMessageConsumer {
    
    private final OptimizedAgenticFlowApplicationService flowService;
    private final RabbitTemplate rabbitTemplate;
    
    @RabbitListener(queues = "${agentic-flow.queue.execution:agentic-flow-execution}")
    public void processExecutionRequest(FlowExecutionRequest request) {
        log.info("Processing flow execution request: {}", request.getFlowId());
        
        try {
            CompletableFuture.runAsync(() -> {
                flowService.executeFlowAsync(
                    request.getFlowId(),
                    request.getPrompt(),
                    request.getContext()
                ).thenAccept(result -> {
                    log.info("Flow {} executed successfully", request.getFlowId());
                }).exceptionally(throwable -> {
                    log.error("Flow {} execution failed: {}", request.getFlowId(), throwable.getMessage());
                    return null;
                });
            });
        } catch (Exception e) {
            log.error("Failed to process execution request", e);
            throw e; // Let RabbitMQ handle retry/DLQ
        }
    }
    
    @RabbitListener(
        queues = "${agentic-flow.queue.priority:agentic-flow-priority}",
        priority = "10"
    )
    public void processPriorityExecutionRequest(FlowExecutionRequest request) {
        log.info("Processing priority flow execution request: {}", request.getFlowId());
        
        try {
            // Process synchronously for priority requests
            flowService.executeFlowAsync(
                request.getFlowId(),
                request.getPrompt(),
                request.getContext()
            ).join(); // Wait for completion
            
            log.info("Priority flow {} executed successfully", request.getFlowId());
        } catch (Exception e) {
            log.error("Failed to process priority execution request", e);
            throw e;
        }
    }
    
    @RabbitListener(queues = "${agentic-flow.queue.analytics:agentic-flow-analytics}")
    public void processAnalyticsData(Object analyticsData) {
        log.debug("Processing analytics data");
        
        try {
            // Process analytics data asynchronously
            CompletableFuture.runAsync(() -> {
                // Analytics processing logic here
                log.debug("Analytics data processed");
            });
        } catch (Exception e) {
            log.error("Failed to process analytics data", e);
            // Don't rethrow - analytics shouldn't break anything
        }
    }
}