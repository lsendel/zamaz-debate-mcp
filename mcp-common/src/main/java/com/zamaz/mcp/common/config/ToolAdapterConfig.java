package com.zamaz.mcp.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zamaz.mcp.common.domain.tool.ExternalToolPort;
import com.zamaz.mcp.common.infrastructure.tool.CalculatorToolAdapter;
import com.zamaz.mcp.common.infrastructure.tool.WebSearchToolAdapter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration class for registering tool adapters.
 */
@Configuration
public class ToolAdapterConfig {

    /**
     * Creates a list of all available tool adapters.
     *
     * @param webSearchTool  The web search tool adapter
     * @param calculatorTool The calculator tool adapter
     * @return A list of all available tool adapters
     */
    @Bean
    public List<ExternalToolPort> toolAdapters(
            WebSearchToolAdapter webSearchTool,
            CalculatorToolAdapter calculatorTool) {
        return List.of(webSearchTool, calculatorTool);
    }
}