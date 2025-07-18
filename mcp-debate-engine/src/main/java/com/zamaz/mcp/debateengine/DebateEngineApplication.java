package com.zamaz.mcp.debateengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Unified Debate Engine Application
 * 
 * Consolidates functionality from:
 * - mcp-controller (orchestration and AI analysis)
 * - mcp-debate (debate management)
 * - mcp-context (context management)
 * 
 * This service handles the complete debate lifecycle:
 * - Debate creation and management
 * - Context creation and versioning
 * - Real-time debate orchestration
 * - AI analysis and quality scoring
 * - WebSocket communication
 * - Template-based debate generation
 */
@SpringBootApplication(scanBasePackages = {
    "com.zamaz.mcp.debateengine",
    "com.zamaz.mcp.common",
    "com.zamaz.mcp.security"
})
@EnableJpaAuditing
@EnableTransactionManagement
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableFeignClients
@IntegrationComponentScan
public class DebateEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(DebateEngineApplication.class, args);
    }
}