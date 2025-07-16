package com.zamaz.mcp.llm;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379",
    "logging.level.com.zamaz.mcp.llm=DEBUG",
    "spring.ai.anthropic.chat.enabled=false",
    "spring.ai.openai.chat.enabled=false"
})
class McpLlmApplicationTest {

    @Test
    void contextLoads() {
        // Test that the Spring Boot application context loads successfully
    }
}