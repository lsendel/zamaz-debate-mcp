package com.zamaz.mcp.common.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry for MCP service URLs and discovery.
 * Provides centralized management of service endpoints for inter-service communication.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class McpServiceRegistry {

    // Default service URLs - can be overridden by properties
    @Value("${mcp.services.organization.url:http://localhost:5005}")
    private String organizationServiceUrl;

    @Value("${mcp.services.context.url:http://localhost:5007}")
    private String contextServiceUrl;

    @Value("${mcp.services.llm.url:http://localhost:5002}")
    private String llmServiceUrl;

    @Value("${mcp.services.controller.url:http://localhost:5013}")
    private String controllerServiceUrl;

    @Value("${mcp.services.rag.url:http://localhost:5004}")
    private String ragServiceUrl;

    @Value("${mcp.services.template.url:http://localhost:5006}")
    private String templateServiceUrl;

    @Value("${mcp.services.gateway.url:http://localhost:8080}")
    private String gatewayServiceUrl;

    private final McpServiceClient mcpServiceClient;

    /**
     * Known MCP services in the system.
     */
    public enum McpService {
        ORGANIZATION("organization"),
        CONTEXT("context"),
        LLM("llm"),
        CONTROLLER("controller"),
        RAG("rag"),
        TEMPLATE("template"),
        GATEWAY("gateway");

        private final String serviceName;

        McpService(String serviceName) {
            this.serviceName = serviceName;
        }

        public String getServiceName() {
            return serviceName;
        }
    }

    /**
     * Get the base URL for a specific MCP service.
     *
     * @param service The MCP service
     * @return Service base URL
     */
    public String getServiceUrl(McpService service) {
        return switch (service) {
            case ORGANIZATION -> organizationServiceUrl;
            case CONTEXT -> contextServiceUrl;
            case LLM -> llmServiceUrl;
            case CONTROLLER -> controllerServiceUrl;
            case RAG -> ragServiceUrl;
            case TEMPLATE -> templateServiceUrl;
            case GATEWAY -> gatewayServiceUrl;
        };
    }

    /**
     * Get service URL by service name string.
     *
     * @param serviceName Name of the service
     * @return Optional service URL
     */
    public Optional<String> getServiceUrl(String serviceName) {
        try {
            McpService service = McpService.valueOf(serviceName.toUpperCase());
            return Optional.of(getServiceUrl(service));
        } catch (IllegalArgumentException e) {
            log.warn("Unknown service name: {}", serviceName);
            return Optional.empty();
        }
    }

    /**
     * Check if a service is available and responding.
     *
     * @param service The MCP service to check
     * @return true if service is available
     */
    public boolean isServiceAvailable(McpService service) {
        String serviceUrl = getServiceUrl(service);
        return mcpServiceClient.isServiceAvailable(serviceUrl);
    }

    /**
     * Get all service URLs as a map.
     *
     * @return Map of service names to URLs
     */
    public Map<String, String> getAllServiceUrls() {
        Map<String, String> services = new HashMap<>();
        for (McpService service : McpService.values()) {
            services.put(service.getServiceName(), getServiceUrl(service));
        }
        return services;
    }

    /**
     * Get health status of all services.
     *
     * @return Map of service names to health status
     */
    public Map<String, Boolean> getServiceHealthStatus() {
        Map<String, Boolean> healthStatus = new HashMap<>();
        
        for (McpService service : McpService.values()) {
            try {
                boolean isHealthy = isServiceAvailable(service);
                healthStatus.put(service.getServiceName(), isHealthy);
                log.debug("Service {} health status: {}", service.getServiceName(), isHealthy);
            } catch (Exception e) {
                log.warn("Failed to check health for service {}: {}", service.getServiceName(), e.getMessage());
                healthStatus.put(service.getServiceName(), false);
            }
        }
        
        return healthStatus;
    }

    /**
     * Get detailed service information including capabilities.
     *
     * @param service The MCP service
     * @return Service information map
     */
    public Map<String, Object> getServiceInfo(McpService service) {
        Map<String, Object> serviceInfo = new HashMap<>();
        serviceInfo.put("name", service.getServiceName());
        serviceInfo.put("url", getServiceUrl(service));
        serviceInfo.put("available", isServiceAvailable(service));

        try {
            // Get server info if service is available
            if (isServiceAvailable(service)) {
                var serverInfo = mcpServiceClient.getServerInfo(getServiceUrl(service));
                serviceInfo.put("serverInfo", serverInfo);
            }
        } catch (Exception e) {
            log.debug("Failed to get server info for {}: {}", service.getServiceName(), e.getMessage());
            serviceInfo.put("error", e.getMessage());
        }

        return serviceInfo;
    }

    /**
     * Validate that all required services are available.
     *
     * @param requiredServices Array of required services
     * @return true if all required services are available
     */
    public boolean validateRequiredServices(McpService... requiredServices) {
        for (McpService service : requiredServices) {
            if (!isServiceAvailable(service)) {
                log.error("Required service is not available: {}", service.getServiceName());
                return false;
            }
        }
        return true;
    }

    /**
     * Get service discovery information.
     *
     * @return Map containing all service discovery information
     */
    public Map<String, Object> getServiceDiscoveryInfo() {
        Map<String, Object> discoveryInfo = new HashMap<>();
        discoveryInfo.put("services", getAllServiceUrls());
        discoveryInfo.put("healthStatus", getServiceHealthStatus());
        discoveryInfo.put("timestamp", System.currentTimeMillis());
        
        // Count healthy services
        Map<String, Boolean> healthStatus = getServiceHealthStatus();
        long healthyCount = healthStatus.values().stream().mapToLong(healthy -> healthy ? 1 : 0).sum();
        discoveryInfo.put("healthyServices", healthyCount);
        discoveryInfo.put("totalServices", McpService.values().length);
        
        return discoveryInfo;
    }
}