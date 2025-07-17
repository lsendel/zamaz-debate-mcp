package com.zamaz.mcp.common.eventsourcing;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for event sourcing components
 */
@Configuration
public class EventSourcingConfiguration {
    
    /**
     * Configure the event store implementation
     */
    @Bean
    @ConditionalOnMissingBean
    public EventStore eventStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        return new PostgreSQLEventStore(jdbcTemplate, objectMapper);
    }
    
    /**
     * Configure executor for async event processing
     */
    @Bean(name = "eventSourcingExecutor")
    public Executor eventSourcingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("EventSourcing-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
    
    /**
     * Configure event sourcing metrics
     */
    @Bean
    @ConditionalOnMissingBean
    public EventSourcingMetrics eventSourcingMetrics(MeterRegistry meterRegistry) {
        return new EventSourcingMetrics(meterRegistry);
    }
    
    /**
     * Configure event sourcing health indicator
     */
    @Bean
    @ConditionalOnMissingBean
    public EventSourcingHealthIndicator eventSourcingHealthIndicator(EventStore eventStore) {
        return new EventSourcingHealthIndicator(eventStore);
    }
}