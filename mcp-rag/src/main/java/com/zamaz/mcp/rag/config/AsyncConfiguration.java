package com.zamaz.mcp.rag.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration for async processing.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfiguration {
    // Spring Boot auto-configures the thread pools based on application.yml settings
}