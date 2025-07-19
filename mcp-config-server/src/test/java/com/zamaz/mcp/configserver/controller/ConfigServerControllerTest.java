package com.zamaz.mcp.configserver.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConfigServerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void testInfoEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    @Test
    void testConfigurationEndpoint() throws Exception {
        // Test fetching configuration for a specific application
        mockMvc.perform(get("/test-app/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test-app"))
                .andExpect(jsonPath("$.profiles[0]").value("default"));
    }

    @Test
    void testConfigurationWithProfile() throws Exception {
        // Test fetching configuration with specific profile
        mockMvc.perform(get("/test-app/development"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test-app"))
                .andExpect(jsonPath("$.profiles[0]").value("development"));
    }

    @Test
    void testConfigurationWithLabel() throws Exception {
        // Test fetching configuration with specific Git label/branch
        mockMvc.perform(get("/test-app/default/main"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test-app"))
                .andExpect(jsonPath("$.label").value("main"));
    }

    @Test
    void testNonExistentApplication() throws Exception {
        // Test fetching configuration for non-existent application
        mockMvc.perform(get("/non-existent-app/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propertySources").isEmpty());
    }
}