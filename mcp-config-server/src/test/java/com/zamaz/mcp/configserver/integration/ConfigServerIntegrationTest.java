package com.zamaz.mcp.configserver.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ConfigServerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testActuatorHealthEndpoint() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health",
                Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
    }

    @Test
    void testConfigurationRetrieval() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/application/default",
                Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("application", response.getBody().get("name"));
    }

    @Test
    void testRefreshEndpoint() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/actuator/refresh",
                null,
                String.class
        );

        // Refresh endpoint might return 204 NO_CONTENT or 200 OK
        assertTrue(response.getStatusCode().is2xxSuccessful());
    }

    @Test
    void testBusRefreshEndpoint() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/actuator/bus-refresh",
                null,
                String.class
        );

        // Bus refresh endpoint might return 204 NO_CONTENT or 200 OK
        assertTrue(response.getStatusCode().is2xxSuccessful());
    }

    @Test
    void testMetricsEndpoint() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/metrics",
                Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("names"));
    }

    @Test
    void testPrometheusEndpoint() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/prometheus",
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("# HELP"));
    }
}