package com.zamaz.mcp.testing.contract;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Consumer contract test for Debate Service interactions
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "DebateController", pactVersion = PactSpecVersion.V3)
public class DebateServiceConsumerPactTest {
    
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }
    
    @Pact(consumer = "DebateUI", provider = "DebateController")
    public RequestResponsePact createDebatePact(PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer test-token");
        headers.put("X-Organization-ID", "test-org-123");
        
        return builder
            .given("User is authenticated and authorized")
            .uponReceiving("A request to create a new debate")
                .path("/api/v1/debates")
                .method("POST")
                .headers(headers)
                .body(new PactDslJsonBody()
                    .stringType("title", "AI Ethics Debate")
                    .stringType("topic", "Should AI be regulated?")
                    .stringType("description", "A debate about AI regulation")
                    .integerType("maxRounds", 5)
                    .stringType("format", "standard")
                    .object("settings")
                        .integerType("turnTimeoutSeconds", 300)
                        .booleanType("allowAudience", true)
                    .closeObject()
                )
            .willRespondWith()
                .status(201)
                .headers(headers)
                .body(new PactDslJsonBody()
                    .uuid("id", "550e8400-e29b-41d4-a716-446655440000")
                    .stringType("title", "AI Ethics Debate")
                    .stringType("topic", "Should AI be regulated?")
                    .stringType("status", "CREATED")
                    .datetime("createdAt", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    .integerType("participantCount", 0)
                )
            .toPact();
    }
    
    @Pact(consumer = "DebateUI", provider = "DebateController")
    public RequestResponsePact getDebatesPact(PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer test-token");
        headers.put("X-Organization-ID", "test-org-123");
        
        return builder
            .given("Organization has debates")
            .uponReceiving("A request to get debates list")
                .path("/api/v1/debates")
                .method("GET")
                .headers(headers)
                .query("status=ACTIVE&page=0&size=10")
            .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                    .minArrayLike("content", 1)
                        .uuid("id")
                        .stringType("title")
                        .stringType("topic")
                        .stringType("status")
                        .datetime("createdAt", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                        .integerType("participantCount")
                    .closeArray()
                    .integerType("totalElements", 10)
                    .integerType("totalPages", 1)
                    .integerType("number", 0)
                )
            .toPact();
    }
    
    @Pact(consumer = "DebateUI", provider = "DebateController")
    public RequestResponsePact addParticipantPact(PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer test-token");
        headers.put("X-Organization-ID", "test-org-123");
        
        return builder
            .given("Debate exists and is accepting participants")
            .uponReceiving("A request to add an AI participant")
                .path("/api/v1/debates/550e8400-e29b-41d4-a716-446655440000/participants")
                .method("POST")
                .headers(headers)
                .body(new PactDslJsonBody()
                    .stringType("type", "AI")
                    .stringType("name", "Claude")
                    .stringType("modelProvider", "anthropic")
                    .stringType("modelName", "claude-3-opus-20240229")
                )
            .willRespondWith()
                .status(201)
                .headers(headers)
                .body(new PactDslJsonBody()
                    .uuid("id")
                    .uuid("debateId", "550e8400-e29b-41d4-a716-446655440000")
                    .stringType("type", "AI")
                    .stringType("name", "Claude")
                    .datetime("joinedAt", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                )
            .toPact();
    }
    
    @Test
    @PactTestFor(pactMethod = "createDebatePact")
    void testCreateDebate(MockServer mockServer) {
        RestAssured.baseURI = mockServer.getUrl();
        
        Map<String, Object> debateRequest = new HashMap<>();
        debateRequest.put("title", "AI Ethics Debate");
        debateRequest.put("topic", "Should AI be regulated?");
        debateRequest.put("description", "A debate about AI regulation");
        debateRequest.put("maxRounds", 5);
        debateRequest.put("format", "standard");
        
        Map<String, Object> settings = new HashMap<>();
        settings.put("turnTimeoutSeconds", 300);
        settings.put("allowAudience", true);
        debateRequest.put("settings", settings);
        
        Response response = given()
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer test-token")
            .header("X-Organization-ID", "test-org-123")
            .body(debateRequest)
            .when()
            .post("/api/v1/debates")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("title", equalTo("AI Ethics Debate"))
            .body("status", equalTo("CREATED"))
            .extract()
            .response();
    }
    
    @Test
    @PactTestFor(pactMethod = "getDebatesPact")
    void testGetDebates(MockServer mockServer) {
        RestAssured.baseURI = mockServer.getUrl();
        
        given()
            .header("Authorization", "Bearer test-token")
            .header("X-Organization-ID", "test-org-123")
            .queryParam("status", "ACTIVE")
            .queryParam("page", 0)
            .queryParam("size", 10)
            .when()
            .get("/api/v1/debates")
            .then()
            .statusCode(200)
            .body("content", hasSize(greaterThanOrEqualTo(1)))
            .body("content[0].id", notNullValue())
            .body("content[0].title", notNullValue())
            .body("totalElements", greaterThanOrEqualTo(1));
    }
    
    @Test
    @PactTestFor(pactMethod = "addParticipantPact")
    void testAddParticipant(MockServer mockServer) {
        RestAssured.baseURI = mockServer.getUrl();
        
        Map<String, Object> participantRequest = new HashMap<>();
        participantRequest.put("type", "AI");
        participantRequest.put("name", "Claude");
        participantRequest.put("modelProvider", "anthropic");
        participantRequest.put("modelName", "claude-3-opus-20240229");
        
        given()
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer test-token")
            .header("X-Organization-ID", "test-org-123")
            .body(participantRequest)
            .when()
            .post("/api/v1/debates/550e8400-e29b-41d4-a716-446655440000/participants")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("type", equalTo("AI"))
            .body("name", equalTo("Claude"));
    }
}