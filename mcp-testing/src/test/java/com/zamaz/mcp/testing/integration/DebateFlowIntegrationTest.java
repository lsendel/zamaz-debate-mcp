package com.zamaz.mcp.testing.integration;

import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.springframework.test.context.jdbc.Sql;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration test for complete debate flow
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class DebateFlowIntegrationTest extends BaseIntegrationTest {
    
    private static String authToken;
    private static String debateId;
    private static final String ORG_ID = "test-org-123";
    
    @BeforeAll
    static void authenticate() {
        // This would typically be done in the base class or test setup
        authToken = "test-token"; // Simplified for example
    }
    
    @Test
    @Order(1)
    @DisplayName("Should create a new debate")
    void createDebate() {
        Map<String, Object> debateRequest = createTestDebate();
        
        Response response = given()
            .spec(authenticatedRequest(authToken, ORG_ID))
            .body(debateRequest)
            .when()
            .post("/api/v1/debates")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("title", equalTo(debateRequest.get("title")))
            .body("status", equalTo("CREATED"))
            .extract()
            .response();
        
        debateId = response.path("id");
        assertNotNull(debateId);
    }
    
    @Test
    @Order(2)
    @DisplayName("Should add AI participants to debate")
    void addParticipants() {
        // Add first AI participant
        Map<String, Object> participant1 = Map.of(
            "type", "AI",
            "name", "Claude",
            "modelProvider", "anthropic",
            "modelName", "claude-3-opus-20240229",
            "position", "PRO"
        );
        
        given()
            .spec(authenticatedRequest(authToken, ORG_ID))
            .body(participant1)
            .when()
            .post("/api/v1/debates/{debateId}/participants", debateId)
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("name", equalTo("Claude"));
        
        // Add second AI participant
        Map<String, Object> participant2 = Map.of(
            "type", "AI",
            "name", "GPT-4",
            "modelProvider", "openai",
            "modelName", "gpt-4",
            "position", "CON"
        );
        
        given()
            .spec(authenticatedRequest(authToken, ORG_ID))
            .body(participant2)
            .when()
            .post("/api/v1/debates/{debateId}/participants", debateId)
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("name", equalTo("GPT-4"));
    }
    
    @Test
    @Order(3)
    @DisplayName("Should start the debate")
    void startDebate() {
        given()
            .spec(authenticatedRequest(authToken, ORG_ID))
            .when()
            .post("/api/v1/debates/{debateId}/start", debateId)
            .then()
            .statusCode(200)
            .body("status", equalTo("IN_PROGRESS"))
            .body("currentRound", equalTo(1));
    }
    
    @Test
    @Order(4)
    @DisplayName("Should process debate rounds automatically")
    void processDebateRounds() {
        // Wait for the first round to complete
        await()
            .atMost(30, TimeUnit.SECONDS)
            .pollInterval(2, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response response = given()
                    .spec(authenticatedRequest(authToken, ORG_ID))
                    .when()
                    .get("/api/v1/debates/{debateId}/rounds/current", debateId)
                    .then()
                    .extract()
                    .response();
                
                assertEquals(200, response.statusCode());
                assertNotNull(response.path("responses"));
                assertTrue(response.path("responses.size()") >= 2);
            });
        
        // Check debate progress
        given()
            .spec(authenticatedRequest(authToken, ORG_ID))
            .when()
            .get("/api/v1/debates/{debateId}", debateId)
            .then()
            .statusCode(200)
            .body("currentRound", greaterThanOrEqualTo(1))
            .body("status", equalTo("IN_PROGRESS"));
    }
    
    @Test
    @Order(5)
    @DisplayName("Should handle concurrent audience interactions")
    void audienceInteractions() throws InterruptedException {
        // Simulate multiple audience members voting
        Thread[] threads = new Thread[10];
        
        for (int i = 0; i < threads.length; i++) {
            final int userId = i;
            threads[i] = new Thread(() -> {
                Map<String, Object> vote = Map.of(
                    "responseId", "response-123", // Would be dynamic in real test
                    "voteType", "AGREE",
                    "userId", "audience-" + userId
                );
                
                given()
                    .spec(authenticatedRequest(authToken, ORG_ID))
                    .body(vote)
                    .when()
                    .post("/api/v1/debates/{debateId}/votes", debateId)
                    .then()
                    .statusCode(anyOf(equalTo(201), equalTo(200)));
            });
            threads[i].start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Verify vote count
        given()
            .spec(authenticatedRequest(authToken, ORG_ID))
            .when()
            .get("/api/v1/debates/{debateId}/votes/summary", debateId)
            .then()
            .statusCode(200)
            .body("totalVotes", greaterThanOrEqualTo(10));
    }
    
    @Test
    @Order(6)
    @DisplayName("Should complete debate and generate summary")
    void completeDebate() {
        // Complete the debate
        given()
            .spec(authenticatedRequest(authToken, ORG_ID))
            .when()
            .post("/api/v1/debates/{debateId}/complete", debateId)
            .then()
            .statusCode(200)
            .body("status", equalTo("COMPLETED"))
            .body("summary", notNullValue());
        
        // Verify debate is archived
        given()
            .spec(authenticatedRequest(authToken, ORG_ID))
            .when()
            .get("/api/v1/debates/{debateId}", debateId)
            .then()
            .statusCode(200)
            .body("status", equalTo("COMPLETED"))
            .body("completedAt", notNullValue());
    }
    
    @Test
    @Order(7)
    @DisplayName("Should search debate content")
    void searchDebateContent() {
        given()
            .spec(authenticatedRequest(authToken, ORG_ID))
            .queryParam("q", "test")
            .queryParam("type", "debate")
            .when()
            .get("/api/v1/search")
            .then()
            .statusCode(200)
            .body("results", hasSize(greaterThan(0)))
            .body("results[0].type", equalTo("debate"));
    }
    
    @Test
    @Order(8)
    @DisplayName("Should export debate transcript")
    void exportDebateTranscript() {
        given()
            .spec(authenticatedRequest(authToken, ORG_ID))
            .when()
            .get("/api/v1/debates/{debateId}/export", debateId)
            .then()
            .statusCode(200)
            .contentType("application/pdf")
            .header("Content-Disposition", containsString("attachment"));
    }
}