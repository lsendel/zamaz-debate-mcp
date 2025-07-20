package com.zamaz.mcp.controller.config;

import com.zamaz.mcp.common.domain.agentic.AgenticFlowAnalyticsRepository;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowRepository;
import com.zamaz.mcp.controller.adapter.persistence.PostgresAgenticFlowAnalyticsRepository;
import com.zamaz.mcp.controller.adapter.persistence.PostgresAgenticFlowRepository;
import com.zamaz.mcp.controller.adapter.persistence.mapper.AgenticFlowMapper;
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
     * @param mapper               The entity mapper
     * @return The repository implementation
     */
    @Bean
    public AgenticFlowRepository agenticFlowRepository(
            SpringDataAgenticFlowRepository springDataRepository,
            AgenticFlowMapper mapper) {
        return new PostgresAgenticFlowRepository(springDataRepository, mapper);
    }

    /**
     * Creates the AgenticFlowAnalyticsRepository bean.
     *
     * @param executionRepository The execution repository
     * @param flowRepository      The flow repository
     * @param mapper              The entity mapper
     * @return The analytics repository implementation
     */
    @Bean
    public AgenticFlowAnalyticsRepository agenticFlowAnalyticsRepository(
            SpringDataAgenticFlowExecutionRepository executionRepository,
            SpringDataAgenticFlowRepository flowRepository,
            AgenticFlowMapper mapper) {
        return new PostgresAgenticFlowAnalyticsRepository(executionRepository, flowRepository, mapper);
    }

    /**
     * Note: The AgenticFlowApplicationService and other domain services are
     * expected to be configured in the mcp-common module or through component
     * scanning.
     * 
     * This configuration focuses on infrastructure adapters specific to the
     * controller module.
     */
}