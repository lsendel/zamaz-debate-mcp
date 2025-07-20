package com.zamaz.mcp.common.infrastructure.persistence.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Configuration for agentic flow persistence layer.
 * This configuration enables JPA repositories and entity scanning for agentic
 * flows.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.zamaz.mcp.common.infrastructure.persistence.repository")
@EntityScan(basePackages = "com.zamaz.mcp.common.infrastructure.persistence.entity")
@EnableTransactionManagement
@ConditionalOnProperty(name = "mcp.agentic-flows.persistence.enabled", havingValue = "true", matchIfMissing = true)
public class AgenticFlowPersistenceConfig {

    // Configuration is handled through annotations
    // Additional beans can be defined here if needed
}