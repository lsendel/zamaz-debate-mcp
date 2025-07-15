package com.zamaz.mcp.modulith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulith;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main application class for MCP Modulith.
 * Uses Spring Modulith to create a modular monolith architecture.
 */
@SpringBootApplication
@EnableAsync
@Modulith(
    systemName = "MCP System",
    useFullyQualifiedModuleNames = true
)
public class McpModulithApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpModulithApplication.class, args);
    }
}