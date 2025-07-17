package com.zamaz.mcp.github.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GitHubInstallation Entity Tests")
class GitHubInstallationTest {

    private GitHubInstallation gitHubInstallation;
    private LocalDateTime testTime;

    @BeforeEach
    void setUp() {
        testTime = LocalDateTime.now();
        gitHubInstallation = GitHubInstallation.builder()
                .id(1L)
                .installationId(12345L)
                .accountId(67890L)
                .accountName("testuser")
                .accountType("User")
                .accessToken("test-token")
                .tokenExpiresAt(testTime.plusHours(1))
                .createdAt(testTime)
                .updatedAt(testTime)
                .build();
    }

    @Test
    @DisplayName("Should create GitHubInstallation with builder pattern")
    void testBuilderPattern() {
        GitHubInstallation installation = GitHubInstallation.builder()
                .installationId(99999L)
                .accountId(11111L)
                .accountName("buildertest")
                .accountType("Organization")
                .accessToken("builder-token")
                .build();

        assertNotNull(installation);
        assertEquals(99999L, installation.getInstallationId());
        assertEquals(11111L, installation.getAccountId());
        assertEquals("buildertest", installation.getAccountName());
        assertEquals("Organization", installation.getAccountType());
        assertEquals("builder-token", installation.getAccessToken());
    }

    @Test
    @DisplayName("Should create GitHubInstallation with no-args constructor")
    void testNoArgsConstructor() {
        GitHubInstallation installation = new GitHubInstallation();
        assertNotNull(installation);
        assertNull(installation.getId());
        assertNull(installation.getInstallationId());
        assertNull(installation.getAccountId());
        assertNull(installation.getAccountName());
        assertNull(installation.getAccountType());
        assertNull(installation.getAccessToken());
        assertNull(installation.getTokenExpiresAt());
        assertNull(installation.getCreatedAt());
        assertNull(installation.getUpdatedAt());
    }

    @Test
    @DisplayName("Should create GitHubInstallation with all-args constructor")
    void testAllArgsConstructor() {
        GitHubInstallation installation = new GitHubInstallation(
                1L, 12345L, 67890L, "testuser", "User", 
                "test-token", testTime.plusHours(1), testTime, testTime
        );

        assertNotNull(installation);
        assertEquals(1L, installation.getId());
        assertEquals(12345L, installation.getInstallationId());
        assertEquals(67890L, installation.getAccountId());
        assertEquals("testuser", installation.getAccountName());
        assertEquals("User", installation.getAccountType());
        assertEquals("test-token", installation.getAccessToken());
        assertEquals(testTime.plusHours(1), installation.getTokenExpiresAt());
        assertEquals(testTime, installation.getCreatedAt());
        assertEquals(testTime, installation.getUpdatedAt());
    }

    @Test
    @DisplayName("Should get and set installation ID")
    void testInstallationId() {
        assertEquals(12345L, gitHubInstallation.getInstallationId());
        
        gitHubInstallation.setInstallationId(54321L);
        assertEquals(54321L, gitHubInstallation.getInstallationId());
    }

    @Test
    @DisplayName("Should get and set account ID")
    void testAccountId() {
        assertEquals(67890L, gitHubInstallation.getAccountId());
        
        gitHubInstallation.setAccountId(98765L);
        assertEquals(98765L, gitHubInstallation.getAccountId());
    }

    @Test
    @DisplayName("Should get and set account name")
    void testAccountName() {
        assertEquals("testuser", gitHubInstallation.getAccountName());
        
        gitHubInstallation.setAccountName("newuser");
        assertEquals("newuser", gitHubInstallation.getAccountName());
    }

    @Test
    @DisplayName("Should get and set account type")
    void testAccountType() {
        assertEquals("User", gitHubInstallation.getAccountType());
        
        gitHubInstallation.setAccountType("Organization");
        assertEquals("Organization", gitHubInstallation.getAccountType());
    }

    @Test
    @DisplayName("Should get and set access token")
    void testAccessToken() {
        assertEquals("test-token", gitHubInstallation.getAccessToken());
        
        gitHubInstallation.setAccessToken("new-token");
        assertEquals("new-token", gitHubInstallation.getAccessToken());
    }

    @Test
    @DisplayName("Should get and set token expiry time")
    void testTokenExpiresAt() {
        assertEquals(testTime.plusHours(1), gitHubInstallation.getTokenExpiresAt());
        
        LocalDateTime newExpiry = testTime.plusHours(2);
        gitHubInstallation.setTokenExpiresAt(newExpiry);
        assertEquals(newExpiry, gitHubInstallation.getTokenExpiresAt());
    }

    @Test
    @DisplayName("Should get and set created at timestamp")
    void testCreatedAt() {
        assertEquals(testTime, gitHubInstallation.getCreatedAt());
        
        LocalDateTime newCreatedAt = testTime.minusHours(1);
        gitHubInstallation.setCreatedAt(newCreatedAt);
        assertEquals(newCreatedAt, gitHubInstallation.getCreatedAt());
    }

    @Test
    @DisplayName("Should get and set updated at timestamp")
    void testUpdatedAt() {
        assertEquals(testTime, gitHubInstallation.getUpdatedAt());
        
        LocalDateTime newUpdatedAt = testTime.plusMinutes(30);
        gitHubInstallation.setUpdatedAt(newUpdatedAt);
        assertEquals(newUpdatedAt, gitHubInstallation.getUpdatedAt());
    }

    @Test
    @DisplayName("Should handle null access token")
    void testNullAccessToken() {
        gitHubInstallation.setAccessToken(null);
        assertNull(gitHubInstallation.getAccessToken());
    }

    @Test
    @DisplayName("Should handle null token expiry")
    void testNullTokenExpiry() {
        gitHubInstallation.setTokenExpiresAt(null);
        assertNull(gitHubInstallation.getTokenExpiresAt());
    }

    @Test
    @DisplayName("Should test equals and hashCode")
    void testEqualsAndHashCode() {
        GitHubInstallation installation1 = GitHubInstallation.builder()
                .id(1L)
                .installationId(12345L)
                .accountId(67890L)
                .accountName("testuser")
                .accountType("User")
                .build();

        GitHubInstallation installation2 = GitHubInstallation.builder()
                .id(1L)
                .installationId(12345L)
                .accountId(67890L)
                .accountName("testuser")
                .accountType("User")
                .build();

        GitHubInstallation installation3 = GitHubInstallation.builder()
                .id(2L)
                .installationId(54321L)
                .accountId(98765L)
                .accountName("otheruser")
                .accountType("Organization")
                .build();

        // Test equals
        assertEquals(installation1, installation2);
        assertNotEquals(installation1, installation3);
        assertNotEquals(installation1, null);
        assertNotEquals(installation1, new Object());

        // Test hashCode
        assertEquals(installation1.hashCode(), installation2.hashCode());
        assertNotEquals(installation1.hashCode(), installation3.hashCode());
    }

    @Test
    @DisplayName("Should test toString method")
    void testToString() {
        String toString = gitHubInstallation.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("GitHubInstallation"));
        assertTrue(toString.contains("installationId=12345"));
        assertTrue(toString.contains("accountId=67890"));
        assertTrue(toString.contains("accountName=testuser"));
        assertTrue(toString.contains("accountType=User"));
    }

    @Test
    @DisplayName("Should create minimal GitHubInstallation")
    void testMinimalInstallation() {
        GitHubInstallation minimal = GitHubInstallation.builder()
                .installationId(123L)
                .accountId(456L)
                .accountName("minimal")
                .accountType("User")
                .build();

        assertNotNull(minimal);
        assertEquals(123L, minimal.getInstallationId());
        assertEquals(456L, minimal.getAccountId());
        assertEquals("minimal", minimal.getAccountName());
        assertEquals("User", minimal.getAccountType());
        assertNull(minimal.getAccessToken());
        assertNull(minimal.getTokenExpiresAt());
    }

    @Test
    @DisplayName("Should handle organization account type")
    void testOrganizationAccountType() {
        GitHubInstallation orgInstallation = GitHubInstallation.builder()
                .installationId(999L)
                .accountId(888L)
                .accountName("myorg")
                .accountType("Organization")
                .build();

        assertEquals("Organization", orgInstallation.getAccountType());
    }
}