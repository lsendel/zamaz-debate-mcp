package com.zamaz.mcp.context;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the MCP Context Service.
 * Provides multi-tenant context management with PostgreSQL storage and Redis caching.
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
public class McpContextApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpContextApplication.class, args);
    }
}