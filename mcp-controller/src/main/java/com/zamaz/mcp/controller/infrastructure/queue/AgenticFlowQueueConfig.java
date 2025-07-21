package com.zamaz.mcp.controller.infrastructure.queue;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ configuration for agentic flow queue-based processing.
 */
@Configuration
public class AgenticFlowQueueConfig {
    
    @Value("${agentic-flow.queue.exchange:agentic-flow-exchange}")
    private String exchangeName;
    
    @Value("${agentic-flow.queue.execution:agentic-flow-execution}")
    private String executionQueueName;
    
    @Value("${agentic-flow.queue.priority:agentic-flow-priority}")
    private String priorityQueueName;
    
    @Value("${agentic-flow.queue.analytics:agentic-flow-analytics}")
    private String analyticsQueueName;
    
    @Value("${agentic-flow.queue.dlq:agentic-flow-dlq}")
    private String deadLetterQueueName;
    
    // Exchange configuration
    @Bean
    public TopicExchange agenticFlowExchange() {
        return new TopicExchange(exchangeName, true, false);
    }
    
    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(exchangeName + ".dlx", true, false);
    }
    
    // Queue configurations
    @Bean
    public Queue executionQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", 300000); // 5 minutes
        args.put("x-max-length", 1000);
        args.put("x-dead-letter-exchange", exchangeName + ".dlx");
        args.put("x-dead-letter-routing-key", "dlq");
        
        return new Queue(executionQueueName, true, false, false, args);
    }
    
    @Bean
    public Queue priorityQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-max-priority", 10);
        args.put("x-message-ttl", 180000); // 3 minutes
        args.put("x-dead-letter-exchange", exchangeName + ".dlx");
        args.put("x-dead-letter-routing-key", "dlq");
        
        return new Queue(priorityQueueName, true, false, false, args);
    }
    
    @Bean
    public Queue analyticsQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", 600000); // 10 minutes
        args.put("x-max-length", 5000);
        
        return new Queue(analyticsQueueName, true, false, false, args);
    }
    
    @Bean
    public Queue deadLetterQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", 86400000); // 24 hours
        args.put("x-max-length", 1000);
        
        return new Queue(deadLetterQueueName, true, false, false, args);
    }
    
    // Bindings
    @Bean
    public Binding executionBinding() {
        return BindingBuilder
            .bind(executionQueue())
            .to(agenticFlowExchange())
            .with("flow.execute.#");
    }
    
    @Bean
    public Binding priorityBinding() {
        return BindingBuilder
            .bind(priorityQueue())
            .to(agenticFlowExchange())
            .with("flow.priority.#");
    }
    
    @Bean
    public Binding analyticsBinding() {
        return BindingBuilder
            .bind(analyticsQueue())
            .to(agenticFlowExchange())
            .with("flow.analytics.#");
    }
    
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder
            .bind(deadLetterQueue())
            .to(deadLetterExchange())
            .with("dlq");
    }
    
    // Message converter
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    // RabbitTemplate configuration
    @Bean
    public RabbitTemplate agenticFlowRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        template.setExchange(exchangeName);
        
        // Retry configuration
        template.setRetryTemplate(retryTemplate());
        
        return template;
    }
    
    private org.springframework.retry.support.RetryTemplate retryTemplate() {
        org.springframework.retry.support.RetryTemplate retryTemplate = 
            new org.springframework.retry.support.RetryTemplate();
        
        retryTemplate.setRetryPolicy(
            new org.springframework.retry.policy.SimpleRetryPolicy(3));
        
        retryTemplate.setBackOffPolicy(
            new org.springframework.retry.backoff.ExponentialBackOffPolicy());
        
        return retryTemplate;
    }
}