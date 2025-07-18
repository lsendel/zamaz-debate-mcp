package com.zamaz.mcp.controller.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Spring configuration for the debate controller service.
 */
@Configuration
@ComponentScan(basePackages = {
    "com.zamaz.mcp.controller.application.usecase",
    "com.zamaz.mcp.controller.adapter.web",
    "com.zamaz.mcp.controller.adapter.persistence"
})
@EnableJpaRepositories(basePackages = "com.zamaz.mcp.controller.adapter.persistence")
@EnableTransactionManagement
public class DebateConfig {
    
    // Configuration handled by component scanning and annotations
}