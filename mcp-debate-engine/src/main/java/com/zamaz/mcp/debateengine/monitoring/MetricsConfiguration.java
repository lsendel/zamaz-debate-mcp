package com.zamaz.mcp.debateengine.monitoring;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Comprehensive metrics configuration for Prometheus monitoring.
 * Tracks business metrics, performance metrics, and system health.
 */
@Configuration
@EnableAspectJAutoProxy
public class MetricsConfiguration {

    /**
     * Customize meter registry with common tags
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
                .commonTags(
                    "application", "debate-engine",
                    "environment", System.getProperty("spring.profiles.active", "default")
                );
    }

    /**
     * Enable @Timed annotation support
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    /**
     * JVM metrics
     */
    @Bean
    public JvmGcMetrics jvmGcMetrics() {
        return new JvmGcMetrics();
    }

    @Bean
    public JvmMemoryMetrics jvmMemoryMetrics() {
        return new JvmMemoryMetrics();
    }

    @Bean
    public JvmThreadMetrics jvmThreadMetrics() {
        return new JvmThreadMetrics();
    }

    /**
     * System metrics
     */
    @Bean
    public ProcessorMetrics processorMetrics() {
        return new ProcessorMetrics();
    }

    @Bean
    public UptimeMetrics uptimeMetrics() {
        return new UptimeMetrics();
    }

    /**
     * Business metrics helper
     */
    @Bean
    public BusinessMetrics businessMetrics(MeterRegistry registry) {
        return new BusinessMetrics(registry);
    }
}

/**
 * Helper class for business metrics
 */
class BusinessMetrics {
    
    private final Counter debateCreatedCounter;
    private final Counter debateCompletedCounter;
    private final Counter debateCancelledCounter;
    private final Timer debateDurationTimer;
    private final Counter messageAddedCounter;
    private final Counter contextCreatedCounter;
    private final Counter participantJoinedCounter;
    private final Counter aiResponseCounter;
    private final Timer aiResponseTimer;
    
    public BusinessMetrics(MeterRegistry registry) {
        // Debate metrics
        this.debateCreatedCounter = Counter.builder("debates.created")
                .description("Number of debates created")
                .register(registry);
                
        this.debateCompletedCounter = Counter.builder("debates.completed")
                .description("Number of debates completed")
                .register(registry);
                
        this.debateCancelledCounter = Counter.builder("debates.cancelled")
                .description("Number of debates cancelled")
                .register(registry);
                
        this.debateDurationTimer = Timer.builder("debates.duration")
                .description("Duration of completed debates")
                .register(registry);
        
        // Context metrics
        this.messageAddedCounter = Counter.builder("messages.added")
                .description("Number of messages added to contexts")
                .register(registry);
                
        this.contextCreatedCounter = Counter.builder("contexts.created")
                .description("Number of contexts created")
                .register(registry);
        
        // Participant metrics
        this.participantJoinedCounter = Counter.builder("participants.joined")
                .description("Number of participants joined debates")
                .tag("type", "all")
                .register(registry);
        
        // AI metrics
        this.aiResponseCounter = Counter.builder("ai.responses")
                .description("Number of AI responses generated")
                .register(registry);
                
        this.aiResponseTimer = Timer.builder("ai.response.duration")
                .description("Time taken to generate AI responses")
                .register(registry);
    }
    
    // Debate metrics
    public void recordDebateCreated() {
        debateCreatedCounter.increment();
    }
    
    public void recordDebateCompleted(long durationMillis) {
        debateCompletedCounter.increment();
        debateDurationTimer.record(java.time.Duration.ofMillis(durationMillis));
    }
    
    public void recordDebateCancelled() {
        debateCancelledCounter.increment();
    }
    
    // Context metrics
    public void recordMessageAdded() {
        messageAddedCounter.increment();
    }
    
    public void recordContextCreated() {
        contextCreatedCounter.increment();
    }
    
    // Participant metrics
    public void recordParticipantJoined(String participantType) {
        Counter.builder("participants.joined")
                .tag("type", participantType)
                .register(participantJoinedCounter.getId().getMeterRegistry())
                .increment();
    }
    
    // AI metrics
    public void recordAiResponse(String provider, long durationMillis) {
        Counter.builder("ai.responses")
                .tag("provider", provider)
                .register(aiResponseCounter.getId().getMeterRegistry())
                .increment();
                
        Timer.builder("ai.response.duration")
                .tag("provider", provider)
                .register(aiResponseTimer.getId().getMeterRegistry())
                .record(java.time.Duration.ofMillis(durationMillis));
    }
}