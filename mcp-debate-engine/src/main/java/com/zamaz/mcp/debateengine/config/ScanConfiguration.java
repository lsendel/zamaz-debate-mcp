package com.zamaz.mcp.debateengine.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Component scan configuration for debate engine.
 */
@Configuration
@ComponentScan(basePackages = {
    "com.zamaz.mcp.debateengine",
    "com.zamaz.mcp.common"
})
@EnableJpaRepositories(basePackages = "com.zamaz.mcp.debateengine.adapter.persistence.repository")
@EntityScan(basePackages = "com.zamaz.mcp.debateengine.adapter.persistence.entity")
@EnableTransactionManagement
@EnableJpaAuditing
public class ScanConfiguration {
}