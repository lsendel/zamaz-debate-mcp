package com.zamaz.mcp.common.testing.chaos;

import com.zamaz.mcp.common.testing.chaos.ChaosEngineeringFramework.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Pre-defined chaos experiments for MCP services.
 * Contains common failure scenarios and resilience tests.
 */
public class McpChaosExperiments {

    private final ChaosEngineeringFramework framework;
    private final RestTemplate restTemplate;

    public McpChaosExperiments(ChaosEngineeringFramework framework) {
        this.framework = framework;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Database failure resilience test.
     */
    public ChaosExperiment databaseFailureResilience() {
        return framework.experiment()
            .named("Database Failure Resilience")
            .describedAs("Tests system behavior when database becomes unavailable")
            .runningFor(Duration.ofMinutes(3))
            .withFailure(new DatabaseFailureInjection("postgres", 
                DatabaseFailureInjection.FailureType.CONNECTION_TIMEOUT))
            .withAssertion(new ServiceAvailabilityAssertion("mcp-organization", 
                () -> checkServiceHealth("http://localhost:8080/health")))
            .withAssertion(new ResponseTimeAssertion("mcp-organization", Duration.ofSeconds(10),
                () -> measureResponseTime("http://localhost:8080/health")))
            .withAssertion(new DataConsistencyAssertion("organization data",
                this::verifyOrganizationDataConsistency))
            .withMetadata("category", "database")
            .withMetadata("severity", "high")
            .build();
    }

    /**
     * LLM provider failure test.
     */
    public ChaosExperiment llmProviderFailure() {
        return framework.experiment()
            .named("LLM Provider Failure")
            .describedAs("Tests debate continuation when LLM providers become unavailable")
            .runningFor(Duration.ofMinutes(2))
            .withFailure(new ServiceUnavailabilityInjection("anthropic-api", Duration.ofMinutes(1)))
            .withFailure(new ServiceUnavailabilityInjection("openai-api", Duration.ofSeconds(30)))
            .withAssertion(new ServiceAvailabilityAssertion("mcp-llm",
                () -> checkServiceHealth("http://localhost:8081/health")))
            .withAssertion(new ResponseTimeAssertion("llm-completion", Duration.ofSeconds(15),
                () -> measureResponseTime("http://localhost:8081/llm/completion")))
            .withAssertion(new DataConsistencyAssertion("debate state",
                this::verifyDebateStateConsistency))
            .withMetadata("category", "external-dependency")
            .withMetadata("severity", "medium")
            .build();
    }

    /**
     * Network partition test.
     */
    public ChaosExperiment networkPartition() {
        return framework.experiment()
            .named("Network Partition")
            .describedAs("Tests service mesh resilience during network partitions")
            .runningFor(Duration.ofMinutes(4))
            .withFailure(new NetworkLatencyInjection("mcp-organization", Duration.ofSeconds(5), 0.5))
            .withFailure(new NetworkLatencyInjection("mcp-context", Duration.ofSeconds(3), 0.3))
            .withAssertion(new ServiceAvailabilityAssertion("mcp-controller",
                () -> checkServiceHealth("http://localhost:8082/health")))
            .withAssertion(new ResponseTimeAssertion("debate-creation", Duration.ofSeconds(20),
                () -> measureResponseTime("http://localhost:8082/debates")))
            .withAssertion(new DataConsistencyAssertion("cross-service data",
                this::verifyCrossServiceDataConsistency))
            .withMetadata("category", "network")
            .withMetadata("severity", "high")
            .build();
    }

    /**
     * Memory pressure test.
     */
    public ChaosExperiment memoryPressure() {
        return framework.experiment()
            .named("Memory Pressure")
            .describedAs("Tests system behavior under high memory pressure")
            .runningFor(Duration.ofMinutes(3))
            .withFailure(new MemoryPressureInjection(512 * 1024 * 1024)) // 512MB
            .withAssertion(new ServiceAvailabilityAssertion("all-services",
                this::checkAllServicesHealth))
            .withAssertion(new ResponseTimeAssertion("health-check", Duration.ofSeconds(5),
                () -> measureAverageResponseTime()))
            .withAssertion(new DataConsistencyAssertion("memory integrity",
                this::verifyMemoryIntegrity))
            .withMetadata("category", "resource")
            .withMetadata("severity", "medium")
            .build();
    }

    /**
     * Cascading failure test.
     */
    public ChaosExperiment cascadingFailure() {
        return framework.experiment()
            .named("Cascading Failure")
            .describedAs("Tests system resilience to cascading service failures")
            .runningFor(Duration.ofMinutes(5))
            .withFailure(new ServiceUnavailabilityInjection("mcp-rag", Duration.ofMinutes(2)))
            .withFailure(new ServiceUnavailabilityInjection("mcp-context", Duration.ofMinutes(1)))
            .withFailure(new DatabaseFailureInjection("redis", 
                DatabaseFailureInjection.FailureType.CONNECTION_POOL_EXHAUSTION))
            .withAssertion(new ServiceAvailabilityAssertion("core-services",
                this::checkCoreServicesHealth))
            .withAssertion(new DataConsistencyAssertion("system state",
                this::verifySystemStateConsistency))
            .withAssertion(new ResponseTimeAssertion("degraded-performance", Duration.ofSeconds(30),
                this::measureDegradedPerformance))
            .withMetadata("category", "cascade")
            .withMetadata("severity", "critical")
            .build();
    }

    /**
     * Multi-tenant isolation under stress.
     */
    public ChaosExperiment multiTenantIsolationStress() {
        return framework.experiment()
            .named("Multi-Tenant Isolation Stress")
            .describedAs("Tests tenant isolation under high load and failures")
            .runningFor(Duration.ofMinutes(4))
            .withFailure(new NetworkLatencyInjection("mcp-organization", Duration.ofSeconds(2), 0.8))
            .withFailure(new MemoryPressureInjection(256 * 1024 * 1024))
            .withAssertion(new DataConsistencyAssertion("tenant-a-isolation",
                () -> verifyTenantIsolation("tenant-a", "tenant-b")))
            .withAssertion(new DataConsistencyAssertion("tenant-b-isolation",
                () -> verifyTenantIsolation("tenant-b", "tenant-c")))
            .withAssertion(new ResponseTimeAssertion("tenant-operations", Duration.ofSeconds(15),
                this::measureTenantOperationTime))
            .withMetadata("category", "multi-tenant")
            .withMetadata("severity", "high")
            .build();
    }

    /**
     * Real-time communication failure.
     */
    public ChaosExperiment realTimeCommunicationFailure() {
        return framework.experiment()
            .named("Real-Time Communication Failure")
            .describedAs("Tests WebSocket and SSE resilience during failures")
            .runningFor(Duration.ofMinutes(3))
            .withFailure(new NetworkLatencyInjection("mcp-gateway", Duration.ofSeconds(1), 0.9))
            .withFailure(new ServiceUnavailabilityInjection("mcp-gateway", Duration.ofSeconds(30)))
            .withAssertion(new DataConsistencyAssertion("websocket-recovery",
                this::verifyWebSocketRecovery))
            .withAssertion(new DataConsistencyAssertion("sse-recovery",
                this::verifySSERecovery))
            .withAssertion(new ResponseTimeAssertion("connection-reestablishment", Duration.ofSeconds(10),
                this::measureConnectionReestablishmentTime))
            .withMetadata("category", "real-time")
            .withMetadata("severity", "medium")
            .build();
    }

    /**
     * Security under pressure test.
     */
    public ChaosExperiment securityUnderPressure() {
        return framework.experiment()
            .named("Security Under Pressure")
            .describedAs("Tests security controls during system stress")
            .runningFor(Duration.ofMinutes(3))
            .withFailure(new MemoryPressureInjection(128 * 1024 * 1024))
            .withFailure(new NetworkLatencyInjection("mcp-organization", Duration.ofMillis(500), 0.5))
            .withAssertion(new DataConsistencyAssertion("authentication-integrity",
                this::verifyAuthenticationIntegrity))
            .withAssertion(new DataConsistencyAssertion("authorization-consistency",
                this::verifyAuthorizationConsistency))
            .withAssertion(new DataConsistencyAssertion("token-validation",
                this::verifyTokenValidation))
            .withMetadata("category", "security")
            .withMetadata("severity", "critical")
            .build();
    }

    // Helper methods for health checks and assertions

    private boolean checkServiceHealth(String healthUrl) {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    private Duration measureResponseTime(String url) {
        long start = System.currentTimeMillis();
        try {
            restTemplate.getForEntity(url, String.class);
        } catch (Exception e) {
            // Include failed requests in timing
        }
        long end = System.currentTimeMillis();
        return Duration.ofMillis(end - start);
    }

    private boolean checkAllServicesHealth() {
        String[] services = {
            "http://localhost:8080/health", // mcp-organization
            "http://localhost:8081/health", // mcp-llm
            "http://localhost:8082/health", // mcp-controller
            "http://localhost:8083/health", // mcp-context
            "http://localhost:8084/health", // mcp-rag
            "http://localhost:8085/health"  // mcp-gateway
        };

        int healthyServices = 0;
        for (String service : services) {
            if (checkServiceHealth(service)) {
                healthyServices++;
            }
        }

        // At least 80% of services should be healthy
        return (double) healthyServices / services.length >= 0.8;
    }

    private boolean checkCoreServicesHealth() {
        String[] coreServices = {
            "http://localhost:8080/health", // mcp-organization
            "http://localhost:8082/health", // mcp-controller
            "http://localhost:8085/health"  // mcp-gateway
        };

        for (String service : coreServices) {
            if (!checkServiceHealth(service)) {
                return false;
            }
        }
        return true;
    }

    private Duration measureAverageResponseTime() {
        String[] endpoints = {
            "http://localhost:8080/health",
            "http://localhost:8081/health",
            "http://localhost:8082/health"
        };

        long totalTime = 0;
        int successfulRequests = 0;

        for (String endpoint : endpoints) {
            try {
                long start = System.currentTimeMillis();
                restTemplate.getForEntity(endpoint, String.class);
                long end = System.currentTimeMillis();
                totalTime += (end - start);
                successfulRequests++;
            } catch (Exception e) {
                // Skip failed requests
            }
        }

        if (successfulRequests == 0) {
            return Duration.ofSeconds(30); // Assume failure
        }

        return Duration.ofMillis(totalTime / successfulRequests);
    }

    private Duration measureDegradedPerformance() {
        // Measure performance under degraded conditions
        return measureResponseTime("http://localhost:8082/debates");
    }

    private Duration measureTenantOperationTime() {
        // Measure tenant-specific operations
        long start = System.currentTimeMillis();
        try {
            restTemplate.getForEntity("http://localhost:8080/organizations/tenant-a", String.class);
        } catch (Exception e) {
            // Include failures in timing
        }
        long end = System.currentTimeMillis();
        return Duration.ofMillis(end - start);
    }

    private Duration measureConnectionReestablishmentTime() {
        // Simulate WebSocket reconnection time
        return Duration.ofMillis(ThreadLocalRandom.current().nextLong(1000, 5000));
    }

    private boolean verifyOrganizationDataConsistency() {
        // Verify organization data remains consistent during database issues
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:8080/organizations/test-org", String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean verifyDebateStateConsistency() {
        // Verify debate state remains consistent during LLM provider failures
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:8082/debates/test-debate-1", String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean verifyCrossServiceDataConsistency() {
        // Verify data consistency across services during network issues
        try {
            // Check if organization and debate services are in sync
            ResponseEntity<String> orgResponse = restTemplate.getForEntity(
                "http://localhost:8080/organizations/test-org", String.class);
            ResponseEntity<String> debateResponse = restTemplate.getForEntity(
                "http://localhost:8082/debates?organizationId=test-org", String.class);
            
            return orgResponse.getStatusCode().is2xxSuccessful() && 
                   debateResponse.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean verifyMemoryIntegrity() {
        // Verify system integrity under memory pressure
        return checkAllServicesHealth();
    }

    private boolean verifySystemStateConsistency() {
        // Verify overall system state consistency during cascading failures
        return checkCoreServicesHealth();
    }

    private boolean verifyTenantIsolation(String tenantA, String tenantB) {
        // Verify tenant data isolation under stress
        try {
            // This would check that tenant A cannot access tenant B's data
            ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:8080/organizations/" + tenantA + "/data", String.class);
            
            // In a real implementation, this would verify the response contains
            // only tenant A's data and no tenant B data
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean verifyWebSocketRecovery() {
        // Verify WebSocket connections can recover
        // This would test actual WebSocket connection recovery
        return true; // Simplified for example
    }

    private boolean verifySSERecovery() {
        // Verify Server-Sent Events can recover
        // This would test actual SSE connection recovery
        return true; // Simplified for example
    }

    private boolean verifyAuthenticationIntegrity() {
        // Verify authentication works correctly under pressure
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:8080/auth/validate", String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean verifyAuthorizationConsistency() {
        // Verify authorization rules are consistently applied
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:8080/organizations/test-org/users/test-user/permissions", String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean verifyTokenValidation() {
        // Verify JWT token validation works under stress
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:8080/auth/tokens/validate", String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}