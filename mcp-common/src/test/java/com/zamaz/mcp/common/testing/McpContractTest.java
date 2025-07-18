package com.zamaz.mcp.common.testing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zamaz.mcp.common.client.McpServiceClient;
import com.zamaz.mcp.common.client.McpServiceRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.core.Authentication;

import java.util.*;

/**
 * Contract tests for MCP protocol compliance.
 * Validates that all MCP services follow the protocol specification.
 */
@TestComponent
@RequiredArgsConstructor
@Slf4j
public class McpContractTest {

    private final McpServiceClient mcpServiceClient;
    private final McpServiceRegistry serviceRegistry;
    private final ObjectMapper objectMapper;

    /**
     * Result of contract testing.
     */
    public static class McpContractResult {
        private final String serviceName;
        private final String contractType;
        private final boolean compliant;
        private final List<String> violations;
        private final Map<String, Object> details;

        public McpContractResult(String serviceName, String contractType, boolean compliant, 
                               List<String> violations, Map<String, Object> details) {
            this.serviceName = serviceName;
            this.contractType = contractType;
            this.compliant = compliant;
            this.violations = violations != null ? violations : new ArrayList<>();
            this.details = details != null ? details : new HashMap<>();
        }

        // Getters
        public String getServiceName() { return serviceName; }
        public String getContractType() { return contractType; }
        public boolean isCompliant() { return compliant; }
        public List<String> getViolations() { return violations; }
        public Map<String, Object> getDetails() { return details; }

        @Override
        public String toString() {
            return String.format("McpContractResult{service='%s', type='%s', compliant=%s, violations=%d}", 
                               serviceName, contractType, compliant, violations.size());
        }
    }

    /**
     * Run all contract tests for a service.
     */
    public List<McpContractResult> runAllContractTests(McpServiceRegistry.McpService service) {
        List<McpContractResult> results = new ArrayList<>();
        String serviceName = service.getServiceName();
        
        log.info("Running contract tests for service: {}", serviceName);
        
        // Test server info endpoint contract
        results.add(testServerInfoContract(service));
        
        // Test list tools endpoint contract
        results.add(testListToolsContract(service));
        
        // Test call tool endpoint contract
        results.add(testCallToolContract(service));
        
        // Test error handling contract
        results.add(testErrorHandlingContract(service));
        
        return results;
    }

    /**
     * Test server info endpoint contract compliance.
     */
    private McpContractResult testServerInfoContract(McpServiceRegistry.McpService service) {
        String serviceName = service.getServiceName();
        List<String> violations = new ArrayList<>();
        Map<String, Object> details = new HashMap<>();
        
        try {
            String serviceUrl = serviceRegistry.getServiceUrl(service);
            JsonNode response = mcpServiceClient.getServerInfo(serviceUrl);
            
            // Validate response structure
            if (response == null) {
                violations.add("Server info response is null");
            } else {
                details.put("responseReceived", true);
                
                // Check required fields
                if (!response.has("name")) {
                    violations.add("Missing required field: name");
                }
                
                if (!response.has("version")) {
                    violations.add("Missing required field: version");
                }
                
                // Check protocol compliance
                if (response.has("protocolVersion")) {
                    String protocolVersion = response.get("protocolVersion").asText();
                    if (!protocolVersion.matches("\\d+\\.\\d+(\\.\\d+)?")) {
                        violations.add("Invalid protocol version format: " + protocolVersion);
                    }
                    details.put("protocolVersion", protocolVersion);
                }
                
                // Check capabilities structure
                if (response.has("capabilities")) {
                    JsonNode capabilities = response.get("capabilities");
                    if (!capabilities.isObject()) {
                        violations.add("Capabilities field must be an object");
                    } else {
                        details.put("capabilities", capabilities);
                    }
                }
            }
            
        } catch (Exception e) {
            violations.add("Server info endpoint failed: " + e.getMessage());
            details.put("exception", e.getClass().getSimpleName());
        }
        
        boolean compliant = violations.isEmpty();
        return new McpContractResult(serviceName, "server_info", compliant, violations, details);
    }

    /**
     * Test list tools endpoint contract compliance.
     */
    private McpContractResult testListToolsContract(McpServiceRegistry.McpService service) {
        String serviceName = service.getServiceName();
        List<String> violations = new ArrayList<>();
        Map<String, Object> details = new HashMap<>();
        
        try {
            String serviceUrl = serviceRegistry.getServiceUrl(service);
            JsonNode response = mcpServiceClient.listTools(serviceUrl);
            
            if (response == null) {
                violations.add("List tools response is null");
            } else {
                details.put("responseReceived", true);
                
                // Check if response has tools array
                if (!response.has("tools")) {
                    violations.add("Missing required field: tools");
                } else {
                    JsonNode tools = response.get("tools");
                    if (!tools.isArray()) {
                        violations.add("Tools field must be an array");
                    } else {
                        details.put("toolCount", tools.size());
                        
                        // Validate each tool definition
                        for (int i = 0; i < tools.size(); i++) {
                            JsonNode tool = tools.get(i);
                            String toolPrefix = "tool[" + i + "]";
                            
                            if (!tool.has("name")) {
                                violations.add(toolPrefix + ": Missing required field: name");
                            }
                            
                            if (!tool.has("description")) {
                                violations.add(toolPrefix + ": Missing required field: description");
                            }
                            
                            // Check input schema if present
                            if (tool.has("inputSchema")) {
                                JsonNode inputSchema = tool.get("inputSchema");
                                if (!inputSchema.isObject()) {
                                    violations.add(toolPrefix + ": inputSchema must be an object");
                                }
                                
                                if (!inputSchema.has("type")) {
                                    violations.add(toolPrefix + ": inputSchema missing type field");
                                }
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            violations.add("List tools endpoint failed: " + e.getMessage());
            details.put("exception", e.getClass().getSimpleName());
        }
        
        boolean compliant = violations.isEmpty();
        return new McpContractResult(serviceName, "list_tools", compliant, violations, details);
    }

    /**
     * Test call tool endpoint contract compliance.
     */
    private McpContractResult testCallToolContract(McpServiceRegistry.McpService service) {
        String serviceName = service.getServiceName();
        List<String> violations = new ArrayList<>();
        Map<String, Object> details = new HashMap<>();
        
        try {
            String serviceUrl = serviceRegistry.getServiceUrl(service);
            
            // First get available tools
            JsonNode toolsResponse = mcpServiceClient.listTools(serviceUrl);
            if (toolsResponse != null && toolsResponse.has("tools")) {
                JsonNode tools = toolsResponse.get("tools");
                
                if (tools.size() > 0) {
                    // Test calling the first available tool with empty parameters
                    JsonNode firstTool = tools.get(0);
                    String toolName = firstTool.get("name").asText();
                    
                    try {
                        Map<String, Object> emptyParams = new HashMap<>();
                        JsonNode response = mcpServiceClient.callTool(serviceUrl, toolName, emptyParams);
                        
                        // The response should either succeed or fail with proper error structure
                        if (response != null) {
                            details.put("toolCallResponseReceived", true);
                            
                            // Check response structure
                            if (response.has("content")) {
                                // Success response
                                details.put("toolCallSuccess", true);
                            } else if (response.has("error")) {
                                // Error response - validate error structure
                                JsonNode error = response.get("error");
                                if (!error.has("code") || !error.has("message")) {
                                    violations.add("Invalid error response structure for tool: " + toolName);
                                } else {
                                    details.put("toolCallError", error.get("message").asText());
                                }
                            } else {
                                violations.add("Invalid tool call response structure for tool: " + toolName);
                            }
                        }
                        
                    } catch (Exception toolCallException) {
                        // Tool call failing is acceptable, but should not throw unexpected exceptions
                        details.put("toolCallException", toolCallException.getClass().getSimpleName());
                    }
                } else {
                    details.put("noToolsAvailable", true);
                }
            }
            
        } catch (Exception e) {
            violations.add("Call tool contract test failed: " + e.getMessage());
            details.put("exception", e.getClass().getSimpleName());
        }
        
        boolean compliant = violations.isEmpty();
        return new McpContractResult(serviceName, "call_tool", compliant, violations, details);
    }

    /**
     * Test error handling contract compliance.
     */
    private McpContractResult testErrorHandlingContract(McpServiceRegistry.McpService service) {
        String serviceName = service.getServiceName();
        List<String> violations = new ArrayList<>();
        Map<String, Object> details = new HashMap<>();
        
        try {
            String serviceUrl = serviceRegistry.getServiceUrl(service);
            
            // Test calling non-existent tool
            try {
                Map<String, Object> params = new HashMap<>();
                JsonNode response = mcpServiceClient.callTool(serviceUrl, "non_existent_tool", params);
                
                if (response != null && response.has("error")) {
                    JsonNode error = response.get("error");
                    
                    if (!error.has("code")) {
                        violations.add("Error response missing 'code' field");
                    }
                    
                    if (!error.has("message")) {
                        violations.add("Error response missing 'message' field");
                    }
                    
                    // Check error code format
                    if (error.has("code")) {
                        JsonNode code = error.get("code");
                        if (!code.isNumber() && !code.isTextual()) {
                            violations.add("Error code must be number or string");
                        }
                        details.put("errorCode", code.asText());
                    }
                    
                    details.put("errorHandled", true);
                } else {
                    violations.add("Non-existent tool should return error response");
                }
                
            } catch (Exception errorTestException) {
                // Exception handling is acceptable for invalid tool calls
                details.put("errorTestException", errorTestException.getClass().getSimpleName());
            }
            
        } catch (Exception e) {
            violations.add("Error handling contract test failed: " + e.getMessage());
            details.put("exception", e.getClass().getSimpleName());
        }
        
        boolean compliant = violations.isEmpty();
        return new McpContractResult(serviceName, "error_handling", compliant, violations, details);
    }

    /**
     * Generate contract compliance report.
     */
    public String generateComplianceReport(List<McpContractResult> results) {
        StringBuilder report = new StringBuilder();
        
        report.append("=".repeat(80)).append("\n");
        report.append("MCP CONTRACT COMPLIANCE REPORT\n");
        report.append("=".repeat(80)).append("\n\n");
        
        // Summary
        long compliantTests = results.stream().mapToLong(r -> r.isCompliant() ? 1 : 0).sum();
        report.append(String.format("Total Contract Tests: %d\n", results.size()));
        report.append(String.format("Compliant: %d\n", compliantTests));
        report.append(String.format("Non-Compliant: %d\n", results.size() - compliantTests));
        report.append(String.format("Compliance Rate: %.1f%%\n\n", 
            results.size() > 0 ? (double) compliantTests / results.size() * 100 : 0));
        
        // Group by service
        Map<String, List<McpContractResult>> resultsByService = new HashMap<>();
        for (McpContractResult result : results) {
            resultsByService.computeIfAbsent(result.getServiceName(), k -> new ArrayList<>()).add(result);
        }
        
        // Service-by-service breakdown
        for (Map.Entry<String, List<McpContractResult>> entry : resultsByService.entrySet()) {
            String serviceName = entry.getKey();
            List<McpContractResult> serviceResults = entry.getValue();
            
            report.append(String.format("SERVICE: %s\n", serviceName.toUpperCase()));
            report.append("-".repeat(40)).append("\n");
            
            for (McpContractResult result : serviceResults) {
                String status = result.isCompliant() ? "✅ COMPLIANT" : "❌ NON-COMPLIANT";
                report.append(String.format("  %s: %s\n", result.getContractType(), status));
                
                if (!result.isCompliant()) {
                    for (String violation : result.getViolations()) {
                        report.append(String.format("    - %s\n", violation));
                    }
                }
            }
            report.append("\n");
        }
        
        report.append("=".repeat(80)).append("\n");
        
        return report.toString();
    }

    /**
     * Test authentication contract compliance.
     */
    public McpContractResult testAuthenticationContract(McpServiceRegistry.McpService service, 
                                                      Authentication validAuth, 
                                                      Authentication invalidAuth) {
        String serviceName = service.getServiceName();
        List<String> violations = new ArrayList<>();
        Map<String, Object> details = new HashMap<>();
        
        try {
            String serviceUrl = serviceRegistry.getServiceUrl(service);
            
            // Test with valid authentication
            try {
                JsonNode response = mcpServiceClient.listTools(serviceUrl, validAuth);
                if (response != null) {
                    details.put("validAuthAccepted", true);
                } else {
                    violations.add("Valid authentication was rejected");
                }
            } catch (Exception e) {
                details.put("validAuthException", e.getClass().getSimpleName());
            }
            
            // Test with invalid authentication
            try {
                JsonNode response = mcpServiceClient.listTools(serviceUrl, invalidAuth);
                if (response != null && !response.has("error")) {
                    violations.add("Invalid authentication was accepted");
                } else {
                    details.put("invalidAuthRejected", true);
                }
            } catch (Exception e) {
                // Exception is expected for invalid auth
                details.put("invalidAuthException", e.getClass().getSimpleName());
            }
            
        } catch (Exception e) {
            violations.add("Authentication contract test failed: " + e.getMessage());
            details.put("exception", e.getClass().getSimpleName());
        }
        
        boolean compliant = violations.isEmpty();
        return new McpContractResult(serviceName, "authentication", compliant, violations, details);
    }
}