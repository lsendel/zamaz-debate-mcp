# Integration Testing Plan

## Overview

This document outlines the comprehensive integration testing strategy for the zamaz-debate-mcp project. The plan focuses on testing service interactions, data flow, and end-to-end scenarios while maintaining fast feedback cycles and reliable test execution.

## Integration Testing Architecture

### Test Layers

1. **Contract Tests**: API contract validation between services
2. **Service Integration Tests**: Testing service interactions with real implementations
3. **End-to-End Tests**: Complete user journey testing
4. **Performance Integration Tests**: Load testing of integrated services
5. **Chaos Tests**: Failure injection and resilience testing

### Test Environment Strategy

#### TestContainers Integration
- **Database Containers**: PostgreSQL for each service requiring persistence
- **Redis Container**: Shared cache layer
- **Service Containers**: Lightweight service instances for integration testing
- **External Service Mocks**: WireMock containers for external dependencies

#### Test Data Management
- **Test Data Builder**: Consistent test data creation across services
- **Data Seeding**: Pre-populate databases with realistic test data
- **Data Cleanup**: Automatic cleanup between test runs
- **Test Isolation**: Each test gets its own data context

## Service Integration Testing

### 1. Organization ↔ Security Integration
**Test Scenarios:**
- User authentication within organization context
- Role-based access control across organizations
- Multi-tenant data isolation
- Organization switching security

**Implementation:**
```java
@IntegrationTest
@TestMethodOrder(OrderAnnotation.class)
class OrganizationSecurityIntegrationTest {
    
    @Test
    @Order(1)
    void shouldAuthenticateUserInOrganization() {
        // Test user login with organization context
    }
    
    @Test
    @Order(2) 
    void shouldEnforceOrganizationIsolation() {
        // Test data isolation between orgs
    }
}
```

### 2. Controller ↔ LLM Integration
**Test Scenarios:**
- Debate orchestration with LLM responses
- Provider failover and retry logic
- Token usage tracking and cost calculation
- Response quality validation

### 3. RAG ↔ LLM Integration
**Test Scenarios:**
- Context retrieval and LLM prompt enhancement
- Embedding generation and similarity search
- Knowledge base updates and cache invalidation
- Multi-modal content processing

### 4. Gateway ↔ All Services Integration
**Test Scenarios:**
- Request routing and load balancing
- Authentication and authorization flow
- Rate limiting across services
- WebSocket connection management

## End-to-End Test Scenarios

### Scenario 1: Complete Debate Creation Flow
```gherkin
Feature: Debate Creation
  Scenario: User creates and manages a debate
    Given user is authenticated in organization "test-org"
    When user creates a debate with topic "AI Ethics"
    And user adds participants Claude and ChatGPT
    And user starts the debate
    Then debate should be created successfully
    And participants should receive initial prompts
    And debate status should be "IN_PROGRESS"
```

### Scenario 2: Multi-Organization User Journey
```gherkin
Feature: Multi-Organization Access
  Scenario: User switches between organizations
    Given user has access to organizations "org-1" and "org-2"
    When user switches to organization "org-1"
    Then user should see debates from "org-1" only
    When user switches to organization "org-2"
    Then user should see debates from "org-2" only
```

### Scenario 3: LLM Provider Failover
```gherkin
Feature: LLM Provider Resilience
  Scenario: Automatic provider failover
    Given debate is using OpenAI as primary provider
    When OpenAI becomes unavailable
    Then system should switch to Claude automatically
    And debate should continue without interruption
```

## Performance Integration Testing

### Load Testing Scenarios
1. **Concurrent User Load**: 100 concurrent users creating debates
2. **High Throughput**: 1000 requests/second across all endpoints
3. **Large Debate Management**: Debates with 10+ participants
4. **Bulk Operations**: Mass debate creation and deletion

### Performance Benchmarks
- **Response Time**: 95th percentile < 500ms for API calls
- **Throughput**: Handle 1000 concurrent debates
- **Resource Usage**: Memory < 2GB per service instance
- **Database Performance**: Query execution < 100ms

## Test Infrastructure

### TestContainers Configuration
```java
@TestConfiguration
public class IntegrationTestConfig {
    
    @Bean
    @Primary
    public PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("mcp_test")
            .withUsername("test")
            .withPassword("test");
    }
    
    @Bean
    @Primary 
    public GenericContainer<?> redisContainer() {
        return new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);
    }
}
```

### WireMock Service Mocks
```java
@Component
public class ExternalServiceMocks {
    
    private WireMockServer llmMockServer;
    
    @PostConstruct
    public void setup() {
        llmMockServer = new WireMockServer(8089);
        llmMockServer.start();
        
        // Mock OpenAI API
        llmMockServer.stubFor(post(urlPathEqualTo("/v1/chat/completions"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("openai-response.json")));
    }
}
```

### Contract Testing with Pact

#### Consumer Tests (Frontend/Services)
```java
@ExtendWith(PactConsumerTestExt.class)
class DebateServiceContractTest {
    
    @Pact(consumer = "debate-ui")
    public RequestResponsePact createDebatePact(PactDslWithProvider builder) {
        return builder
            .given("user is authenticated")
            .uponReceiving("create debate request")
            .path("/api/debates")
            .method("POST")
            .body(newJsonBody(body -> {
                body.stringType("title", "Test Debate");
                body.stringType("description", "Test Description");
            }).build())
            .willRespondWith()
            .status(201)
            .body(newJsonBody(body -> {
                body.stringType("id", "debate-123");
                body.stringType("status", "CREATED");
            }).build())
            .toPact();
    }
}
```

#### Provider Tests (Backend Services)
```java
@Provider("debate-service")
@PactBroker(url = "https://pact-broker.example.com")
class DebateServiceProviderTest {
    
    @BeforeEach
    void setUp(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", 8080));
    }
    
    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }
    
    @State("user is authenticated")
    void userIsAuthenticated() {
        // Setup authenticated user state
    }
}
```

## Chaos Engineering Tests

### Failure Injection Scenarios
1. **Network Partitions**: Simulate network splits between services
2. **Service Crashes**: Random service shutdowns during operations
3. **Database Failures**: Connection timeouts and query failures
4. **Resource Exhaustion**: CPU and memory pressure testing
5. **Latency Injection**: Artificial delays in service responses

### Chaos Test Implementation
```java
@ChaosTest
class ServiceResilienceTest {
    
    @Test
    void shouldHandleServiceFailure() {
        // Start normal operation
        String debateId = createDebate();
        
        // Inject failure
        chaosMonkey.shutdown("mcp-llm");
        
        // Verify graceful degradation
        assertThat(getDebateStatus(debateId))
            .isEqualTo("PAUSED");
        
        // Restore service
        chaosMonkey.restore("mcp-llm");
        
        // Verify recovery
        assertThat(getDebateStatus(debateId))
            .isEqualTo("IN_PROGRESS");
    }
}
```

## Test Data Management

### Test Data Builders
```java
public class IntegrationTestDataBuilder {
    
    public static Organization.OrganizationBuilder defaultOrganization() {
        return Organization.builder()
            .id("test-org-" + UUID.randomUUID())
            .name("Test Organization")
            .active(true);
    }
    
    public static User.UserBuilder defaultUser(String organizationId) {
        return User.builder()
            .id("user-" + UUID.randomUUID())
            .email("test@example.com")
            .organizationId(organizationId)
            .roles(Set.of(Role.USER));
    }
    
    public static Debate.DebateBuilder defaultDebate(String organizationId) {
        return Debate.builder()
            .title("Integration Test Debate")
            .description("Test debate for integration testing")
            .organizationId(organizationId)
            .status(DebateStatus.CREATED);
    }
}
```

### Test Scenarios Repository
```java
@Component
public class TestScenarios {
    
    public TestScenario.MultiUserDebate multiUserDebate() {
        return TestScenario.builder()
            .withOrganization(defaultOrganization())
            .withUsers(3) // Create 3 test users
            .withDebate(defaultDebate())
            .withLLMProviders("openai", "claude")
            .build();
    }
    
    public TestScenario.CrossOrganizationAccess crossOrgAccess() {
        return TestScenario.builder()
            .withOrganizations(2) // Create 2 orgs
            .withUserInMultipleOrgs()
            .withPermissionTesting()
            .build();
    }
}
```

## Continuous Integration

### GitHub Actions Integration
```yaml
name: Integration Tests
on: [push, pull_request]

jobs:
  integration-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          
      - name: Start test services
        run: docker-compose -f docker-compose-test.yml up -d
        
      - name: Wait for services
        run: ./scripts/wait-for-services.sh
        
      - name: Run integration tests
        run: mvn verify -P integration-test
        
      - name: Publish test results
        uses: dorny/test-reporter@v1
        if: always()
        with:
          name: Integration Test Results
          path: '**/target/surefire-reports/*.xml'
          reporter: java-junit
```

## Monitoring and Observability

### Test Observability
- **Test Execution Metrics**: Success rates, execution times, flaky test detection
- **Service Health During Tests**: Monitor service performance during test execution
- **Resource Usage**: Track memory, CPU, and network usage during tests
- **Test Coverage**: Integration test coverage across service boundaries

### Test Reporting
- **Real-time Dashboards**: Live test execution status
- **Trend Analysis**: Test performance over time
- **Failure Analysis**: Automatic categorization of test failures
- **Coverage Reports**: Visual representation of integration test coverage

## Implementation Timeline

### Phase 1 (Week 1-2): Foundation
- Set up TestContainers infrastructure
- Implement basic service integration tests
- Create test data management framework

### Phase 2 (Week 3-4): Service Integration
- Implement all service-to-service integration tests
- Add contract testing with Pact
- Create performance integration tests

### Phase 3 (Week 5-6): End-to-End & Chaos
- Implement complete E2E scenarios
- Add chaos engineering tests
- Set up monitoring and reporting

### Phase 4 (Week 7-8): Optimization
- Optimize test execution times
- Implement parallel test execution
- Add comprehensive documentation

## Success Criteria

- **Test Execution Time**: Full integration test suite < 15 minutes
- **Test Reliability**: < 1% flaky test rate
- **Coverage**: 100% of critical integration paths covered
- **Feedback Time**: Test results available within 20 minutes of commit
- **Developer Experience**: Easy to run locally and debug failures