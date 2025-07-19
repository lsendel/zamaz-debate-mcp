package com.zamaz.mcp.configserver.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testUnauthenticatedAccess() throws Exception {
        // Actuator health endpoint should be accessible without authentication
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void testAuthenticatedConfigAccess() throws Exception {
        // Configuration endpoints should require authentication
        mockMvc.perform(get("/application/default"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testAuthorizedConfigAccess() throws Exception {
        // With proper authentication, should be able to access config
        mockMvc.perform(get("/application/default"))
                .andExpect(status().isOk());
    }

    @Test
    void testBasicAuthAccess() throws Exception {
        // Test with basic authentication
        mockMvc.perform(get("/application/default")
                .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk());
    }

    @Test
    void testInvalidCredentials() throws Exception {
        // Test with invalid credentials
        mockMvc.perform(get("/application/default")
                .with(httpBasic("admin", "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testRefreshEndpointSecurity() throws Exception {
        // Refresh endpoints should require authentication
        mockMvc.perform(post("/actuator/refresh"))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void testRefreshEndpointUnauthorized() throws Exception {
        // Refresh endpoint without authentication
        mockMvc.perform(post("/actuator/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testBusRefreshEndpointSecurity() throws Exception {
        // Bus refresh endpoints should require authentication
        mockMvc.perform(post("/actuator/bus-refresh"))
                .andExpect(status().is2xxSuccessful());
    }
}