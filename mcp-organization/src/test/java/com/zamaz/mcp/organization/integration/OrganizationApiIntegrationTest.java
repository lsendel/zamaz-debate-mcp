package com.zamaz.mcp.organization.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zamaz.mcp.organization.dto.OrganizationDto;
import com.zamaz.mcp.organization.entity.Organization;
import com.zamaz.mcp.organization.repository.OrganizationRepository;
import com.zamaz.mcp.organization.service.OrganizationService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Organization API Integration Tests")
class OrganizationApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationService organizationService;

    private static UUID testOrganizationId;
    private static final String TEST_USER_ID = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        // Clean up test data
        organizationRepository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("Should create organization via MCP tool endpoint")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void shouldCreateOrganizationViaMcpTool() throws Exception {
        // Given
        Map<String, Object> request = Map.of(
                "name", "Test Organization",
                "description", "Integration test organization"
        );

        // When & Then
        String response = mockMvc.perform(post("/tools/create_organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.organization.name").value("Test Organization"))
                .andExpect(jsonPath("$.organization.description").value("Integration test organization"))
                .andExpect(jsonPath("$.organization.id").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
        Map<String, Object> org = (Map<String, Object>) responseMap.get("organization");
        testOrganizationId = UUID.fromString(org.get("id").toString());

        // Verify in database
        assertThat(organizationRepository.count()).isEqualTo(1);
        Organization saved = organizationRepository.findById(testOrganizationId).orElseThrow();
        assertThat(saved.getName()).isEqualTo("Test Organization");
    }

    @Test
    @Order(2)
    @DisplayName("Should reject organization creation without authentication")
    void shouldRejectOrganizationCreationWithoutAuth() throws Exception {
        // Given
        Map<String, Object> request = Map.of(
                "name", "Unauthorized Organization"
        );

        // When & Then
        mockMvc.perform(post("/tools/create_organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(3)
    @DisplayName("Should get organization by ID")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void shouldGetOrganizationById() throws Exception {
        // Given - create organization first
        Organization org = createTestOrganization();

        // When & Then
        mockMvc.perform(post("/tools/get_organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of()))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.organization.id").value(org.getId().toString()))
                .andExpect(jsonPath("$.organization.name").value(org.getName()));
    }

    @Test
    @Order(4)
    @DisplayName("Should update organization")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void shouldUpdateOrganization() throws Exception {
        // Given
        Organization org = createTestOrganization();
        
        Map<String, Object> updateRequest = Map.of(
                "name", "Updated Organization Name",
                "description", "Updated description",
                "isActive", false
        );

        // When & Then
        mockMvc.perform(post("/tools/update_organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.organization.name").value("Updated Organization Name"))
                .andExpect(jsonPath("$.organization.description").value("Updated description"))
                .andExpect(jsonPath("$.organization.isActive").value(false));

        // Verify in database
        Organization updated = organizationRepository.findById(org.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Updated Organization Name");
        assertThat(updated.isActive()).isFalse();
    }

    @Test
    @Order(5)
    @DisplayName("Should delete organization with admin role")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldDeleteOrganizationWithAdminRole() throws Exception {
        // Given
        Organization org = createTestOrganization();
        assertThat(organizationRepository.count()).isEqualTo(1);

        // When & Then
        mockMvc.perform(post("/tools/delete_organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of()))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Organization deleted successfully"));

        // Verify deleted
        assertThat(organizationRepository.count()).isEqualTo(0);
    }

    @Test
    @Order(6)
    @DisplayName("Should reject delete without admin role")
    @WithMockUser(username = "user", roles = {"USER"})
    void shouldRejectDeleteWithoutAdminRole() throws Exception {
        // Given
        createTestOrganization();

        // When & Then
        mockMvc.perform(post("/tools/delete_organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of()))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(7)
    @DisplayName("Should add user to organization")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldAddUserToOrganization() throws Exception {
        // Given
        Organization org = createTestOrganization();
        UUID userId = UUID.randomUUID();

        Map<String, Object> request = Map.of(
                "userId", userId.toString(),
                "role", "member"
        );

        // When & Then
        mockMvc.perform(post("/tools/add_user_to_organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User added to organization successfully"));
    }

    @Test
    @Order(8)
    @DisplayName("Should remove user from organization")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldRemoveUserFromOrganization() throws Exception {
        // Given
        Organization org = createTestOrganization();
        UUID userId = UUID.randomUUID();
        
        // First add user
        organizationService.addUserToOrganization(org.getId(), userId, "member");

        Map<String, Object> request = Map.of(
                "userId", userId.toString()
        );

        // When & Then
        mockMvc.perform(post("/tools/remove_user_from_organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User removed from organization successfully"));
    }

    @Test
    @Order(9)
    @DisplayName("Should list organizations as MCP resource")
    @WithMockUser(username = "user", roles = {"USER"})
    void shouldListOrganizationsAsResource() throws Exception {
        // Given
        createTestOrganization();

        // When & Then
        mockMvc.perform(get("/tools/resources/organizations")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizations").isArray())
                .andExpect(jsonPath("$.organizations", hasSize(1)))
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    @Order(10)
    @DisplayName("Should handle validation errors gracefully")
    @WithMockUser(username = "user", roles = {"USER"})
    void shouldHandleValidationErrorsGracefully() throws Exception {
        // Given - request without required name field
        Map<String, Object> request = Map.of(
                "description", "Missing name field"
        );

        // When & Then
        mockMvc.perform(post("/tools/create_organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error_type").value("ValidationError"));
    }

    @Test
    @Order(11)
    @DisplayName("Should handle rate limiting")
    @WithMockUser(username = "user", roles = {"USER"})
    void shouldHandleRateLimiting() throws Exception {
        // Given - rate limit is 3 per hour for organization creation
        Map<String, Object> request = Map.of(
                "name", "Rate Limited Org",
                "description", "Testing rate limits"
        );

        // When - make requests up to limit
        for (int i = 0; i < 3; i++) {
            request = Map.of("name", "Rate Limited Org " + i);
            mockMvc.perform(post("/tools/create_organization")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        // Then - next request should be rate limited
        mockMvc.perform(post("/tools/create_organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @Order(12)
    @DisplayName("Should handle concurrent organization operations")
    @WithMockUser(username = "user", roles = {"USER"})
    @Transactional
    void shouldHandleConcurrentOrganizationOperations() throws Exception {
        // Given
        Organization org = createTestOrganization();
        
        // When - simulate concurrent updates
        Map<String, Object> update1 = Map.of(
                "name", "Concurrent Update 1",
                "description", "First update"
        );
        
        Map<String, Object> update2 = Map.of(
                "name", "Concurrent Update 2",
                "description", "Second update"
        );

        // Perform concurrent updates
        mockMvc.perform(post("/tools/update_organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update1))
                        .with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/tools/update_organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update2))
                        .with(csrf()))
                .andExpect(status().isOk());

        // Then - last update should win
        Organization updated = organizationRepository.findById(org.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Concurrent Update 2");
    }

    // Helper methods
    private Organization createTestOrganization() {
        Organization org = new Organization();
        org.setName("Test Org " + UUID.randomUUID());
        org.setDescription("Test organization for integration tests");
        org.setActive(true);
        return organizationRepository.save(org);
    }
}