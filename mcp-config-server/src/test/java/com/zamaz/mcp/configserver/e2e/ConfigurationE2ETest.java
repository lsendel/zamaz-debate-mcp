package com.zamaz.mcp.configserver.e2e;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ConfigurationE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
    }

    @Test
    void testCompleteConfigurationWorkflow() {
        // Step 1: Verify Config Server is healthy
        ResponseEntity<Map> healthResponse = restTemplate.getForEntity(
                baseUrl + "/actuator/health",
                Map.class
        );
        assertEquals(HttpStatus.OK, healthResponse.getStatusCode());
        assertEquals("UP", healthResponse.getBody().get("status"));

        // Step 2: Fetch configuration for a service
        ResponseEntity<Environment> configResponse = restTemplate.getForEntity(
                baseUrl + "/test-app/default",
                Environment.class
        );
        assertEquals(HttpStatus.OK, configResponse.getStatusCode());
        assertNotNull(configResponse.getBody());
        assertEquals("test-app", configResponse.getBody().getName());

        // Step 3: Verify properties are loaded
        Environment environment = configResponse.getBody();
        assertNotNull(environment.getPropertySources());
        assertFalse(environment.getPropertySources().isEmpty());

        // Step 4: Test profile-specific configuration
        ResponseEntity<Environment> devConfigResponse = restTemplate.getForEntity(
                baseUrl + "/test-app/development",
                Environment.class
        );
        assertEquals(HttpStatus.OK, devConfigResponse.getStatusCode());
        assertEquals("development", devConfigResponse.getBody().getProfiles()[0]);

        // Step 5: Test refresh functionality
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth("admin", "admin");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> refreshResponse = restTemplate.exchange(
                baseUrl + "/actuator/refresh",
                HttpMethod.POST,
                entity,
                String.class
        );
        assertTrue(refreshResponse.getStatusCode().is2xxSuccessful());

        // Step 6: Verify metrics are being collected
        ResponseEntity<Map> metricsResponse = restTemplate.getForEntity(
                baseUrl + "/actuator/metrics/config.server.requests",
                Map.class
        );
        assertEquals(HttpStatus.OK, metricsResponse.getStatusCode());
    }

    @Test
    void testServiceConfigurationLoading() {
        // Test loading configuration for multiple services
        String[] services = {"mcp-organization", "mcp-llm", "mcp-controller", "mcp-rag"};
        
        for (String service : services) {
            ResponseEntity<Environment> response = restTemplate.getForEntity(
                    baseUrl + "/" + service + "/default",
                    Environment.class
            );
            
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(service, response.getBody().getName());
        }
    }

    @Test
    void testEnvironmentSpecificConfiguration() {
        String[] environments = {"development", "staging", "production"};
        
        for (String env : environments) {
            ResponseEntity<Environment> response = restTemplate.getForEntity(
                    baseUrl + "/test-app/" + env,
                    Environment.class
            );
            
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().getProfiles().length > 0);
            assertEquals(env, response.getBody().getProfiles()[0]);
        }
    }

    @Test
    void testConfigurationWithLabel() {
        // Test fetching configuration from specific Git branch/tag
        ResponseEntity<Environment> response = restTemplate.getForEntity(
                baseUrl + "/test-app/default/main",
                Environment.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("main", response.getBody().getLabel());
    }

    @Test
    void testPropertiesEndpoint() {
        // Test raw properties endpoint
        ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl + "/test-app-default.properties",
                Map.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testYamlEndpoint() {
        // Test YAML endpoint
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/test-app-default.yml",
                String.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("test:"));
    }

    @Test
    void testJsonEndpoint() {
        // Test JSON endpoint
        ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl + "/test-app-default.json",
                Map.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}