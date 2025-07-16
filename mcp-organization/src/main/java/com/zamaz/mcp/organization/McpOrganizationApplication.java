package com.zamaz.mcp.organization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"com.zamaz.mcp.organization", "com.zamaz.mcp.security", "com.zamaz.mcp.common"})
@EnableJpaRepositories(basePackages = "com.zamaz.mcp.organization.repository")
@EntityScan(basePackages = "com.zamaz.mcp.organization.entity")
public class McpOrganizationApplication {
    public static void main(String[] args) {
        SpringApplication.run(McpOrganizationApplication.class, args);
    }
}