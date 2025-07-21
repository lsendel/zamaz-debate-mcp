package com.zamaz.mcp.controller.infrastructure.queue;

import com.zamaz.mcp.controller.domain.FlowExecutionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Message producer for agentic flow queue operations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AgenticFlowMessageProducer {
    
    private final RabbitTemplate rabbitTemplate;
    
    /**
     * Sends a flow execution request to the standard queue.
     */
    public void sendExecutionRequest(FlowExecutionRequest request) {
        sendExecutionRequest(request, 5); // Default priority
    }
    
    /**
     * Sends a flow execution request with priority.
     */
    public void sendExecutionRequest(FlowExecutionRequest request, int priority) {
        try {
            String routingKey = priority >= 8 ? "flow.priority.high" : "flow.execute.normal";
            
            Message message = MessageBuilder
                .withBody(rabbitTemplate.getMessageConverter().toMessage(request, new MessageProperties()).getBody())
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setMessageId(UUID.randomUUID().toString())
                .setPriority(priority)
                .setHeader("flowType", request.getFlowId())
                .setHeader("organizationId", request.getContext().getOrganizationId())
                .setHeader("timestamp", System.currentTimeMillis())
                .build();
            
            rabbitTemplate.send(routingKey, message);
            
            log.debug("Sent flow execution request {} with priority {}", request.getFlowId(), priority);
        } catch (Exception e) {
            log.error("Failed to send execution request", e);
            throw new RuntimeException("Failed to queue execution request", e);
        }
    }
    
    /**
     * Sends analytics data to the analytics queue.
     */
    public void sendAnalyticsData(Object analyticsData) {
        try {
            rabbitTemplate.convertAndSend("flow.analytics.record", analyticsData);
            log.debug("Sent analytics data to queue");
        } catch (Exception e) {
            log.error("Failed to send analytics data", e);
            // Don't throw - analytics shouldn't break main flow
        }
    }
    
    /**
     * Sends a batch of execution requests.
     */
    public void sendBatchExecutionRequests(List<FlowExecutionRequest> requests) {
        for (FlowExecutionRequest request : requests) {
            sendExecutionRequest(request);
        }
    }
}