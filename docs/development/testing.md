# Testing Strategy

This document outlines the testing strategy for the Zamaz Debate MCP system, including testing approaches, tools, and best practices.

## Testing Levels

### 1. Unit Testing

Unit tests verify the functionality of individual components in isolation.

#### Scope

- Individual classes and methods
- Business logic
- Utility functions
- Data models

#### Tools

- JUnit 5 for Java services
- Mockito for mocking dependencies
- AssertJ for fluent assertions
- Pytest for Python services

#### Best Practices

- Test one concept per test method
- Use descriptive test names that explain the scenario and expected outcome
- Mock external dependencies
- Aim for high code coverage (>80%)
- Test edge cases and error conditions
- Use parameterized tests for testing multiple inputs

#### Example

```java
@Test
void shouldReturnErrorWhenInvalidModelProvided() {
    // Given
    String invalidModel = "invalid-model";
    CompletionRequest request = new CompletionRequest(invalidModel, "test prompt");
    
    // When
    ModelNotFoundException exception = assertThrows(
        ModelNotFoundException.class,
        () -> llmService.complete(request)
    );
    
    // Then
    assertThat(exception.getMessage()).contains(invalidModel);
}
```

### 2. Integration Testing

Integration tests verify that different components work together correctly.

#### Scope

- Service interactions
- Database operations
- External API interactions
- Message processing flows

#### Tools

- Spring Boot Test for Java services
- Testcontainers for database testing
- WireMock for mocking external APIs
- MockMVC for testing REST endpoints

#### Best Practices

- Use test containers for database and infrastructure dependencies
- Clean up test data after each test
- Test realistic scenarios
- Use test profiles for configuration
- Mock external services that are not part of the test scope

#### Example

```java
@SpringBootTest
@Testcontainers
class LlmServiceIntegrationTest {
    
    @Container
    static RedisContainer redisContainer = new RedisContainer("redis:7-alpine");
    
    @Autowired
    private LlmService llmService;
    
    @MockBean
    private ClaudeClient claudeClient;
    
    @Test
    void shouldCacheCompletionResults() {
        // Given
        CompletionRequest request = new CompletionRequest("claude-3-opus", "test prompt");
        CompletionResponse expectedResponse = new CompletionResponse("test response");
        given(claudeClient.complete(any())).willReturn(expectedResponse);
        
        // When
        CompletionResponse firstResponse = llmService.complete(request);
        CompletionResponse secondResponse = llmService.complete(request);
        
        // Then
        assertThat(firstResponse).isEqualTo(expectedResponse);
        assertThat(secondResponse).isEqualTo(expectedResponse);
        verify(claudeClient, times(1)).complete(any()); // Called only once due to caching
    }
}
```

### 3. API Testing

API tests verify that the REST APIs work as expected.

#### Scope

- API endpoints
- Request/response validation
- Error handling
- Authentication and authorization
- Rate limiting

#### Tools

- REST Assured for Java services
- Postman for manual API testing
- Newman for automated Postman collections
- Swagger UI for API exploration

#### Best Practices

- Test all API endpoints
- Test with valid and invalid inputs
- Verify response status codes, headers, and body
- Test authentication and authorization
- Test error responses
- Use data-driven testing for multiple scenarios

#### Example

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CompletionControllerApiTest {
    
    @LocalServerPort
    private int port;
    
    @Test
    void shouldReturnCompletionForValidRequest() {
        // Given
        String requestBody = """
            {
                "model": "claude-3-opus",
                "prompt": "Hello, world!",
                "maxTokens": 100
            }
            """;
        
        // When/Then
        given()
            .port(port)
            .contentType(ContentType.JSON)
            .header("X-Organization-ID", "org-123")
            .body(requestBody)
        .when()
            .post("/api/v1/completions")
        .then()
            .statusCode(200)
            .body("model", equalTo("claude-3-opus"))
            .body("content", not(emptyString()))
            .body("usage.promptTokens", greaterThan(0))
            .body("usage.completionTokens", greaterThan(0));
    }
}
```

### 4. End-to-End Testing

End-to-end tests verify that the entire system works together correctly.

#### Scope

- Complete user flows
- Multi-service interactions
- Real external dependencies (when possible)
- Performance and reliability

#### Tools

- Selenium for web UI testing
- Cypress for modern web UI testing
- JMeter for load testing
- Custom test scripts for complex scenarios

#### Best Practices

- Focus on critical user journeys
- Use realistic test data
- Test in an environment similar to production
- Monitor system behavior during tests
- Clean up test data after tests

#### Example

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DebateEndToEndTest {
    
    @LocalServerPort
    private int port;
    
    @Test
    void shouldCompleteFullDebateCycle() {
        // Create debate
        String debateId = given()
            .port(port)
            .contentType(ContentType.JSON)
            .header("X-Organization-ID", "org-123")
            .body(createDebateRequest())
        .when()
            .post("/api/v1/debates")
        .then()
            .statusCode(201)
            .extract().path("id");
        
        // Add message for first participant
        String messageId = given()
            .port(port)
            .contentType(ContentType.JSON)
            .header("X-Organization-ID", "org-123")
            .body(createMessageRequest("participant-1", "First argument"))
        .when()
            .post("/api/v1/debates/" + debateId + "/messages")
        .then()
            .statusCode(201)
            .extract().path("id");
        
        // Get next turn
        String nextParticipantId = given()
            .port(port)
            .contentType(ContentType.JSON)
            .header("X-Organization-ID", "org-123")
        .when()
            .post("/api/v1/debates/" + debateId + "/next-turn")
        .then()
            .statusCode(200)
            .extract().path("currentParticipant.id");
        
        // Add message for second participant
        given()
            .port(port)
            .contentType(ContentType.JSON)
            .header("X-Organization-ID", "org-123")
            .body(createMessageRequest(nextParticipantId, "Response to first argument"))
        .when()
            .post("/api/v1/debates/" + debateId + "/messages")
        .then()
            .statusCode(201);
        
        // Generate summary
        given()
            .port(port)
            .contentType(ContentType.JSON)
            .header("X-Organization-ID", "org-123")
            .body("{\"strategy\": \"pro_con\"}")
        .when()
            .post("/api/v1/debates/" + debateId + "/summarize")
        .then()
            .statusCode(200)
            .body("content", not(emptyString()));
    }
}
```

## Testing Pyramid

We follow the testing pyramid approach to ensure comprehensive test coverage:

```
    /\
   /  \
  /    \
 / E2E  \
/--------\
/ API     \
/----------\
/ Integration\
/------------\
/    Unit     \
--------------
```

- **Many Unit Tests**: Fast, focused tests for individual components
- **Moderate Integration Tests**: Verify component interactions
- **Some API Tests**: Verify API contracts and behavior
- **Few End-to-End Tests**: Verify critical user journeys

## Test Environments

### Local Development

- In-memory databases
- Mocked external services
- Docker Compose for infrastructure dependencies
- Local development profiles

### CI/CD Pipeline

- Dedicated test databases
- Test containers for infrastructure
- Mocked external services
- CI/CD-specific profiles

### Staging

- Dedicated staging environment
- Real infrastructure components
- Test instances of external services
- Production-like configuration

## Test Data Management

### Test Data Sources

- In-memory data for unit tests
- Test containers with predefined data for integration tests
- Test data generation scripts for API and E2E tests
- Anonymized production data for staging tests

### Test Data Cleanup

- Clean up test data after each test
- Use transaction rollbacks when possible
- Dedicated cleanup scripts for persistent data
- Regular cleanup of staging environment

## Continuous Testing

### CI/CD Integration

- Run unit and integration tests on every pull request
- Run API tests on every merge to main branch
- Run E2E tests on staging before production deployment
- Generate test coverage reports

### Test Automation

- Automated test execution in CI/CD pipeline
- Scheduled nightly test runs for long-running tests
- Automated test data generation and cleanup
- Test result reporting and notifications

## Performance Testing

### Load Testing

- Simulate expected user load
- Measure response times and throughput
- Identify bottlenecks
- Verify scaling capabilities

### Stress Testing

- Test system behavior under extreme load
- Identify breaking points
- Verify graceful degradation
- Test recovery mechanisms

### Tools

- JMeter for load and stress testing
- Gatling for high-throughput testing
- Prometheus and Grafana for monitoring during tests
- Custom scripts for specific scenarios

## Security Testing

### Static Analysis

- SonarQube for code quality and security analysis
- Dependency scanning for vulnerabilities
- Secret scanning in code and configuration
- Compliance checks

### Dynamic Analysis

- OWASP ZAP for security scanning
- Penetration testing
- Authentication and authorization testing
- API security testing

## Test Documentation

### Test Plans

- Test objectives and scope
- Test environments and configurations
- Test data requirements
- Test execution schedule

### Test Cases

- Test scenario description
- Preconditions and setup
- Test steps and expected results
- Postconditions and cleanup

### Test Reports

- Test execution summary
- Test results and status
- Issues and defects found
- Recommendations and follow-up actions

## Best Practices

1. **Write Testable Code**
   - Follow SOLID principles
   - Use dependency injection
   - Separate concerns
   - Avoid static methods and global state

2. **Automate Tests**
   - Automate all repeatable tests
   - Include tests in CI/CD pipeline
   - Maintain test scripts alongside code
   - Use test automation frameworks

3. **Maintain Test Quality**
   - Review and refactor tests regularly
   - Eliminate flaky tests
   - Keep tests independent
   - Document test purpose and approach

4. **Monitor Test Coverage**
   - Track code coverage metrics
   - Identify untested code paths
   - Focus on critical functionality
   - Balance coverage with test value

5. **Continuous Improvement**
   - Learn from test failures
   - Refine testing strategy based on results
   - Share testing knowledge across team
   - Stay updated on testing tools and techniques
