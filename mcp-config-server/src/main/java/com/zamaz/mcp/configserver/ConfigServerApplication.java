package com.zamaz.mcp.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import com.zamaz.mcp.configserver.config.SecurityProperties;

/**
 * Spring Cloud Config Server application for centralized configuration management.
 * 
 * This server provides configuration to all MCP microservices from a Git repository,
 * supporting environment-specific profiles, encrypted properties, and dynamic refresh.
 * 
 * Features:
 * - Environment-specific security configuration
 * - Integration with HashiCorp Vault and AWS Secrets Manager
 * - Dynamic configuration refresh without service restarts
 * - Configuration validation with minimum security requirements
 */
@SpringBootApplication
@EnableConfigServer
@EnableConfigurationProperties(SecurityProperties.class)
@RefreshScope
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}