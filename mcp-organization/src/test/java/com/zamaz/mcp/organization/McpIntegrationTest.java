package com.zamaz.mcp.organization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zamaz.mcp.organization.config.TestRedisConfiguration;
import com.zamaz.mcp.organization.config.TestSecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for MCP endpoints.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Import({TestRedisConfiguration.class, TestSecurityConfiguration.class})
class McpIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testMcpInfo() {
        webTestClient.get()
            .uri("/mcp")
            .exchange()
            .expectStatus().isOk()
            .expectBody(JsonNode.class)
            .value(response -> {
                assertThat(response.get("name").asText()).isEqualTo("mcp-organization");
                assertThat(response.get("version").asText()).isNotEmpty();
                assertThat(response.get("description").asText()).isNotEmpty();
                assertThat(response.get("capabilities")).isNotNull();
            });
    }

    @Test
    void testListTools() {
        webTestClient.post()
            .uri("/mcp/list-tools")
            .exchange()
            .expectStatus().isOk()
            .expectBody(JsonNode.class)
            .value(response -> {
                assertThat(response.get("tools")).isNotNull();
                assertThat(response.get("tools").isArray()).isTrue();
                assertThat(response.get("tools").size()).isGreaterThan(0);
                
                // Verify at least one tool exists
                JsonNode firstTool = response.get("tools").get(0);
                assertThat(firstTool.get("name")).isNotNull();
                assertThat(firstTool.get("description")).isNotNull();
            });
    }

    @Test
    void testHealthEndpoint() {
        webTestClient.get()
            .uri("/health")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .isEqualTo("OK");
    }

    @Test
    void testCallToolCreateOrganization() throws Exception {
        // Prepare request
        ObjectMapper mapper = new ObjectMapper();
        JsonNode arguments = mapper.createObjectNode()
            .put("name", "Test Organization")
            .put("description", "Test Description");
        
        JsonNode request = mapper.createObjectNode()
            .put("name", "create_organization")
            .set("arguments", arguments);

        webTestClient.post()
            .uri("/mcp/call-tool")
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk()
            .expectBody(JsonNode.class)
            .value(response -> {
                assertThat(response.get("id")).isNotNull();
                assertThat(response.get("name").asText()).isEqualTo("Test Organization");
                assertThat(response.get("status").asText()).isEqualTo("ACTIVE");
            });
    }
}