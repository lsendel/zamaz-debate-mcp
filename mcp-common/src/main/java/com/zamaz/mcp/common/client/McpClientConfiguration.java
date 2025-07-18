package com.zamaz.mcp.common.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration for MCP client components.
 * Automatically configures all necessary beans for inter-service MCP communication.
 */
@Configuration
@Import({
    McpServiceClient.class,
    McpServiceRegistry.class,
    OrganizationServiceClient.class,
    ContextServiceClient.class,
    LlmServiceClient.class
})
public class McpClientConfiguration {

    /**
     * Configure MCP service client if not already present.
     * This allows services to override with custom configurations.
     */
    @Bean
    @ConditionalOnMissingBean
    public McpServiceClient mcpServiceClient() {
        // Bean will be created via @Component annotation
        return null;
    }

    /**
     * Configure MCP service registry if not already present.
     */
    @Bean
    @ConditionalOnMissingBean
    public McpServiceRegistry mcpServiceRegistry() {
        // Bean will be created via @Component annotation
        return null;
    }

    /**
     * Configure organization service client if not already present.
     */
    @Bean
    @ConditionalOnMissingBean
    public OrganizationServiceClient organizationServiceClient() {
        // Bean will be created via @Component annotation
        return null;
    }

    /**
     * Configure context service client if not already present.
     */
    @Bean
    @ConditionalOnMissingBean
    public ContextServiceClient contextServiceClient() {
        // Bean will be created via @Component annotation
        return null;
    }

    /**
     * Configure LLM service client if not already present.
     */
    @Bean
    @ConditionalOnMissingBean
    public LlmServiceClient llmServiceClient() {
        // Bean will be created via @Component annotation
        return null;
    }
}