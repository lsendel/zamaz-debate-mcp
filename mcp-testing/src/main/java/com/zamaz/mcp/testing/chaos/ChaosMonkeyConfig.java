package com.zamaz.mcp.testing.chaos;

import de.codecentric.spring.boot.chaos.monkey.configuration.AssaultProperties;
import de.codecentric.spring.boot.chaos.monkey.configuration.ChaosMonkeyProperties;
import de.codecentric.spring.boot.chaos.monkey.configuration.WatcherProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Chaos Monkey configuration for resilience testing
 */
@Configuration
@Profile({"chaos", "test"})
@ConditionalOnProperty(name = "chaos.monkey.enabled", havingValue = "true")
@Slf4j
public class ChaosMonkeyConfig {
    
    /**
     * Configure Chaos Monkey properties
     */
    @Bean
    @Primary
    public ChaosMonkeyProperties chaosMonkeyProperties() {
        ChaosMonkeyProperties properties = new ChaosMonkeyProperties();
        properties.setEnabled(true);
        
        log.warn("CHAOS MONKEY IS ENABLED - This should only be used in test environments!");
        
        return properties;
    }
    
    /**
     * Configure assault properties - what chaos to introduce
     */
    @Bean
    public AssaultProperties assaultProperties() {
        AssaultProperties properties = new AssaultProperties();
        
        // Latency assault
        properties.setLatencyActive(true);
        properties.setLatencyRangeStart(2000); // 2 seconds
        properties.setLatencyRangeEnd(5000);   // 5 seconds
        
        // Exception assault
        properties.setExceptionsActive(true);
        properties.setException(new RuntimeException("Chaos Monkey Exception"));
        
        // Kill application assault (disabled by default)
        properties.setKillApplicationActive(false);
        
        // Memory assault
        properties.setMemoryActive(true);
        properties.setMemoryMillisecondsHoldFilledMemory(5000);
        properties.setMemoryMillisecondsWaitNextIncrease(1000);
        properties.setMemoryFillIncrementFraction(0.15);
        properties.setMemoryFillTargetFraction(0.25);
        
        // CPU assault
        properties.setCpuActive(true);
        properties.setCpuMillisecondsHoldLoad(5000);
        properties.setCpuLoadTargetFraction(0.8);
        
        // Set assault level (1-10, where 10 is most aggressive)
        properties.setLevel(5);
        
        return properties;
    }
    
    /**
     * Configure watcher properties - what to watch
     */
    @Bean
    public WatcherProperties watcherProperties() {
        WatcherProperties properties = new WatcherProperties();
        
        // Watch specific components
        properties.setController(true);
        properties.setService(true);
        properties.setRepository(true);
        properties.setRestController(true);
        properties.setComponent(true);
        
        // Exclude critical components from chaos
        properties.setExcludeClasses(new String[]{
            "com.zamaz.mcp.*.security.*",
            "com.zamaz.mcp.*.config.*",
            "com.zamaz.mcp.*.health.*"
        });
        
        return properties;
    }
    
    /**
     * Custom assault configuration for specific services
     */
    @Bean
    public ServiceSpecificChaosConfig serviceSpecificChaosConfig() {
        return new ServiceSpecificChaosConfig();
    }
    
    /**
     * Service-specific chaos configurations
     */
    public static class ServiceSpecificChaosConfig {
        
        /**
         * LLM Service chaos - higher latency tolerance
         */
        public AssaultProperties llmServiceAssault() {
            AssaultProperties properties = new AssaultProperties();
            properties.setLatencyActive(true);
            properties.setLatencyRangeStart(10000); // 10 seconds
            properties.setLatencyRangeEnd(30000);   // 30 seconds
            properties.setLevel(3); // Less aggressive
            return properties;
        }
        
        /**
         * Gateway chaos - focus on resilience
         */
        public AssaultProperties gatewayAssault() {
            AssaultProperties properties = new AssaultProperties();
            properties.setLatencyActive(true);
            properties.setLatencyRangeStart(100);   // 100ms
            properties.setLatencyRangeEnd(1000);    // 1 second
            properties.setExceptionsActive(true);
            properties.setLevel(7); // More aggressive
            return properties;
        }
        
        /**
         * Database chaos - connection issues
         */
        public AssaultProperties databaseAssault() {
            AssaultProperties properties = new AssaultProperties();
            properties.setExceptionsActive(true);
            properties.setException(new java.sql.SQLException("Chaos Monkey: Database connection lost"));
            properties.setLevel(5);
            return properties;
        }
    }
}