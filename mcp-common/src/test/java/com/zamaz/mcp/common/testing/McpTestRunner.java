package com.zamaz.mcp.common.testing;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.core.Authentication;

import java.util.*;

/**
 * Main test runner for comprehensive MCP testing.
 * Orchestrates all types of MCP tests and generates comprehensive reports.
 */
@TestComponent
@RequiredArgsConstructor
@Slf4j
public class McpTestRunner {

    private final McpTestFramework testFramework;
    private final McpContractTest contractTest;
    private final McpIntegrationTest integrationTest;
    private final McpTestAuthenticationProvider authProvider;
    private final McpTestUtils testUtils;

    /**
     * Comprehensive test execution result.
     */
    public static class McpTestExecutionResult {
        private final McpTestFramework.McpTestSuiteResult frameworkResults;
        private final List<McpContractTest.McpContractResult> contractResults;
        private final List<McpIntegrationTest.McpIntegrationResult> integrationResults;
        private final Map<String, Object> summary;
        private final long totalExecutionTimeMs;

        public McpTestExecutionResult(McpTestFramework.McpTestSuiteResult frameworkResults,
                                    List<McpContractTest.McpContractResult> contractResults,
                                    List<McpIntegrationTest.McpIntegrationResult> integrationResults,
                                    Map<String, Object> summary,
                                    long totalExecutionTimeMs) {
            this.frameworkResults = frameworkResults;
            this.contractResults = contractResults;
            this.integrationResults = integrationResults;
            this.summary = summary;
            this.totalExecutionTimeMs = totalExecutionTimeMs;
        }

        // Getters
        public McpTestFramework.McpTestSuiteResult getFrameworkResults() { return frameworkResults; }
        public List<McpContractTest.McpContractResult> getContractResults() { return contractResults; }
        public List<McpIntegrationTest.McpIntegrationResult> getIntegrationResults() { return integrationResults; }
        public Map<String, Object> getSummary() { return summary; }
        public long getTotalExecutionTimeMs() { return totalExecutionTimeMs; }
    }

    /**
     * Run complete MCP test suite.
     */
    public McpTestExecutionResult runCompleteMcpTestSuite() {
        long startTime = System.currentTimeMillis();
        
        log.info("Starting complete MCP test suite execution");
        
        // Create test authentication
        Authentication testAuth = authProvider.createProUserAuthentication();
        
        // Run framework tests
        log.info("Running MCP framework tests...");
        McpTestFramework.McpTestSuiteResult frameworkResults = testFramework.runComprehensiveTestSuite(testAuth);
        
        // Run contract tests
        log.info("Running MCP contract compliance tests...");
        List<McpContractTest.McpContractResult> contractResults = runAllContractTests();
        
        // Run integration tests
        log.info("Running MCP integration tests...");
        List<McpIntegrationTest.McpIntegrationResult> integrationResults = 
            integrationTest.runAllIntegrationTests(testAuth);
        
        long totalExecutionTime = System.currentTimeMillis() - startTime;
        
        // Generate summary
        Map<String, Object> summary = generateExecutionSummary(
            frameworkResults, contractResults, integrationResults, totalExecutionTime);
        
        McpTestExecutionResult result = new McpTestExecutionResult(
            frameworkResults, contractResults, integrationResults, summary, totalExecutionTime);
        
        log.info("Complete MCP test suite finished: {} tests in {}ms", 
                getTotalTestCount(result), totalExecutionTime);
        
        return result;
    }

    /**
     * Run only framework tests.
     */
    public McpTestFramework.McpTestSuiteResult runFrameworkTestsOnly() {
        Authentication testAuth = authProvider.createProUserAuthentication();
        return testFramework.runComprehensiveTestSuite(testAuth);
    }

    /**
     * Run only contract tests.
     */
    public List<McpContractTest.McpContractResult> runContractTestsOnly() {
        return runAllContractTests();
    }

    /**
     * Run only integration tests.
     */
    public List<McpIntegrationTest.McpIntegrationResult> runIntegrationTestsOnly() {
        Authentication testAuth = authProvider.createProUserAuthentication();
        return integrationTest.runAllIntegrationTests(testAuth);
    }

    /**
     * Run performance tests with custom concurrency.
     */
    public List<McpTestFramework.McpTestResult> runPerformanceTests(int concurrency) {
        Authentication testAuth = authProvider.createProUserAuthentication();
        return testFramework.runParallelTests(testAuth, concurrency);
    }

    /**
     * Run security-focused tests.
     */
    public List<McpContractTest.McpContractResult> runSecurityTests() {
        List<McpContractTest.McpContractResult> results = new ArrayList<>();
        
        Authentication validAuth = authProvider.createProUserAuthentication();
        Authentication invalidAuth = authProvider.createAnonymousAuthentication();
        
        // Test authentication on each service
        for (com.zamaz.mcp.common.client.McpServiceRegistry.McpService service : 
             com.zamaz.mcp.common.client.McpServiceRegistry.McpService.values()) {
            McpContractTest.McpContractResult authResult = 
                contractTest.testAuthenticationContract(service, validAuth, invalidAuth);
            results.add(authResult);
        }
        
        return results;
    }

    /**
     * Generate comprehensive report.
     */
    public String generateComprehensiveReport(McpTestExecutionResult executionResult) {
        StringBuilder report = new StringBuilder();
        
        report.append("=".repeat(100)).append("\n");
        report.append("COMPREHENSIVE MCP TEST EXECUTION REPORT\n");
        report.append("=".repeat(100)).append("\n\n");
        
        // Executive Summary
        report.append("EXECUTIVE SUMMARY\n");
        report.append("-".repeat(50)).append("\n");
        Map<String, Object> summary = executionResult.getSummary();
        for (Map.Entry<String, Object> entry : summary.entrySet()) {
            report.append(String.format("%-30s: %s\n", entry.getKey(), entry.getValue()));
        }
        report.append("\n");
        
        // Framework Tests Summary
        report.append("FRAMEWORK TESTS\n");
        report.append("-".repeat(50)).append("\n");
        report.append(testUtils.generateTestSummaryReport(executionResult.getFrameworkResults()));
        report.append("\n");
        
        // Contract Compliance Summary
        report.append("CONTRACT COMPLIANCE\n");
        report.append("-".repeat(50)).append("\n");
        report.append(contractTest.generateComplianceReport(executionResult.getContractResults()));
        report.append("\n");
        
        // Integration Tests Summary
        report.append("INTEGRATION TESTS\n");
        report.append("-".repeat(50)).append("\n");
        generateIntegrationTestSummary(executionResult.getIntegrationResults(), report);
        report.append("\n");
        
        // Recommendations
        report.append("RECOMMENDATIONS\n");
        report.append("-".repeat(50)).append("\n");
        generateRecommendations(executionResult, report);
        
        report.append("\n").append("=".repeat(100)).append("\n");
        
        return report.toString();
    }

    /**
     * Save all reports to files.
     */
    public void saveAllReports(McpTestExecutionResult executionResult) {
        // Save comprehensive report
        String comprehensiveReport = generateComprehensiveReport(executionResult);
        testUtils.saveReportToFile(comprehensiveReport, "mcp-comprehensive-report.txt");
        
        // Save detailed JSON report
        JsonNode detailedJson = testUtils.generateDetailedJsonReport(executionResult.getFrameworkResults());
        testUtils.saveReportToFile(detailedJson.toPrettyString(), "mcp-detailed-report.json");
        
        // Save framework-only report
        String frameworkReport = testUtils.generateTestSummaryReport(executionResult.getFrameworkResults());
        testUtils.saveReportToFile(frameworkReport, "mcp-framework-report.txt");
        
        // Save contract compliance report
        String contractReport = contractTest.generateComplianceReport(executionResult.getContractResults());
        testUtils.saveReportToFile(contractReport, "mcp-contract-report.txt");
        
        log.info("All MCP test reports saved to test-reports/ directory");
    }

    // Private helper methods

    private List<McpContractTest.McpContractResult> runAllContractTests() {
        List<McpContractTest.McpContractResult> allResults = new ArrayList<>();
        
        for (com.zamaz.mcp.common.client.McpServiceRegistry.McpService service : 
             com.zamaz.mcp.common.client.McpServiceRegistry.McpService.values()) {
            List<McpContractTest.McpContractResult> serviceResults = 
                contractTest.runAllContractTests(service);
            allResults.addAll(serviceResults);
        }
        
        return allResults;
    }

    private Map<String, Object> generateExecutionSummary(
            McpTestFramework.McpTestSuiteResult frameworkResults,
            List<McpContractTest.McpContractResult> contractResults,
            List<McpIntegrationTest.McpIntegrationResult> integrationResults,
            long totalExecutionTime) {
        
        Map<String, Object> summary = new HashMap<>();
        
        // Overall counts
        int totalTests = frameworkResults.getTotalCount() + contractResults.size() + integrationResults.size();
        int totalPassed = frameworkResults.getPassedCount() + 
                         (int) contractResults.stream().mapToLong(r -> r.isCompliant() ? 1 : 0).sum() +
                         (int) integrationResults.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
        
        summary.put("Total Tests", totalTests);
        summary.put("Total Passed", totalPassed);
        summary.put("Total Failed", totalTests - totalPassed);
        summary.put("Overall Success Rate", String.format("%.1f%%", 
            totalTests > 0 ? (double) totalPassed / totalTests * 100 : 0));
        summary.put("Total Execution Time", testUtils.formatExecutionTime(totalExecutionTime));
        
        // Framework summary
        summary.put("Framework Tests", frameworkResults.getTotalCount());
        summary.put("Framework Passed", frameworkResults.getPassedCount());
        summary.put("Framework Success Rate", String.format("%.1f%%", frameworkResults.getSuccessRate()));
        
        // Contract summary
        long contractPassed = contractResults.stream().mapToLong(r -> r.isCompliant() ? 1 : 0).sum();
        summary.put("Contract Tests", contractResults.size());
        summary.put("Contract Compliant", contractPassed);
        summary.put("Contract Compliance Rate", String.format("%.1f%%", 
            contractResults.size() > 0 ? (double) contractPassed / contractResults.size() * 100 : 0));
        
        // Integration summary
        long integrationPassed = integrationResults.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
        summary.put("Integration Tests", integrationResults.size());
        summary.put("Integration Passed", integrationPassed);
        summary.put("Integration Success Rate", String.format("%.1f%%", 
            integrationResults.size() > 0 ? (double) integrationPassed / integrationResults.size() * 100 : 0));
        
        return summary;
    }

    private int getTotalTestCount(McpTestExecutionResult result) {
        return result.getFrameworkResults().getTotalCount() + 
               result.getContractResults().size() + 
               result.getIntegrationResults().size();
    }

    private void generateIntegrationTestSummary(List<McpIntegrationTest.McpIntegrationResult> results, 
                                               StringBuilder report) {
        long passed = results.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
        long failed = results.size() - passed;
        
        report.append(String.format("Total Integration Tests: %d\n", results.size()));
        report.append(String.format("Passed: %d\n", passed));
        report.append(String.format("Failed: %d\n", failed));
        report.append(String.format("Success Rate: %.1f%%\n\n", 
            results.size() > 0 ? (double) passed / results.size() * 100 : 0));
        
        for (McpIntegrationTest.McpIntegrationResult result : results) {
            String status = result.isSuccess() ? "✅ PASSED" : "❌ FAILED";
            report.append(String.format("  %s: %s (%dms)\n", 
                result.getTestName(), status, result.getExecutionTimeMs()));
            
            if (!result.isSuccess() && result.getErrorMessage() != null) {
                report.append(String.format("    Error: %s\n", result.getErrorMessage()));
            }
            
            if (!result.getStepsExecuted().isEmpty()) {
                report.append(String.format("    Steps: %s\n", 
                    String.join(" → ", result.getStepsExecuted())));
            }
        }
    }

    private void generateRecommendations(McpTestExecutionResult result, StringBuilder report) {
        List<String> recommendations = new ArrayList<>();
        
        // Analyze results and generate recommendations
        double overallSuccessRate = (double) Integer.parseInt(
            result.getSummary().get("Total Passed").toString()) / 
            Integer.parseInt(result.getSummary().get("Total Tests").toString()) * 100;
        
        if (overallSuccessRate < 95) {
            recommendations.add("🔴 Overall success rate is below 95%. Review failed tests and address issues.");
        } else {
            recommendations.add("✅ Excellent overall success rate. System is performing well.");
        }
        
        // Check contract compliance
        long contractIssues = result.getContractResults().stream()
            .mapToLong(r -> r.isCompliant() ? 0 : 1).sum();
        if (contractIssues > 0) {
            recommendations.add("🔴 Contract compliance issues detected. Review MCP protocol implementation.");
        }
        
        // Check performance
        long slowTests = result.getFrameworkResults().getAllResults().stream()
            .mapToLong(r -> r.getExecutionTimeMs() > 2000 ? 1 : 0).sum();
        if (slowTests > 0) {
            recommendations.add("⚠️ Some tests are running slowly (>2s). Consider performance optimization.");
        }
        
        // Check integration test failures
        long integrationFailures = result.getIntegrationResults().stream()
            .mapToLong(r -> r.isSuccess() ? 0 : 1).sum();
        if (integrationFailures > 0) {
            recommendations.add("🔴 Integration test failures detected. Review service interactions.");
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("🎉 All tests passing! System is ready for production deployment.");
        }
        
        for (String recommendation : recommendations) {
            report.append(recommendation).append("\n");
        }
    }
}