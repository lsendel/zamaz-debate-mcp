package com.zamaz.mcp.configserver.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.bus.jackson.RemoteApplicationEventScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

/**
 * Configuration for Spring Cloud Bus with RabbitMQ.
 * Enables broadcasting of configuration refresh events across all service instances.
 */
@Configuration
@ConditionalOnProperty(name = "spring.cloud.bus.enabled", havingValue = "true", matchIfMissing = true)
@RemoteApplicationEventScan
public class BusConfiguration {

    private static final String EXCHANGE_NAME = "springCloudBus";
    private static final String QUEUE_NAME = "springCloudBus.anonymous";
    private static final String ROUTING_KEY = "#";

    /**
     * Declares the Spring Cloud Bus exchange.
     */
    @Bean
    public TopicExchange busExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    /**
     * Declares the Spring Cloud Bus queue.
     */
    @Bean
    public Queue busQueue() {
        return new AnonymousQueue();
    }

    /**
     * Binds the queue to the exchange.
     */
    @Bean
    public Binding busBinding(Queue busQueue, TopicExchange busExchange) {
        return BindingBuilder
            .bind(busQueue)
            .to(busExchange)
            .with(ROUTING_KEY);
    }

    /**
     * Configures JSON message converter for bus events.
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Configures RabbitTemplate with JSON converter.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }

    /**
     * Dead letter exchange for failed messages.
     */
    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange("springCloudBus.dlx", true, false);
    }

    /**
     * Dead letter queue for failed messages.
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder
            .durable("springCloudBus.dlq")
            .build();
    }

    /**
     * Binding for dead letter queue.
     */
    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, TopicExchange deadLetterExchange) {
        return BindingBuilder
            .bind(deadLetterQueue)
            .to(deadLetterExchange)
            .with("#");
    }
}