package com.zamaz.mcp.controller.config;

import com.zamaz.mcp.common.domain.agentic.AgenticFlowRepository;
import com.zamaz.mcp.controller.adapter.persistence.PostgresAgenticFlowRepository;
import com.zamaz.mcp.controller.adapter.persistence.repository.SpringDataAgenticFlowRepository;
import com.zamaz.mcp.controller.adapter.persistence.repository.SpringDataAgenticFlowExecutionRepository;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuration for agentic flow infrastructure components.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.zamaz.mcp.controller.adapter.persistence.repository")
public class AgenticFlowConfiguration {

    /**
     * Creates the AgenticFlowRepository bean.
     *
     * @param springDataRepository The Spring Data repository
     * @return The repository implementation
     */
    @Bean
    public AgenticFlowRepository agenticFlowRepository(SpringDataAgenticFlowRepository springDataRepository) {
        return new PostgresAgenticFlowRepository(springDataRepository);
    }

    /**
     * Note: The AgenticFlowApplicationService and other domain services are expected to be
     * configured in the mcp-common module or through component scanning.
     * 
     * This configuration focuses on infrastructure adapters specific to the controller module.
     */
}