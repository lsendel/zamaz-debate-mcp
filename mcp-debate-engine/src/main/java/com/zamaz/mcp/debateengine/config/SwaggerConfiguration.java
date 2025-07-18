package com.zamaz.mcp.debateengine.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI configuration for debate engine.
 */
@Configuration
public class SwaggerConfiguration {
    
    @Value("${server.port:5005}")
    private String serverPort;
    
    @Bean
    public OpenAPI debateEngineOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("MCP Debate Engine API")
                .version("1.0.0")
                .description("Unified debate engine combining debate management, context handling, and orchestration")
                .contact(new Contact()
                    .name("MCP Team")
                    .email("team@mcp.com")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:" + serverPort)
                    .description("Local development server"),
                new Server()
                    .url("https://api.mcp.com/debate-engine")
                    .description("Production server")
            ));
    }
}