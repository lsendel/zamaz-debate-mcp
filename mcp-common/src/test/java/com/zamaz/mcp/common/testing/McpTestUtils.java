package com.zamaz.mcp.common.testing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for MCP testing operations.
 * Provides common testing utilities, validation, and reporting functions.
 */
@Component
@Slf4j
public class McpTestUtils {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Validate MCP response structure.
     */
    public boolean validateMcpResponse(JsonNode response) {
        if (response == null) {
            log.warn("MCP response is null");
            return false;
        }

        // Check for error response structure
        if (response.has("error")) {
            JsonNode error = response.get("error");
            boolean hasErrorFields = error.has("code") && error.has("message");
            if (!hasErrorFields) {
                log.warn("Invalid MCP error response structure: {}", response);
            }
            return hasErrorFields;
        }

        // Check for success response structure
        if (!response.has("content")) {
            log.warn("MCP response missing 'content' field: {}", response);
            return false;
        }

        return true;
    }

    /**
     * Validate MCP tool list response.
     */
    public boolean validateToolListResponse(JsonNode response) {
        if (!validateMcpResponse(response)) {
            return false;
        }

        JsonNode content = response.get("content");
        if (!content.isArray()) {
            log.warn("MCP tool list content is not an array: {}", content);
            return false;
        }

        // Validate each tool definition
        for (JsonNode tool : content) {
            if (!tool.has("name") || !tool.has("description")) {
                log.warn("Invalid tool definition: {}", tool);
                return false;
            }
        }

        return true;
    }

    /**
     * Extract tool names from MCP tool list response.
     */
    public List<String> extractToolNames(JsonNode toolListResponse) {
        if (!validateToolListResponse(toolListResponse)) {
            return Collections.emptyList();
        }

        List<String> toolNames = new ArrayList<>();
        JsonNode content = toolListResponse.get("content");
        
        for (JsonNode tool : content) {
            if (tool.has("name")) {
                toolNames.add(tool.get("name").asText());
            }
        }

        return toolNames;
    }

    /**
     * Calculate test execution statistics.
     */
    public Map<String, Object> calculateTestStatistics(List<McpTestFramework.McpTestResult> results) {
        Map<String, Object> stats = new HashMap<>();
        
        int total = results.size();
        int passed = (int) results.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
        int failed = total - passed;
        
        double successRate = total > 0 ? (double) passed / total * 100 : 0;
        
        long totalExecutionTime = results.stream().mapToLong(McpTestFramework.McpTestResult::getExecutionTimeMs).sum();
        double avgExecutionTime = total > 0 ? (double) totalExecutionTime / total : 0;
        
        long maxExecutionTime = results.stream().mapToLong(McpTestFramework.McpTestResult::getExecutionTimeMs).max().orElse(0);
        long minExecutionTime = results.stream().mapToLong(McpTestFramework.McpTestResult::getExecutionTimeMs).min().orElse(0);
        
        stats.put("totalTests", total);
        stats.put("passedTests", passed);
        stats.put("failedTests", failed);
        stats.put("successRate", Math.round(successRate * 100.0) / 100.0);
        stats.put("totalExecutionTimeMs", totalExecutionTime);
        stats.put("averageExecutionTimeMs", Math.round(avgExecutionTime * 100.0) / 100.0);
        stats.put("maxExecutionTimeMs", maxExecutionTime);
        stats.put("minExecutionTimeMs", minExecutionTime);
        
        return stats;
    }

    /**
     * Group test results by service.
     */
    public Map<String, List<McpTestFramework.McpTestResult>> groupResultsByService(
            List<McpTestFramework.McpTestResult> results) {
        return results.stream().collect(Collectors.groupingBy(McpTestFramework.McpTestResult::getServiceName));
    }

    /**
     * Group test results by success/failure.
     */
    public Map<Boolean, List<McpTestFramework.McpTestResult>> groupResultsBySuccess(
            List<McpTestFramework.McpTestResult> results) {
        return results.stream().collect(Collectors.groupingBy(McpTestFramework.McpTestResult::isSuccess));
    }

    /**
     * Find tests that exceed performance thresholds.
     */
    public List<McpTestFramework.McpTestResult> findSlowTests(
            List<McpTestFramework.McpTestResult> results, long thresholdMs) {
        return results.stream()
                .filter(r -> r.getExecutionTimeMs() > thresholdMs)
                .sorted(Comparator.comparingLong(McpTestFramework.McpTestResult::getExecutionTimeMs).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Generate test summary report.
     */
    public String generateTestSummaryReport(McpTestFramework.McpTestSuiteResult suiteResult) {
        StringBuilder report = new StringBuilder();
        
        report.append("=".repeat(80)).append("\n");
        report.append("MCP TEST SUITE SUMMARY REPORT\n");
        report.append("Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
        report.append("=".repeat(80)).append("\n\n");
        
        // Overall statistics
        report.append("OVERALL RESULTS:\n");
        report.append(String.format("  Total Tests: %d\n", suiteResult.getTotalCount()));
        report.append(String.format("  Passed: %d\n", suiteResult.getPassedCount()));
        report.append(String.format("  Failed: %d\n", suiteResult.getFailedCount()));
        report.append(String.format("  Success Rate: %.1f%%\n", suiteResult.getSuccessRate()));
        report.append(String.format("  Total Execution Time: %dms\n\n", suiteResult.getTotalExecutionTimeMs()));
        
        // Results by service
        report.append("RESULTS BY SERVICE:\n");
        Map<String, Integer> passedByService = suiteResult.getPassedCountByService();
        Map<String, List<McpTestFramework.McpTestResult>> resultsByService = suiteResult.getResultsByService();
        
        for (Map.Entry<String, List<McpTestFramework.McpTestResult>> entry : resultsByService.entrySet()) {
            String serviceName = entry.getKey();
            List<McpTestFramework.McpTestResult> serviceResults = entry.getValue();
            int servicePassed = passedByService.getOrDefault(serviceName, 0);
            int serviceTotal = serviceResults.size();
            double serviceSuccessRate = serviceTotal > 0 ? (double) servicePassed / serviceTotal * 100 : 0;
            
            report.append(String.format("  %s: %d/%d (%.1f%%)\n", 
                serviceName.toUpperCase(), servicePassed, serviceTotal, serviceSuccessRate));
        }
        
        // Failed tests details
        List<McpTestFramework.McpTestResult> failedTests = suiteResult.getFailedTests();
        if (!failedTests.isEmpty()) {
            report.append("\nFAILED TESTS:\n");
            for (McpTestFramework.McpTestResult failed : failedTests) {
                report.append(String.format("  ❌ %s (%s/%s): %s\n", 
                    failed.getTestName(), failed.getServiceName(), failed.getToolName(), 
                    failed.getErrorMessage()));
            }
        }
        
        // Performance analysis
        List<McpTestFramework.McpTestResult> allResults = suiteResult.getAllResults();
        List<McpTestFramework.McpTestResult> slowTests = findSlowTests(allResults, 1000); // > 1 second
        if (!slowTests.isEmpty()) {
            report.append("\nSLOW TESTS (>1000ms):\n");
            for (McpTestFramework.McpTestResult slow : slowTests) {
                report.append(String.format("  ⚠️  %s (%s): %dms\n", 
                    slow.getTestName(), slow.getServiceName(), slow.getExecutionTimeMs()));
            }
        }
        
        report.append("\n").append("=".repeat(80)).append("\n");
        
        return report.toString();
    }

    /**
     * Generate detailed test report in JSON format.
     */
    public JsonNode generateDetailedJsonReport(McpTestFramework.McpTestSuiteResult suiteResult) {
        Map<String, Object> report = new HashMap<>();
        
        // Metadata
        report.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        report.put("reportType", "MCP_TEST_SUITE_DETAILED");
        
        // Overall statistics
        Map<String, Object> overall = new HashMap<>();
        overall.put("totalTests", suiteResult.getTotalCount());
        overall.put("passedTests", suiteResult.getPassedCount());
        overall.put("failedTests", suiteResult.getFailedCount());
        overall.put("successRate", suiteResult.getSuccessRate());
        overall.put("totalExecutionTimeMs", suiteResult.getTotalExecutionTimeMs());
        report.put("overall", overall);
        
        // Service breakdown
        Map<String, Object> serviceBreakdown = new HashMap<>();
        Map<String, List<McpTestFramework.McpTestResult>> resultsByService = suiteResult.getResultsByService();
        
        for (Map.Entry<String, List<McpTestFramework.McpTestResult>> entry : resultsByService.entrySet()) {
            String serviceName = entry.getKey();
            List<McpTestFramework.McpTestResult> serviceResults = entry.getValue();
            
            Map<String, Object> serviceStats = calculateTestStatistics(serviceResults);
            serviceBreakdown.put(serviceName, serviceStats);
        }
        report.put("serviceBreakdown", serviceBreakdown);
        
        // All test results
        List<Map<String, Object>> testResults = new ArrayList<>();
        for (McpTestFramework.McpTestResult result : suiteResult.getAllResults()) {
            Map<String, Object> testResult = new HashMap<>();
            testResult.put("testName", result.getTestName());
            testResult.put("serviceName", result.getServiceName());
            testResult.put("toolName", result.getToolName());
            testResult.put("success", result.isSuccess());
            testResult.put("executionTimeMs", result.getExecutionTimeMs());
            testResult.put("errorMessage", result.getErrorMessage());
            testResult.put("response", result.getResponse());
            testResult.put("metadata", result.getMetadata());
            testResults.add(testResult);
        }
        report.put("testResults", testResults);
        
        return objectMapper.valueToTree(report);
    }

    /**
     * Save test report to file.
     */
    public void saveReportToFile(String content, String filename) {
        try {
            // Create reports directory if it doesn't exist
            File reportsDir = new File("test-reports");
            if (!reportsDir.exists()) {
                reportsDir.mkdirs();
            }
            
            // Generate timestamped filename
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            String fullFilename = String.format("test-reports/%s-%s", timestamp, filename);
            
            try (FileWriter writer = new FileWriter(fullFilename)) {
                writer.write(content);
            }
            
            log.info("Test report saved to: {}", fullFilename);
        } catch (IOException e) {
            log.error("Failed to save test report to file: {}", filename, e);
        }
    }

    /**
     * Create test comparison between runs.
     */
    public Map<String, Object> compareTestRuns(
            McpTestFramework.McpTestSuiteResult previousRun,
            McpTestFramework.McpTestSuiteResult currentRun) {
        
        Map<String, Object> comparison = new HashMap<>();
        
        // Overall comparison
        Map<String, Object> overallComparison = new HashMap<>();
        overallComparison.put("previousSuccessRate", previousRun.getSuccessRate());
        overallComparison.put("currentSuccessRate", currentRun.getSuccessRate());
        overallComparison.put("successRateChange", currentRun.getSuccessRate() - previousRun.getSuccessRate());
        overallComparison.put("previousExecutionTime", previousRun.getTotalExecutionTimeMs());
        overallComparison.put("currentExecutionTime", currentRun.getTotalExecutionTimeMs());
        overallComparison.put("executionTimeChange", currentRun.getTotalExecutionTimeMs() - previousRun.getTotalExecutionTimeMs());
        
        comparison.put("overall", overallComparison);
        
        // Newly failing tests
        Set<String> previousFailedTestNames = previousRun.getFailedTests().stream()
                .map(McpTestFramework.McpTestResult::getTestName)
                .collect(Collectors.toSet());
        
        Set<String> currentFailedTestNames = currentRun.getFailedTests().stream()
                .map(McpTestFramework.McpTestResult::getTestName)
                .collect(Collectors.toSet());
        
        Set<String> newlyFailing = new HashSet<>(currentFailedTestNames);
        newlyFailing.removeAll(previousFailedTestNames);
        
        Set<String> newlyPassing = new HashSet<>(previousFailedTestNames);
        newlyPassing.removeAll(currentFailedTestNames);
        
        comparison.put("newlyFailingTests", newlyFailing);
        comparison.put("newlyPassingTests", newlyPassing);
        
        return comparison;
    }

    /**
     * Validate test environment health.
     */
    public Map<String, Object> validateTestEnvironment() {
        Map<String, Object> health = new HashMap<>();
        
        // Check Java version
        String javaVersion = System.getProperty("java.version");
        health.put("javaVersion", javaVersion);
        health.put("javaVersionValid", javaVersion.startsWith("11") || javaVersion.startsWith("17"));
        
        // Check available memory
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        Map<String, Object> memory = new HashMap<>();
        memory.put("maxMemoryMB", maxMemory / (1024 * 1024));
        memory.put("usedMemoryMB", usedMemory / (1024 * 1024));
        memory.put("freeMemoryMB", freeMemory / (1024 * 1024));
        memory.put("memoryHealthy", usedMemory < maxMemory * 0.8); // Less than 80% used
        
        health.put("memory", memory);
        
        // Check file system
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        health.put("tempDirExists", tempDir.exists());
        health.put("tempDirWritable", tempDir.canWrite());
        
        return health;
    }

    /**
     * Format execution time in human-readable format.
     */
    public String formatExecutionTime(long milliseconds) {
        if (milliseconds < 1000) {
            return milliseconds + "ms";
        } else if (milliseconds < 60000) {
            return String.format("%.1fs", milliseconds / 1000.0);
        } else {
            long minutes = milliseconds / 60000;
            long seconds = (milliseconds % 60000) / 1000;
            return String.format("%dm %ds", minutes, seconds);
        }
    }

    /**
     * Create test execution context.
     */
    public Map<String, Object> createTestExecutionContext() {
        Map<String, Object> context = new HashMap<>();
        context.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        context.put("testFrameworkVersion", "1.0.0");
        context.put("environment", validateTestEnvironment());
        return context;
    }
}