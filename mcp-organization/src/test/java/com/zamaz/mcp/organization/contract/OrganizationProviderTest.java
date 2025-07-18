package com.zamaz.mcp.organization.contract;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.zamaz.mcp.organization.domain.model.Organization;
import com.zamaz.mcp.organization.domain.model.User;
import com.zamaz.mcp.organization.testing.OrganizationTestFixtures;
import com.zamaz.mcp.security.testing.SecurityTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Map;

/**
 * Provider contract verification tests for mcp-organization service.
 * Verifies that the organization service fulfills contracts established by consumers.
 */
@Provider("mcp-organization")
@PactFolder("pacts")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@SpringJUnitConfig
class OrganizationProviderTest {

    @LocalServerPort
    private int port;

    private OrganizationTestFixtures fixtures;

    @BeforeEach
    void setUp() {
        System.setProperty("pact.verifier.publishResults", "true");
        System.setProperty("pact.provider.version", "1.0.0");
        
        fixtures = new OrganizationTestFixtures();
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    /**
     * State: organization exists
     */
    @State("organization exists")
    void organizationExists(Map<String, Object> params) {
        String orgId = (String) params.getOrDefault("organizationId", "test-org");
        
        Organization org = fixtures.createOrganization()
            .withId(orgId)
            .withName("Test Organization")
            .withStatus(Organization.Status.ACTIVE)
            .build();
            
        // Setup organization in repository/database
        setupOrganization(org);
    }

    /**
     * State: user is authenticated
     */
    @State("user is authenticated") 
    void userIsAuthenticated(Map<String, Object> params) {
        String userId = (String) params.getOrDefault("userId", "test-user");
        String orgId = (String) params.getOrDefault("organizationId", "test-org");
        
        User user = fixtures.createUser()
            .withId(userId)
            .withEmail("test@example.com")
            .withOrganizationId(orgId)
            .withRole(User.Role.USER)
            .build();
            
        // Setup authenticated user context
        SecurityTestContext.setContext(
            SecurityTestContext.organizationUser(orgId, userId)
        );
        
        setupUser(user);
    }

    /**
     * State: user has admin permissions
     */
    @State("user has admin permissions")
    void userHasAdminPermissions(Map<String, Object> params) {
        String userId = (String) params.getOrDefault("userId", "admin-user");
        String orgId = (String) params.getOrDefault("organizationId", "test-org");
        
        User adminUser = fixtures.createUser()
            .withId(userId)
            .withEmail("admin@example.com")
            .withOrganizationId(orgId)
            .withRole(User.Role.ADMIN)
            .build();
            
        SecurityTestContext.setContext(
            SecurityTestContext.organizationAdmin(orgId, userId)
        );
        
        setupUser(adminUser);
    }

    /**
     * State: organization has active subscription
     */
    @State("organization has active subscription")
    void organizationHasActiveSubscription(Map<String, Object> params) {
        String orgId = (String) params.getOrDefault("organizationId", "test-org");
        
        Organization org = fixtures.createOrganization()
            .withId(orgId)
            .withName("Test Organization")
            .withStatus(Organization.Status.ACTIVE)
            .withSubscription(fixtures.createSubscription()
                .withTier("PRO")
                .withMaxUsers(50)
                .withFeature("advanced_debates")
                .withFeature("analytics")
                .build())
            .build();
            
        setupOrganization(org);
    }

    /**
     * State: organization has usage limits
     */
    @State("organization has usage limits")
    void organizationHasUsageLimits(Map<String, Object> params) {
        String orgId = (String) params.getOrDefault("organizationId", "test-org");
        
        Organization org = fixtures.createOrganization()
            .withId(orgId)
            .withName("Test Organization")
            .withStatus(Organization.Status.ACTIVE)
            .build();
            
        // Setup usage limits
        setupOrganizationLimits(orgId, Map.of(
            "maxConcurrentDebates", 10,
            "maxParticipantsPerDebate", 4,
            "maxRoundsPerDebate", 10,
            "monthlyTokenQuota", 1000000
        ));
        
        setupOrganization(org);
    }

    /**
     * State: organization has LLM settings configured
     */
    @State("organization has LLM settings")
    void organizationHasLlmSettings(Map<String, Object> params) {
        String orgId = (String) params.getOrDefault("organizationId", "test-org");
        
        // Setup LLM configuration for organization
        setupOrganizationLlmSettings(orgId, Map.of(
            "enabledProviders", new String[]{"anthropic", "openai"},
            "quotas", Map.of(
                "monthlyTokenLimit", 1000000,
                "dailyRequestLimit", 10000
            ),
            "modelPreferences", Map.of(
                "defaultProvider", "anthropic",
                "defaultModel", "claude-3-sonnet"
            )
        ));
        
        organizationExists(params);
    }

    /**
     * State: multiple users exist in organization
     */
    @State("multiple users exist in organization")
    void multipleUsersExistInOrganization(Map<String, Object> params) {
        String orgId = (String) params.getOrDefault("organizationId", "test-org");
        
        // Create multiple users
        User user1 = fixtures.createUser()
            .withId("user-1")
            .withEmail("user1@example.com")
            .withOrganizationId(orgId)
            .withRole(User.Role.USER)
            .build();
            
        User user2 = fixtures.createUser()
            .withId("user-2")
            .withEmail("user2@example.com")
            .withOrganizationId(orgId)
            .withRole(User.Role.MODERATOR)
            .build();
            
        User admin = fixtures.createUser()
            .withId("admin-1")
            .withEmail("admin@example.com")
            .withOrganizationId(orgId)
            .withRole(User.Role.ADMIN)
            .build();
        
        setupUser(user1);
        setupUser(user2);
        setupUser(admin);
        
        organizationExists(params);
    }

    /**
     * State: organization has team hierarchy
     */
    @State("organization has team hierarchy")
    void organizationHasTeamHierarchy(Map<String, Object> params) {
        String orgId = (String) params.getOrDefault("organizationId", "test-org");
        
        OrganizationTestFixtures.OrganizationHierarchy hierarchy = 
            fixtures.createHierarchy()
                .withRootOrganization(orgId, "Test Organization")
                .withDepartment("dept-1", "Engineering", orgId)
                .withTeam("team-1", "Backend Team", "dept-1")
                .withTeam("team-2", "Frontend Team", "dept-1")
                .build();
        
        setupOrganizationHierarchy(hierarchy);
    }

    /**
     * State: user has specific permissions
     */
    @State("user has permissions")
    void userHasPermissions(Map<String, Object> params) {
        String userId = (String) params.getOrDefault("userId", "test-user");
        String orgId = (String) params.getOrDefault("organizationId", "test-org");
        String[] permissions = (String[]) params.getOrDefault("permissions", 
            new String[]{"DEBATE_CREATE", "DEBATE_VIEW", "DEBATE_MANAGE"});
        
        User user = fixtures.createUser()
            .withId(userId)
            .withEmail("test@example.com")
            .withOrganizationId(orgId)
            .withRole(User.Role.USER)
            .build();
            
        // Setup user permissions
        setupUserPermissions(userId, permissions);
        
        SecurityTestContext.setContext(
            SecurityTestContext.organizationUser(orgId, userId)
        );
        
        setupUser(user);
    }

    /**
     * State: organization has current usage
     */
    @State("organization has current usage")
    void organizationHasCurrentUsage(Map<String, Object> params) {
        String orgId = (String) params.getOrDefault("organizationId", "test-org");
        
        // Setup current usage statistics
        setupOrganizationUsage(orgId, Map.of(
            "activeDebates", 3,
            "monthlyTokensUsed", 50000,
            "dailyRequestsUsed", 150
        ));
        
        organizationExists(params);
        organizationHasUsageLimits(params);
    }

    /**
     * State: invitation exists
     */
    @State("invitation exists")
    void invitationExists(Map<String, Object> params) {
        String invitationId = (String) params.getOrDefault("invitationId", "test-invitation");
        String orgId = (String) params.getOrDefault("organizationId", "test-org");
        String email = (String) params.getOrDefault("email", "invited@example.com");
        
        // Setup invitation
        setupInvitation(invitationId, orgId, email, User.Role.USER);
        
        organizationExists(params);
    }

    // Helper methods for setting up test data

    private void setupOrganization(Organization org) {
        // In a real implementation, this would save to the database
        // For testing, we might use in-memory repositories or mocks
    }

    private void setupUser(User user) {
        // Setup user in repository/database
    }

    private void setupOrganizationLimits(String orgId, Map<String, Object> limits) {
        // Setup organization limits
    }

    private void setupOrganizationLlmSettings(String orgId, Map<String, Object> settings) {
        // Setup LLM configuration
    }

    private void setupOrganizationHierarchy(OrganizationTestFixtures.OrganizationHierarchy hierarchy) {
        // Setup organizational hierarchy
    }

    private void setupUserPermissions(String userId, String[] permissions) {
        // Setup user permissions
    }

    private void setupOrganizationUsage(String orgId, Map<String, Object> usage) {
        // Setup current usage statistics
    }

    private void setupInvitation(String invitationId, String orgId, String email, User.Role role) {
        // Setup invitation
    }
}