package com.zamaz.mcp.authserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Spring Authorization Server application for MCP Services.
 * Provides OAuth2/OIDC authentication and authorization capabilities.
 */
@SpringBootApplication
@EnableRedisHttpSession
public class AuthServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServerApplication.class, args);
    }
}