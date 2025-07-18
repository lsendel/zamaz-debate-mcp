package com.zamaz.mcp.controller.e2e;

import com.zamaz.mcp.common.testing.e2e.EndToEndValidationSuite;
import com.zamaz.mcp.common.testing.e2e.EndToEndValidationSuite.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-End validation tests for the debate system.
 * Orchestrates comprehensive validation across all testing frameworks.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("e2e-test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DebateE2EValidationTest {

    private EndToEndValidationSuite validationSuite;

    @BeforeEach
    void setUp() {
        E2EConfiguration config = new E2EConfiguration();
        config.setConcurrency(3);
        config.setTotalTimeout(Duration.ofHours(1));
        config.setFailFast(false);
        config.setGenerateReports(true);
        
        validationSuite = new EndToEndValidationSuite(config);
    }

    @AfterEach
    void tearDown() {
        if (validationSuite != null) {
            validationSuite.shutdown();
        }
    }

    /**
     * Complete end-to-end validation of the entire debate system.
     */
    @Test
    @Order(1)
    void runCompleteE2EValidation() throws Exception {
        CompletableFuture<ValidationSuiteResult> resultFuture = validationSuite.executeValidation();
        ValidationSuiteResult result = resultFuture.get();

        // Verify overall success
        assertThat(result.isOverallSuccess()).isTrue();
        
        // Verify success rate is acceptable
        assertThat(result.getSuccessRate()).isGreaterThanOrEqualTo(0.95);
        
        // Verify all critical phases passed
        assertThat(result.getResultsByPhase())
            .containsKeys(
                ValidationPhase.PREREQUISITES,
                ValidationPhase.UNIT_INTEGRATION,
                ValidationPhase.CONTRACT,
                ValidationPhase.FUNCTIONAL
            );

        // Verify specific assertions for debate system
        verifyDebateSpecificValidations(result);

        // Generate and verify report
        ValidationReport report = validationSuite.generateReport(result);
        assertThat(report.getHtmlReport()).isNotEmpty();
        assertThat(report.getJsonReport()).isNotEmpty();

        // Log summary for manual review
        logValidationSummary(result);
    }

    /**
     * Test validation with only critical phases enabled.
     */
    @Test
    @Order(2)
    void runCriticalValidationOnly() throws Exception {
        E2EConfiguration config = new E2EConfiguration();
        config.setEnabledPhases(EnumSet.of(
            ValidationPhase.PREREQUISITES,
            ValidationPhase.UNIT_INTEGRATION,
            ValidationPhase.FUNCTIONAL
        ));
        config.setTotalTimeout(Duration.ofMinutes(30));
        
        EndToEndValidationSuite criticalSuite = new EndToEndValidationSuite(config);
        
        try {
            CompletableFuture<ValidationSuiteResult> resultFuture = criticalSuite.executeValidation();
            ValidationSuiteResult result = resultFuture.get();

            // Critical phases must all pass
            assertThat(result.isOverallSuccess()).isTrue();
            assertThat(result.getSuccessRate()).isEqualTo(1.0);
            
            // Verify only enabled phases were executed
            assertThat(result.getResultsByPhase()).hasSize(3);
            assertThat(result.getResultsByPhase())
                .containsOnlyKeys(
                    ValidationPhase.PREREQUISITES,
                    ValidationPhase.UNIT_INTEGRATION,
                    ValidationPhase.FUNCTIONAL
                );
        } finally {
            criticalSuite.shutdown();
        }
    }

    /**
     * Test validation with fail-fast enabled.
     */
    @Test
    @Order(3)
    void runFailFastValidation() throws Exception {
        E2EConfiguration config = new E2EConfiguration();
        config.setFailFast(true);
        config.setTotalTimeout(Duration.ofMinutes(45));
        
        EndToEndValidationSuite failFastSuite = new EndToEndValidationSuite(config);
        
        try {
            CompletableFuture<ValidationSuiteResult> resultFuture = failFastSuite.executeValidation();
            ValidationSuiteResult result = resultFuture.get();

            // Verify execution behavior
            assertThat(result.getTotalDuration()).isLessThan(Duration.ofMinutes(45));
            
            // If any phase failed, subsequent phases should not have run
            if (!result.isOverallSuccess()) {
                verifyFailFastBehavior(result);
            }
        } finally {
            failFastSuite.shutdown();
        }
    }

    /**
     * Test performance validation scenario.
     */
    @Test
    @Order(4)
    void runPerformanceValidation() throws Exception {
        E2EConfiguration config = new E2EConfiguration();
        config.setEnabledPhases(EnumSet.of(
            ValidationPhase.PREREQUISITES,
            ValidationPhase.PERFORMANCE
        ));
        config.setConcurrency(4);
        
        EndToEndValidationSuite perfSuite = new EndToEndValidationSuite(config);
        
        try {
            CompletableFuture<ValidationSuiteResult> resultFuture = perfSuite.executeValidation();
            ValidationSuiteResult result = resultFuture.get();

            // Verify performance metrics were collected
            assertThat(result.getAggregatedMetrics())
                .containsKeys(
                    "performance_modules",
                    "performance_successful",
                    "performance_duration_seconds"
                );

            // Verify performance requirements
            verifyPerformanceRequirements(result);
        } finally {
            perfSuite.shutdown();
        }
    }

    /**
     * Test resilience validation scenario.
     */
    @Test
    @Order(5)
    void runResilienceValidation() throws Exception {
        E2EConfiguration config = new E2EConfiguration();
        config.setEnabledPhases(EnumSet.of(
            ValidationPhase.PREREQUISITES,
            ValidationPhase.RESILIENCE
        ));
        config.setTotalTimeout(Duration.ofMinutes(30));
        
        EndToEndValidationSuite resilienceSuite = new EndToEndValidationSuite(config);
        
        try {
            CompletableFuture<ValidationSuiteResult> resultFuture = resilienceSuite.executeValidation();
            ValidationSuiteResult result = resultFuture.get();

            // Verify resilience testing was performed
            assertThat(result.getAggregatedMetrics())
                .containsKeys(
                    "resilience_modules",
                    "resilience_successful"
                );

            // Resilience tests may have some acceptable failures
            assertThat(result.getSuccessRate()).isGreaterThanOrEqualTo(0.80);
            
            verifyResilienceRequirements(result);
        } finally {
            resilienceSuite.shutdown();
        }
    }

    /**
     * Test security validation scenario.
     */
    @Test
    @Order(6)
    void runSecurityValidation() throws Exception {
        E2EConfiguration config = new E2EConfiguration();
        config.setEnabledPhases(EnumSet.of(
            ValidationPhase.PREREQUISITES,
            ValidationPhase.SECURITY
        ));
        
        EndToEndValidationSuite securitySuite = new EndToEndValidationSuite(config);
        
        try {
            CompletableFuture<ValidationSuiteResult> resultFuture = securitySuite.executeValidation();
            ValidationSuiteResult result = resultFuture.get();

            // Security tests must all pass
            assertThat(result.isOverallSuccess()).isTrue();
            assertThat(result.getSuccessRate()).isEqualTo(1.0);
            
            verifySecurityRequirements(result);
        } finally {
            securitySuite.shutdown();
        }
    }

    // Helper methods for verification

    private void verifyDebateSpecificValidations(ValidationSuiteResult result) {
        // Verify that debate-specific functionality was tested
        assertThat(result.getAggregatedMetrics()).containsKey("total_modules");
        assertThat((Long) result.getAggregatedMetrics().get("total_modules")).isGreaterThan(5L);
        
        // Verify functional tests covered debate scenarios
        if (result.getResultsByPhase().containsKey(ValidationPhase.FUNCTIONAL)) {
            ModuleResult functionalResult = result.getResultsByPhase()
                .get(ValidationPhase.FUNCTIONAL)
                .stream()
                .filter(r -> r.getModuleName().contains("Functional"))
                .findFirst()
                .orElse(null);
                
            assertThat(functionalResult).isNotNull();
            assertThat(functionalResult.isSuccessful()).isTrue();
            assertThat(functionalResult.getMetrics())
                .containsKeys("debate_scenarios_tested", "multi_tenant_scenarios_tested");
        }
    }

    private void verifyFailFastBehavior(ValidationSuiteResult result) {
        // Find the first failed phase
        ValidationPhase firstFailedPhase = null;
        for (ValidationPhase phase : ValidationPhase.values()) {
            if (result.getResultsByPhase().containsKey(phase)) {
                boolean phaseSuccess = result.getResultsByPhase().get(phase)
                    .stream().allMatch(ModuleResult::isSuccessful);
                if (!phaseSuccess) {
                    firstFailedPhase = phase;
                    break;
                }
            }
        }
        
        if (firstFailedPhase != null) {
            // Verify subsequent phases were not executed
            boolean foundFailedPhase = false;
            for (ValidationPhase phase : ValidationPhase.values()) {
                if (phase.equals(firstFailedPhase)) {
                    foundFailedPhase = true;
                    continue;
                }
                if (foundFailedPhase) {
                    assertThat(result.getResultsByPhase()).doesNotContainKey(phase);
                }
            }
        }
    }

    private void verifyPerformanceRequirements(ValidationSuiteResult result) {
        if (result.getResultsByPhase().containsKey(ValidationPhase.PERFORMANCE)) {
            ModuleResult perfResult = result.getResultsByPhase()
                .get(ValidationPhase.PERFORMANCE)
                .stream()
                .filter(r -> r.getModuleName().contains("Performance"))
                .findFirst()
                .orElse(null);
                
            if (perfResult != null && perfResult.isSuccessful()) {
                // Verify performance SLAs
                assertThat(perfResult.getMetrics()).containsKey("average_response_time_ms");
                Object avgResponseTime = perfResult.getMetrics().get("average_response_time_ms");
                if (avgResponseTime instanceof Number) {
                    assertThat(((Number) avgResponseTime).doubleValue()).isLessThan(500.0);
                }
                
                assertThat(perfResult.getMetrics()).containsKey("error_rate_percentage");
                Object errorRate = perfResult.getMetrics().get("error_rate_percentage");
                if (errorRate instanceof Number) {
                    assertThat(((Number) errorRate).doubleValue()).isLessThan(1.0);
                }
            }
        }
    }

    private void verifyResilienceRequirements(ValidationSuiteResult result) {
        if (result.getResultsByPhase().containsKey(ValidationPhase.RESILIENCE)) {
            ModuleResult resilienceResult = result.getResultsByPhase()
                .get(ValidationPhase.RESILIENCE)
                .stream()
                .filter(r -> r.getModuleName().contains("Chaos"))
                .findFirst()
                .orElse(null);
                
            if (resilienceResult != null) {
                // Verify resilience metrics
                assertThat(resilienceResult.getMetrics()).containsKey("chaos_experiments_run");
                assertThat(resilienceResult.getMetrics()).containsKey("system_recovery_time_seconds");
                
                Object recoveryTime = resilienceResult.getMetrics().get("system_recovery_time_seconds");
                if (recoveryTime instanceof Number) {
                    // System should recover within 2 minutes
                    assertThat(((Number) recoveryTime).doubleValue()).isLessThan(120.0);
                }
            }
        }
    }

    private void verifySecurityRequirements(ValidationSuiteResult result) {
        if (result.getResultsByPhase().containsKey(ValidationPhase.SECURITY)) {
            ModuleResult securityResult = result.getResultsByPhase()
                .get(ValidationPhase.SECURITY)
                .stream()
                .filter(r -> r.getModuleName().contains("Security"))
                .findFirst()
                .orElse(null);
                
            if (securityResult != null && securityResult.isSuccessful()) {
                // Verify security metrics
                assertThat(securityResult.getMetrics()).containsKey("vulnerabilities_found");
                Object vulnerabilities = securityResult.getMetrics().get("vulnerabilities_found");
                if (vulnerabilities instanceof Number) {
                    // No vulnerabilities should be found
                    assertThat(((Number) vulnerabilities).intValue()).isEqualTo(0);
                }
                
                assertThat(securityResult.getMetrics()).containsKey("security_score");
                Object securityScore = securityResult.getMetrics().get("security_score");
                if (securityScore instanceof Number) {
                    // Security score should be above 90%
                    assertThat(((Number) securityScore).doubleValue()).isGreaterThan(90.0);
                }
            }
        }
    }

    private void logValidationSummary(ValidationSuiteResult result) {
        System.out.println("\n=== E2E Validation Summary ===");
        System.out.println("Execution ID: " + result.getExecutionId());
        System.out.println("Overall Success: " + result.isOverallSuccess());
        System.out.println("Success Rate: " + String.format("%.1f%%", result.getSuccessRate() * 100));
        System.out.println("Total Duration: " + result.getTotalDuration().toSeconds() + "s");
        System.out.println("Modules: " + result.getSuccessfulModules() + "/" + result.getTotalModules());
        
        System.out.println("\nPhase Results:");
        for (String summary : result.getSummary()) {
            System.out.println("  " + summary);
        }
        
        System.out.println("\nKey Metrics:");
        result.getAggregatedMetrics().forEach((key, value) -> {
            System.out.println("  " + key + ": " + value);
        });
        
        System.out.println("=== End Summary ===\n");
    }
}