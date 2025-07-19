package com.zamaz.mcp.configserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Basic integration test for the Config Server application.
 * 
 * This test verifies that the Spring Boot application context loads successfully
 * with the Config Server configuration.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.cloud.config.server.git.uri=classpath:/config-repo",
    "spring.security.user.name=test-admin",
    "spring.security.user.password=test-password",
    "encrypt.key=test-encryption-key"
})
class ConfigServerApplicationTest {

    @Test
    void contextLoads() {
        // This test verifies that the Spring Boot application context
        // loads successfully with all the Config Server configurations
    }
}