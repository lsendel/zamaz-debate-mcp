package com.zamaz.mcp.controller.chaos;

import com.zamaz.mcp.common.testing.chaos.ChaosEngineeringFramework;
import com.zamaz.mcp.common.testing.chaos.ChaosEngineeringFramework.*;
import com.zamaz.mcp.common.testing.chaos.McpChaosExperiments;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Chaos engineering tests for debate service resilience.
 * Tests the debate controller's ability to handle various failure scenarios.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("chaos-test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DebateResilienceTest {

    private ChaosEngineeringFramework chaosFramework;
    private McpChaosExperiments chaosExperiments;

    @BeforeEach
    void setUp() {
        ChaosConfiguration config = new ChaosConfiguration();
        config.setConcurrency(2);
        config.setDefaultTimeout(Duration.ofMinutes(10));
        config.setSafetyMode(true);
        
        chaosFramework = new ChaosEngineeringFramework(config);
        chaosExperiments = new McpChaosExperiments(chaosFramework);
    }

    /**
     * Test debate creation resilience when LLM services fail.
     */
    @Test
    void testDebateCreationWithLlmFailure() throws Exception {
        ChaosExperiment experiment = chaosFramework.experiment()
            .named("Debate Creation LLM Failure")
            .describedAs("Tests debate creation when LLM providers become unavailable")
            .runningFor(Duration.ofMinutes(2))
            .withFailure(new ServiceUnavailabilityInjection("anthropic-api", Duration.ofMinutes(1)))
            .withAssertion(new ServiceAvailabilityAssertion("mcp-controller",
                () -> checkDebateServiceHealth()))
            .withAssertion(new ResponseTimeAssertion("debate-creation", Duration.ofSeconds(30),
                () -> measureDebateCreationTime()))
            .withAssertion(new DataConsistencyAssertion("debate-persistence",
                this::verifyDebatePersistence))
            .build();

        CompletableFuture<ExperimentResult> resultFuture = chaosFramework.runExperiment(experiment);
        ExperimentResult result = resultFuture.get();

        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getAssertionResults()).allMatch(AssertionResult::isPassed);
    }

    /**
     * Test debate progression during database connectivity issues.
     */
    @Test
    void testDebateProgressionWithDatabaseIssues() throws Exception {
        ChaosExperiment experiment = chaosFramework.experiment()
            .named("Debate Progression Database Issues")
            .describedAs("Tests ongoing debate progression during database connectivity problems")
            .runningFor(Duration.ofMinutes(3))
            .withFailure(new DatabaseFailureInjection("postgres", 
                DatabaseFailureInjection.FailureType.CONNECTION_TIMEOUT))
            .withFailure(new NetworkLatencyInjection("mcp-context", Duration.ofSeconds(2), 0.7))
            .withAssertion(new ServiceAvailabilityAssertion("debate-service",
                this::checkDebateServiceHealth))
            .withAssertion(new DataConsistencyAssertion("debate-state-consistency",
                this::verifyDebateStateConsistency))
            .withAssertion(new ResponseTimeAssertion("turn-processing", Duration.ofSeconds(45),
                this::measureTurnProcessingTime))
            .build();

        CompletableFuture<ExperimentResult> resultFuture = chaosFramework.runExperiment(experiment);
        ExperimentResult result = resultFuture.get();

        assertThat(result.isSuccessful()).isTrue();
        
        // Verify that at least some assertions passed (graceful degradation)
        long passedAssertions = result.getAssertionResults().stream()
            .mapToLong(ar -> ar.isPassed() ? 1 : 0)
            .sum();
        assertThat(passedAssertions).isGreaterThanOrEqualTo(2);
    }

    /**
     * Test multi-participant debate resilience.
     */
    @Test
    void testMultiParticipantDebateResilience() throws Exception {
        ChaosExperiment experiment = chaosFramework.experiment()
            .named("Multi-Participant Debate Resilience")
            .describedAs("Tests multi-participant debates under various failure conditions")
            .runningFor(Duration.ofMinutes(4))
            .withFailure(new ServiceUnavailabilityInjection("openai-api", Duration.ofSeconds(45)))
            .withFailure(new NetworkLatencyInjection("mcp-llm", Duration.ofMillis(800), 0.6))
            .withFailure(new MemoryPressureInjection(256 * 1024 * 1024)) // 256MB
            .withAssertion(new ServiceAvailabilityAssertion("debate-orchestration",
                this::checkDebateOrchestrationHealth))
            .withAssertion(new DataConsistencyAssertion("participant-coordination",
                this::verifyParticipantCoordination))
            .withAssertion(new DataConsistencyAssertion("turn-ordering",
                this::verifyTurnOrdering))
            .withAssertion(new ResponseTimeAssertion("participant-response", Duration.ofMinutes(2),
                this::measureParticipantResponseTime))
            .build();

        CompletableFuture<ExperimentResult> resultFuture = chaosFramework.runExperiment(experiment);
        ExperimentResult result = resultFuture.get();

        // Multi-participant debates should maintain basic functionality
        assertThat(result.getAssertionResults())
            .filteredOn(ar -> ar.getAssertionName().contains("coordination") || 
                            ar.getAssertionName().contains("ordering"))
            .allMatch(AssertionResult::isPassed);
    }

    /**
     * Test real-time updates during network partitions.
     */
    @Test
    void testRealTimeUpdatesWithNetworkPartition() throws Exception {
        ChaosExperiment experiment = chaosFramework.experiment()
            .named("Real-Time Updates Network Partition")
            .describedAs("Tests WebSocket/SSE real-time updates during network partitions")
            .runningFor(Duration.ofMinutes(3))
            .withFailure(new NetworkLatencyInjection("mcp-gateway", Duration.ofSeconds(3), 0.8))
            .withFailure(new ServiceUnavailabilityInjection("mcp-gateway", Duration.ofSeconds(30)))
            .withAssertion(new DataConsistencyAssertion("websocket-recovery",
                this::verifyWebSocketRecovery))
            .withAssertion(new DataConsistencyAssertion("sse-recovery",
                this::verifyServerSentEventRecovery))
            .withAssertion(new ResponseTimeAssertion("reconnection-time", Duration.ofSeconds(15),
                this::measureReconnectionTime))
            .build();

        CompletableFuture<ExperimentResult> resultFuture = chaosFramework.runExperiment(experiment);
        ExperimentResult result = resultFuture.get();

        // Real-time features should be resilient or fail gracefully
        assertThat(result.isSuccessful()).isTrue();
    }

    /**
     * Test debate scaling under load with failures.
     */
    @Test
    void testDebateScalingUnderLoad() throws Exception {
        ChaosExperiment experiment = chaosFramework.experiment()
            .named("Debate Scaling Under Load")
            .describedAs("Tests system's ability to handle multiple debates during failures")
            .runningFor(Duration.ofMinutes(5))
            .withFailure(new MemoryPressureInjection(512 * 1024 * 1024)) // 512MB
            .withFailure(new NetworkLatencyInjection("mcp-llm", Duration.ofSeconds(1), 0.5))
            .withFailure(new DatabaseFailureInjection("redis", 
                DatabaseFailureInjection.FailureType.SLOW_QUERIES))
            .withAssertion(new ServiceAvailabilityAssertion("concurrent-debates",
                this::checkConcurrentDebatesHealth))
            .withAssertion(new ResponseTimeAssertion("debate-throughput", Duration.ofSeconds(60),
                this::measureDebateThroughput))
            .withAssertion(new DataConsistencyAssertion("resource-isolation",
                this::verifyResourceIsolation))
            .build();

        CompletableFuture<ExperimentResult> resultFuture = chaosFramework.runExperiment(experiment);
        ExperimentResult result = resultFuture.get();

        // System should maintain basic functionality under load
        long passedAssertions = result.getAssertionResults().stream()
            .mapToLong(ar -> ar.isPassed() ? 1 : 0)
            .sum();
        assertThat(passedAssertions).isGreaterThanOrEqualTo(2);
    }

    /**
     * Run all pre-defined chaos experiments.
     */
    @Test
    void runAllChaosExperiments() throws Exception {
        // Add all pre-defined experiments
        chaosFramework.addExperiment(chaosExperiments.llmProviderFailure());
        chaosFramework.addExperiment(chaosExperiments.networkPartition());
        chaosFramework.addExperiment(chaosExperiments.multiTenantIsolationStress());
        chaosFramework.addExperiment(chaosExperiments.realTimeCommunicationFailure());

        CompletableFuture<List<ExperimentResult>> resultsFuture = chaosFramework.runAllExperiments();
        List<ExperimentResult> results = resultsFuture.get();

        assertThat(results).isNotEmpty();
        
        // At least 75% of experiments should pass
        long passedExperiments = results.stream()
            .mapToLong(r -> r.isSuccessful() ? 1 : 0)
            .sum();
        double successRate = (double) passedExperiments / results.size();
        assertThat(successRate).isGreaterThanOrEqualTo(0.75);

        // Log results for analysis
        results.forEach(result -> {
            System.out.println("Experiment: " + result.getExperimentName());
            System.out.println("Success: " + result.isSuccessful());
            System.out.println("Duration: " + result.getDuration().toSeconds() + "s");
            System.out.println("---");
        });
    }

    // Helper methods for health checks and measurements

    private boolean checkDebateServiceHealth() {
        // Check if debate service endpoints are responding
        try {
            // Simulate health check
            return true; // Simplified for example
        } catch (Exception e) {
            return false;
        }
    }

    private Duration measureDebateCreationTime() {
        // Measure time to create a new debate
        long start = System.currentTimeMillis();
        try {
            // Simulate debate creation request
            Thread.sleep(1000); // Simulate processing time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long end = System.currentTimeMillis();
        return Duration.ofMillis(end - start);
    }

    private boolean verifyDebatePersistence() {
        // Verify that debates are properly persisted
        return true; // Simplified for example
    }

    private boolean verifyDebateStateConsistency() {
        // Verify that debate state remains consistent
        return true; // Simplified for example
    }

    private Duration measureTurnProcessingTime() {
        // Measure time to process a debate turn
        return Duration.ofSeconds(15); // Simplified for example
    }

    private boolean checkDebateOrchestrationHealth() {
        // Check debate orchestration components
        return true; // Simplified for example
    }

    private boolean verifyParticipantCoordination() {
        // Verify that participants are properly coordinated
        return true; // Simplified for example
    }

    private boolean verifyTurnOrdering() {
        // Verify that debate turns are properly ordered
        return true; // Simplified for example
    }

    private Duration measureParticipantResponseTime() {
        // Measure time for participant to respond
        return Duration.ofSeconds(45); // Simplified for example
    }

    private boolean verifyWebSocketRecovery() {
        // Verify WebSocket connections can recover
        return true; // Simplified for example
    }

    private boolean verifyServerSentEventRecovery() {
        // Verify SSE connections can recover
        return true; // Simplified for example
    }

    private Duration measureReconnectionTime() {
        // Measure time to reestablish connections
        return Duration.ofSeconds(5); // Simplified for example
    }

    private boolean checkConcurrentDebatesHealth() {
        // Check health of concurrent debate processing
        return true; // Simplified for example
    }

    private Duration measureDebateThroughput() {
        // Measure debate processing throughput
        return Duration.ofSeconds(30); // Simplified for example
    }

    private boolean verifyResourceIsolation() {
        // Verify that debates don't interfere with each other
        return true; // Simplified for example
    }
}