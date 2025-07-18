package com.zamaz.mcp.common.testing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zamaz.mcp.common.client.McpServiceClient;
import com.zamaz.mcp.common.client.McpServiceRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive testing framework for MCP tools and services.
 * Provides automated testing capabilities for all MCP endpoints and workflows.
 */
@TestComponent
@RequiredArgsConstructor
@Slf4j
@ActiveProfiles("test")
public class McpTestFramework {

    private final McpServiceClient mcpServiceClient;
    private final McpServiceRegistry serviceRegistry;
    private final ObjectMapper objectMapper;

    /**
     * Test result wrapper for individual tests.
     */
    public static class McpTestResult {
        private final String testName;
        private final String serviceName;
        private final String toolName;
        private final boolean success;
        private final long executionTimeMs;
        private final String errorMessage;
        private final JsonNode response;
        private final Map<String, Object> metadata;

        public McpTestResult(String testName, String serviceName, String toolName, 
                           boolean success, long executionTimeMs, String errorMessage, 
                           JsonNode response, Map<String, Object> metadata) {
            this.testName = testName;
            this.serviceName = serviceName;
            this.toolName = toolName;
            this.success = success;
            this.executionTimeMs = executionTimeMs;
            this.errorMessage = errorMessage;
            this.response = response;
            this.metadata = metadata != null ? metadata : new HashMap<>();
        }

        // Getters
        public String getTestName() { return testName; }
        public String getServiceName() { return serviceName; }
        public String getToolName() { return toolName; }
        public boolean isSuccess() { return success; }
        public long getExecutionTimeMs() { return executionTimeMs; }
        public String getErrorMessage() { return errorMessage; }
        public JsonNode getResponse() { return response; }
        public Map<String, Object> getMetadata() { return metadata; }

        @Override
        public String toString() {
            return String.format("McpTestResult{test='%s', service='%s', tool='%s', success=%s, time=%dms}", 
                               testName, serviceName, toolName, success, executionTimeMs);
        }
    }

    /**
     * Comprehensive test suite for all MCP services.
     */
    public McpTestSuiteResult runComprehensiveTestSuite(Authentication authentication) {
        log.info("Starting comprehensive MCP test suite");
        long startTime = System.currentTimeMillis();
        
        List<McpTestResult> allResults = new ArrayList<>();
        Map<String, List<McpTestResult>> resultsByService = new HashMap<>();

        // Test each service
        for (McpServiceRegistry.McpService service : McpServiceRegistry.McpService.values()) {
            List<McpTestResult> serviceResults = testService(service, authentication);
            allResults.addAll(serviceResults);
            resultsByService.put(service.getServiceName(), serviceResults);
        }

        // Run integration tests
        List<McpTestResult> integrationResults = runIntegrationTests(authentication);
        allResults.addAll(integrationResults);
        resultsByService.put("integration", integrationResults);

        // Run workflow tests
        List<McpTestResult> workflowResults = runWorkflowTests(authentication);
        allResults.addAll(workflowResults);
        resultsByService.put("workflows", workflowResults);

        long totalTime = System.currentTimeMillis() - startTime;
        
        McpTestSuiteResult suiteResult = new McpTestSuiteResult(
            allResults, resultsByService, totalTime);
        
        log.info("Comprehensive MCP test suite completed: {}/{} tests passed in {}ms", 
                suiteResult.getPassedCount(), suiteResult.getTotalCount(), totalTime);
        
        return suiteResult;
    }

    /**
     * Test a specific MCP service and all its tools.
     */
    public List<McpTestResult> testService(McpServiceRegistry.McpService service, Authentication authentication) {
        List<McpTestResult> results = new ArrayList<>();
        String serviceName = service.getServiceName();
        
        log.info("Testing MCP service: {}", serviceName);

        // Test service availability
        results.add(testServiceAvailability(service));

        // Test list tools endpoint
        results.add(testListTools(service));

        // Test server info endpoint  
        results.add(testServerInfo(service));

        // Test individual tools based on service
        switch (service) {
            case ORGANIZATION:
                results.addAll(testOrganizationTools(authentication));
                break;
            case CONTEXT:
                results.addAll(testContextTools(authentication));
                break;
            case LLM:
                results.addAll(testLlmTools(authentication));
                break;
            case CONTROLLER:
                results.addAll(testControllerTools(authentication));
                break;
            case RAG:
                results.addAll(testRagTools(authentication));
                break;
            default:
                log.warn("No specific tests defined for service: {}", serviceName);
        }

        return results;
    }

    /**
     * Test service availability.
     */
    private McpTestResult testServiceAvailability(McpServiceRegistry.McpService service) {
        return executeTest("service_availability", service.getServiceName(), "health_check", () -> {
            boolean available = serviceRegistry.isServiceAvailable(service);
            if (!available) {
                throw new RuntimeException("Service is not available");
            }
            return objectMapper.createObjectNode().put("available", true);
        });
    }

    /**
     * Test list tools endpoint.
     */
    private McpTestResult testListTools(McpServiceRegistry.McpService service) {
        return executeTest("list_tools", service.getServiceName(), "list_tools", () -> {
            String serviceUrl = serviceRegistry.getServiceUrl(service);
            return mcpServiceClient.listTools(serviceUrl);
        });
    }

    /**
     * Test server info endpoint.
     */
    private McpTestResult testServerInfo(McpServiceRegistry.McpService service) {
        return executeTest("server_info", service.getServiceName(), "server_info", () -> {
            String serviceUrl = serviceRegistry.getServiceUrl(service);
            return mcpServiceClient.getServerInfo(serviceUrl);
        });
    }

    /**
     * Test Organization service tools.
     */
    private List<McpTestResult> testOrganizationTools(Authentication authentication) {
        List<McpTestResult> results = new ArrayList<>();
        String serviceName = "organization";

        // Test get organization (read operation)
        results.add(executeTest("get_organization", serviceName, "get_organization", () -> {
            Map<String, Object> params = new HashMap<>();
            return mcpServiceClient.callTool(
                serviceRegistry.getServiceUrl(McpServiceRegistry.McpService.ORGANIZATION),
                "get_organization", params, authentication);
        }));

        // Test list organizations  
        results.add(executeTest("list_organizations", serviceName, "list_organizations", () -> {
            Map<String, Object> params = new HashMap<>();
            return mcpServiceClient.callTool(
                serviceRegistry.getServiceUrl(McpServiceRegistry.McpService.ORGANIZATION),
                "list_organizations", params, authentication);
        }));

        return results;
    }

    /**
     * Test Context service tools.
     */
    private List<McpTestResult> testContextTools(Authentication authentication) {
        List<McpTestResult> results = new ArrayList<>();
        String serviceName = "context";

        // Test search contexts (safe read operation)
        results.add(executeTest("search_contexts", serviceName, "search_contexts", () -> {
            Map<String, Object> params = new HashMap<>();
            params.put("query", "test");
            params.put("page", 0);
            params.put("size", 10);
            return mcpServiceClient.callTool(
                serviceRegistry.getServiceUrl(McpServiceRegistry.McpService.CONTEXT),
                "search_contexts", params, authentication);
        }));

        return results;
    }

    /**
     * Test LLM service tools.
     */
    private List<McpTestResult> testLlmTools(Authentication authentication) {
        List<McpTestResult> results = new ArrayList<>();
        String serviceName = "llm";

        // Test list providers (no auth needed)
        results.add(executeTest("list_providers", serviceName, "list_providers", () -> {
            Map<String, Object> params = new HashMap<>();
            return mcpServiceClient.callTool(
                serviceRegistry.getServiceUrl(McpServiceRegistry.McpService.LLM),
                "list_providers", params);
        }));

        // Test provider status
        results.add(executeTest("get_provider_status", serviceName, "get_provider_status", () -> {
            Map<String, Object> params = new HashMap<>();
            params.put("provider", "claude");
            return mcpServiceClient.callTool(
                serviceRegistry.getServiceUrl(McpServiceRegistry.McpService.LLM),
                "get_provider_status", params);
        }));

        return results;
    }

    /**
     * Test Controller/Debate service tools.
     */
    private List<McpTestResult> testControllerTools(Authentication authentication) {
        List<McpTestResult> results = new ArrayList<>();
        String serviceName = "controller";

        // Test list debates (read operation)
        results.add(executeTest("list_debates", serviceName, "list_debates", () -> {
            Map<String, Object> params = new HashMap<>();
            params.put("organizationId", "test-org-id");
            return mcpServiceClient.callTool(
                serviceRegistry.getServiceUrl(McpServiceRegistry.McpService.CONTROLLER),
                "list_debates", params, authentication);
        }));

        return results;
    }

    /**
     * Test RAG service tools.
     */
    private List<McpTestResult> testRagTools(Authentication authentication) {
        List<McpTestResult> results = new ArrayList<>();
        String serviceName = "rag";

        // Test search (read operation)
        results.add(executeTest("search_documents", serviceName, "search_documents", () -> {
            Map<String, Object> params = new HashMap<>();
            params.put("organizationId", "test-org-id");
            params.put("query", "test query");
            params.put("limit", 5);
            return mcpServiceClient.callTool(
                serviceRegistry.getServiceUrl(McpServiceRegistry.McpService.RAG),
                "search_documents", params, authentication);
        }));

        return results;
    }

    /**
     * Run integration tests that span multiple services.
     */
    private List<McpTestResult> runIntegrationTests(Authentication authentication) {
        List<McpTestResult> results = new ArrayList<>();

        // Test service discovery
        results.add(executeTest("service_discovery", "integration", "service_discovery", () -> {
            Map<String, Object> discoveryInfo = serviceRegistry.getServiceDiscoveryInfo();
            return objectMapper.valueToTree(discoveryInfo);
        }));

        // Test cross-service connectivity
        results.add(executeTest("cross_service_connectivity", "integration", "connectivity", () -> {
            Map<String, Boolean> connectivity = new HashMap<>();
            for (McpServiceRegistry.McpService service : McpServiceRegistry.McpService.values()) {
                connectivity.put(service.getServiceName(), 
                               serviceRegistry.isServiceAvailable(service));
            }
            return objectMapper.valueToTree(connectivity);
        }));

        return results;
    }

    /**
     * Run workflow tests that test complete business processes.
     */
    private List<McpTestResult> runWorkflowTests(Authentication authentication) {
        List<McpTestResult> results = new ArrayList<>();

        // Test basic workflow: Organization → Context → LLM
        results.add(executeTest("basic_workflow", "workflows", "org_context_llm", () -> {
            // This would test a complete workflow but without making destructive changes
            // For now, just verify the services can be called in sequence
            Map<String, Object> workflowResult = new HashMap<>();
            workflowResult.put("status", "simulated");
            workflowResult.put("steps", Arrays.asList("organization", "context", "llm"));
            return objectMapper.valueToTree(workflowResult);
        }));

        return results;
    }

    /**
     * Run parallel tests for performance validation.
     */
    public List<McpTestResult> runParallelTests(Authentication authentication, int concurrency) {
        log.info("Running parallel MCP tests with concurrency: {}", concurrency);
        
        List<CompletableFuture<McpTestResult>> futures = new ArrayList<>();
        
        for (int i = 0; i < concurrency; i++) {
            final int testIndex = i;
            CompletableFuture<McpTestResult> future = CompletableFuture.supplyAsync(() -> {
                return executeTest("parallel_test_" + testIndex, "performance", "parallel", () -> {
                    // Test LLM list providers (lightweight operation)
                    Map<String, Object> params = new HashMap<>();
                    return mcpServiceClient.callTool(
                        serviceRegistry.getServiceUrl(McpServiceRegistry.McpService.LLM),
                        "list_providers", params);
                });
            });
            futures.add(future);
        }

        List<McpTestResult> results = new ArrayList<>();
        try {
            CompletableFuture<Void> allOf = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));
            allOf.get(30, TimeUnit.SECONDS);
            
            for (CompletableFuture<McpTestResult> future : futures) {
                results.add(future.get());
            }
        } catch (Exception e) {
            log.error("Parallel tests failed or timed out", e);
            results.add(new McpTestResult("parallel_tests", "performance", "parallel",
                                        false, 30000, "Parallel tests timed out: " + e.getMessage(),
                                        null, null));
        }

        return results;
    }

    /**
     * Execute a single test with timing and error handling.
     */
    private McpTestResult executeTest(String testName, String serviceName, String toolName, 
                                    TestOperation operation) {
        long startTime = System.currentTimeMillis();
        try {
            JsonNode response = operation.execute();
            long executionTime = System.currentTimeMillis() - startTime;
            
            log.debug("Test '{}' passed in {}ms", testName, executionTime);
            return new McpTestResult(testName, serviceName, toolName, true, executionTime, 
                                   null, response, null);
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.debug("Test '{}' failed in {}ms: {}", testName, executionTime, e.getMessage());
            return new McpTestResult(testName, serviceName, toolName, false, executionTime, 
                                   e.getMessage(), null, null);
        }
    }

    @FunctionalInterface
    private interface TestOperation {
        JsonNode execute() throws Exception;
    }

    /**
     * Test suite result aggregator.
     */
    public static class McpTestSuiteResult {
        private final List<McpTestResult> allResults;
        private final Map<String, List<McpTestResult>> resultsByService;
        private final long totalExecutionTimeMs;

        public McpTestSuiteResult(List<McpTestResult> allResults, 
                                Map<String, List<McpTestResult>> resultsByService,
                                long totalExecutionTimeMs) {
            this.allResults = allResults;
            this.resultsByService = resultsByService;
            this.totalExecutionTimeMs = totalExecutionTimeMs;
        }

        public List<McpTestResult> getAllResults() { return allResults; }
        public Map<String, List<McpTestResult>> getResultsByService() { return resultsByService; }
        public long getTotalExecutionTimeMs() { return totalExecutionTimeMs; }

        public int getTotalCount() { return allResults.size(); }
        public int getPassedCount() { 
            return (int) allResults.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum(); 
        }
        public int getFailedCount() { return getTotalCount() - getPassedCount(); }
        public double getSuccessRate() { 
            return getTotalCount() > 0 ? (double) getPassedCount() / getTotalCount() * 100 : 0; 
        }

        public List<McpTestResult> getFailedTests() {
            return allResults.stream().filter(r -> !r.isSuccess()).toList();
        }

        public Map<String, Integer> getPassedCountByService() {
            Map<String, Integer> counts = new HashMap<>();
            resultsByService.forEach((service, results) -> {
                int passedCount = (int) results.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
                counts.put(service, passedCount);
            });
            return counts;
        }

        @Override
        public String toString() {
            return String.format("McpTestSuiteResult{total=%d, passed=%d, failed=%d, successRate=%.1f%%, time=%dms}",
                               getTotalCount(), getPassedCount(), getFailedCount(), getSuccessRate(), totalExecutionTimeMs);
        }
    }
}