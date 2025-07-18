package com.zamaz.mcp.common.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI configuration for consistent API documentation across all services.
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:mcp-service}")
    private String serviceName;

    @Value("${api.version:1.0.0}")
    private String apiVersion;

    @Value("${api.description:MCP Service API}")
    private String apiDescription;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(createApiInfo())
            .servers(createServers())
            .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
            .components(new io.swagger.v3.oas.models.Components()
                .addSecuritySchemes("Bearer Authentication", createApiKeyScheme()));
    }

    private Info createApiInfo() {
        return new Info()
            .title(formatTitle(serviceName))
            .description(apiDescription)
            .version(apiVersion)
            .contact(new Contact()
                .name("MCP Development Team")
                .email("dev@mcp.com")
                .url("https://mcp.com/contact"))
            .license(new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT"));
    }

    private List<Server> createServers() {
        return List.of(
            new Server()
                .url("http://localhost:" + serverPort + contextPath)
                .description("Development server"),
            new Server()
                .url("https://api.mcp.com" + contextPath)
                .description("Production server"),
            new Server()
                .url("https://staging-api.mcp.com" + contextPath)
                .description("Staging server")
        );
    }

    private SecurityScheme createApiKeyScheme() {
        return new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .bearerFormat("JWT")
            .scheme("bearer")
            .description("JWT Bearer token authentication. " +
                "Format: Authorization: Bearer {token}");
    }

    private String formatTitle(String serviceName) {
        // Convert mcp-organization to "MCP Organization Service API"
        String[] parts = serviceName.split("-");
        StringBuilder title = new StringBuilder();
        
        for (String part : parts) {
            if (title.length() > 0) {
                title.append(" ");
            }
            title.append(part.substring(0, 1).toUpperCase())
                 .append(part.substring(1).toLowerCase());
        }
        
        return title.toString() + " API";
    }
}