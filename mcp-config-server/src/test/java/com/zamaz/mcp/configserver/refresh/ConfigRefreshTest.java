package com.zamaz.mcp.configserver.refresh;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConfigRefreshTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContextRefresher contextRefresher;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testRefreshEndpoint() throws Exception {
        // Test manual refresh endpoint
        mockMvc.perform(post("/actuator/refresh"))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testBusRefreshEndpoint() throws Exception {
        // Test bus refresh endpoint
        mockMvc.perform(post("/actuator/bus-refresh"))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testDestinationSpecificRefresh() throws Exception {
        // Test refresh for specific service
        mockMvc.perform(post("/actuator/bus-refresh/mcp-organization:*"))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void testContextRefresherBean() {
        assertNotNull(contextRefresher);
        
        // Test programmatic refresh
        Set<String> refreshedKeys = contextRefresher.refresh();
        assertNotNull(refreshedKeys);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testRefreshWithInvalidDestination() throws Exception {
        // Test refresh with invalid destination format
        mockMvc.perform(post("/actuator/bus-refresh/invalid-format"))
                .andExpect(status().is4xxClientError());
    }
}