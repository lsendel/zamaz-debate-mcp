package com.zamaz.mcp.common.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for testing and validating MCP client functionality.
 * Provides methods to verify inter-service communication and tool availability.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class McpClientTestUtil {

    private final McpServiceRegistry serviceRegistry;
    private final McpServiceClient mcpServiceClient;
    private final OrganizationServiceClient organizationServiceClient;
    private final ContextServiceClient contextServiceClient;
    private final LlmServiceClient llmServiceClient;

    /**
     * Test result for individual service operations.
     */
    public static class TestResult {
        private final String operation;
        private final boolean success;
        private final String message;
        private final long durationMs;
        private final Exception error;

        public TestResult(String operation, boolean success, String message, long durationMs, Exception error) {
            this.operation = operation;
            this.success = success;
            this.message = message;
            this.durationMs = durationMs;
            this.error = error;
        }

        public String getOperation() { return operation; }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public long getDurationMs() { return durationMs; }
        public Exception getError() { return error; }

        @Override
        public String toString() {
            return String.format("TestResult{operation='%s', success=%s, duration=%dms, message='%s'}", 
                               operation, success, durationMs, message);
        }
    }

    /**
     * Comprehensive test suite for all MCP services.
     *
     * @param authentication Authentication context for testing
     * @return Map of service names to test results
     */
    public Map<String, List<TestResult>> runComprehensiveTests(Authentication authentication) {
        Map<String, List<TestResult>> results = new HashMap<>();

        log.info("Starting comprehensive MCP client tests");

        // Test each service
        results.put("organization", testOrganizationService(authentication));
        results.put("context", testContextService(authentication));
        results.put("llm", testLlmService(authentication));
        results.put("serviceRegistry", testServiceRegistry());

        // Generate summary
        long totalTests = results.values().stream().mapToLong(List::size).sum();
        long successfulTests = results.values().stream()
            .flatMap(List::stream)
            .mapToLong(result -> result.isSuccess() ? 1 : 0)
            .sum();

        log.info("MCP client tests completed: {}/{} successful", successfulTests, totalTests);

        return results;
    }

    /**
     * Test organization service operations.
     */
    public List<TestResult> testOrganizationService(Authentication authentication) {
        List<TestResult> results = new ArrayList<>();

        // Test service availability
        results.add(testOperation("organization-availability", () -> {
            boolean available = organizationServiceClient.isOrganizationServiceAvailable();
            return available ? "Service is available" : "Service is not available";
        }));

        // Test listing tools
        results.add(testOperation("organization-list-tools", () -> {
            JsonNode tools = organizationServiceClient.getAvailableTools();
            int toolCount = tools.has("tools") ? tools.get("tools").size() : 0;
            return "Found " + toolCount + " tools";
        }));

        // Test getting organization (read operation)
        if (authentication != null) {
            results.add(testOperation("organization-get", () -> {
                JsonNode response = organizationServiceClient.getOrganization(authentication);
                boolean success = response.has("success") && response.get("success").asBoolean();
                return success ? "Successfully retrieved organization" : "Failed to retrieve organization";
            }));
        }

        return results;
    }

    /**
     * Test context service operations.
     */
    public List<TestResult> testContextService(Authentication authentication) {
        List<TestResult> results = new ArrayList<>();

        // Test service availability
        results.add(testOperation("context-availability", () -> {
            boolean available = contextServiceClient.isContextServiceAvailable();
            return available ? "Service is available" : "Service is not available";
        }));

        // Test listing tools
        results.add(testOperation("context-list-tools", () -> {
            JsonNode tools = contextServiceClient.getAvailableTools();
            int toolCount = tools.has("tools") ? tools.get("tools").size() : 0;
            return "Found " + toolCount + " tools";
        }));

        // Test search (read operation that doesn't require existing data)
        if (authentication != null) {
            results.add(testOperation("context-search", () -> {
                JsonNode response = contextServiceClient.searchContexts("test", authentication);
                boolean success = response.has("success") && response.get("success").asBoolean();
                return success ? "Search completed successfully" : "Search failed";
            }));
        }

        return results;
    }

    /**
     * Test LLM service operations.
     */
    public List<TestResult> testLlmService(Authentication authentication) {
        List<TestResult> results = new ArrayList<>();

        // Test service availability
        results.add(testOperation("llm-availability", () -> {
            boolean available = llmServiceClient.isLlmServiceAvailable();
            return available ? "Service is available" : "Service is not available";
        }));

        // Test listing tools
        results.add(testOperation("llm-list-tools", () -> {
            JsonNode tools = llmServiceClient.getAvailableTools();
            int toolCount = tools.has("tools") ? tools.get("tools").size() : 0;
            return "Found " + toolCount + " tools";
        }));

        // Test listing providers
        results.add(testOperation("llm-list-providers", () -> {
            JsonNode response = llmServiceClient.listProviders();
            int providerCount = response.has("providers") ? response.get("providers").size() : 0;
            return "Found " + providerCount + " providers";
        }));

        // Test provider status
        results.add(testOperation("llm-provider-status", () -> {
            JsonNode response = llmServiceClient.getProviderStatus("claude");
            String status = response.has("status") ? response.get("status").asText() : "unknown";
            return "Claude provider status: " + status;
        }));

        return results;
    }

    /**
     * Test service registry functionality.
     */
    public List<TestResult> testServiceRegistry() {
        List<TestResult> results = new ArrayList<>();

        // Test service URL resolution
        results.add(testOperation("registry-service-urls", () -> {
            Map<String, String> urls = serviceRegistry.getAllServiceUrls();
            return "Configured " + urls.size() + " service URLs";
        }));

        // Test health status checking
        results.add(testOperation("registry-health-status", () -> {
            Map<String, Boolean> healthStatus = serviceRegistry.getServiceHealthStatus();
            long healthyServices = healthStatus.values().stream().mapToLong(healthy -> healthy ? 1 : 0).sum();
            return healthyServices + "/" + healthStatus.size() + " services healthy";
        }));

        // Test service discovery
        results.add(testOperation("registry-discovery", () -> {
            Map<String, Object> discoveryInfo = serviceRegistry.getServiceDiscoveryInfo();
            Object healthyCount = discoveryInfo.get("healthyServices");
            Object totalCount = discoveryInfo.get("totalServices");
            return "Service discovery: " + healthyCount + "/" + totalCount + " services";
        }));

        return results;
    }

    /**
     * Test connectivity to all configured services.
     *
     * @return Map of service names to connectivity status
     */
    public Map<String, Boolean> testServiceConnectivity() {
        Map<String, Boolean> connectivity = new HashMap<>();

        for (McpServiceRegistry.McpService service : McpServiceRegistry.McpService.values()) {
            try {
                boolean isAvailable = serviceRegistry.isServiceAvailable(service);
                connectivity.put(service.getServiceName(), isAvailable);
                log.debug("Service {} connectivity: {}", service.getServiceName(), isAvailable);
            } catch (Exception e) {
                connectivity.put(service.getServiceName(), false);
                log.debug("Service {} connectivity test failed: {}", service.getServiceName(), e.getMessage());
            }
        }

        return connectivity;
    }

    /**
     * Perform a quick health check of all services.
     *
     * @return Overall system health status
     */
    public boolean performHealthCheck() {
        Map<String, Boolean> connectivity = testServiceConnectivity();
        long healthyServices = connectivity.values().stream().mapToLong(healthy -> healthy ? 1 : 0).sum();
        long totalServices = connectivity.size();

        log.info("Health check completed: {}/{} services healthy", healthyServices, totalServices);
        
        // Consider system healthy if at least 75% of services are available
        return (double) healthyServices / totalServices >= 0.75;
    }

    /**
     * Test parallel calls to multiple services.
     *
     * @param authentication Authentication context
     * @return Test results for parallel operations
     */
    public List<TestResult> testParallelCalls(Authentication authentication) {
        List<TestResult> results = new ArrayList<>();

        List<CompletableFuture<TestResult>> futures = new ArrayList<>();

        // Create parallel tasks
        futures.add(CompletableFuture.supplyAsync(() -> 
            testOperation("parallel-organization", () -> {
                JsonNode response = organizationServiceClient.getOrganization(authentication);
                return "Organization call completed";
            })
        ));

        futures.add(CompletableFuture.supplyAsync(() -> 
            testOperation("parallel-llm-providers", () -> {
                JsonNode response = llmServiceClient.listProviders();
                return "LLM providers call completed";
            })
        ));

        futures.add(CompletableFuture.supplyAsync(() -> 
            testOperation("parallel-context-search", () -> {
                JsonNode response = contextServiceClient.searchContexts("test", authentication);
                return "Context search completed";
            })
        ));

        // Wait for all to complete with timeout
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        
        try {
            allOf.get(30, TimeUnit.SECONDS);
            futures.forEach(future -> {
                try {
                    results.add(future.get());
                } catch (Exception e) {
                    results.add(new TestResult("parallel-operation", false, "Failed: " + e.getMessage(), 0, e));
                }
            });
        } catch (Exception e) {
            log.error("Parallel test timeout or error: {}", e.getMessage());
            results.add(new TestResult("parallel-timeout", false, "Parallel operations timed out", 30000, e));
        }

        return results;
    }

    /**
     * Helper method to execute and time test operations.
     */
    private TestResult testOperation(String operationName, TestOperation operation) {
        long startTime = System.currentTimeMillis();
        try {
            String message = operation.execute();
            long duration = System.currentTimeMillis() - startTime;
            log.debug("Test operation '{}' succeeded in {}ms: {}", operationName, duration, message);
            return new TestResult(operationName, true, message, duration, null);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.debug("Test operation '{}' failed in {}ms: {}", operationName, duration, e.getMessage());
            return new TestResult(operationName, false, e.getMessage(), duration, e);
        }
    }

    @FunctionalInterface
    private interface TestOperation {
        String execute() throws Exception;
    }
}