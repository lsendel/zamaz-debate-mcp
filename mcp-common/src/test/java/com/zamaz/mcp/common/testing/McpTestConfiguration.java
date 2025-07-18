package com.zamaz.mcp.common.testing;

import com.zamaz.mcp.common.client.McpServiceClient;
import com.zamaz.mcp.common.client.McpServiceRegistry;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

/**
 * Test configuration for MCP testing framework.
 * Provides test-specific beans and configurations.
 */
@TestConfiguration
@ActiveProfiles("test")
public class McpTestConfiguration {

    /**
     * Mock MCP service client for unit tests.
     */
    @MockBean
    @Profile("mock")
    public McpServiceClient mockMcpServiceClient() {
        return null; // Will be mocked by @MockBean
    }

    /**
     * Mock MCP service registry for unit tests.
     */
    @MockBean
    @Profile("mock")
    public McpServiceRegistry mockMcpServiceRegistry() {
        return null; // Will be mocked by @MockBean
    }

    /**
     * Test authentication provider for integration tests.
     */
    @Bean
    @Profile("integration")
    public McpTestAuthenticationProvider mcpTestAuthenticationProvider() {
        return new McpTestAuthenticationProvider();
    }

    /**
     * Test data factory for creating test scenarios.
     */
    @Bean
    public McpTestDataFactory mcpTestDataFactory() {
        return new McpTestDataFactory();
    }

    /**
     * Test utilities for common test operations.
     */
    @Bean
    public McpTestUtils mcpTestUtils() {
        return new McpTestUtils();
    }
}