# GitHub Integration E2E Tests

This directory contains comprehensive end-to-end tests for the GitHub Integration service. These tests validate the complete workflow from webhook reception to pull request review completion.

## Test Structure

### Test Classes

1. **GitHubIntegrationE2ETest**
   - Complete PR review workflow (happy path)
   - Installation webhook processing
   - PR closed webhook processing
   - Configuration changes impact
   - Analytics data collection
   - Error scenarios and recovery
   - Webhook health and monitoring

2. **PullRequestReviewFlowE2ETest**
   - Complex PR review flows with multiple updates
   - Concurrent PR processing
   - Review analysis with different issue types
   - Review failure and recovery
   - Branch pattern filtering
   - Review comment management

3. **PerformanceE2ETest**
   - High volume webhook processing
   - Memory usage under load
   - Database performance under load
   - GitHub API rate limiting handling
   - Concurrent installation processing

### Test Infrastructure

- **TestContainers**: PostgreSQL and Redis containers for isolated testing
- **WireMock**: GitHub API mocking for controlled testing
- **Test Utilities**: Common helpers for test data generation
- **Test Configuration**: Optimized settings for E2E testing

## Running the Tests

### Prerequisites

1. **Docker**: Must be running for TestContainers
2. **Java 17+**: Required for the application
3. **Maven**: For building and running tests
4. **Sufficient Memory**: At least 2GB RAM available

### Quick Start

```bash
# Run all E2E tests
./run-e2e-tests.sh all

# Run specific test types
./run-e2e-tests.sh basic
./run-e2e-tests.sh pr-flow
./run-e2e-tests.sh performance
```

### Manual Execution

```bash
# Run complete test suite
mvn test -Dtest=GitHubIntegrationE2ETestSuite

# Run individual test classes
mvn test -Dtest=GitHubIntegrationE2ETest
mvn test -Dtest=PullRequestReviewFlowE2ETest
mvn test -Dtest=PerformanceE2ETest
```

### Maven Profiles

```bash
# Run with specific profile
mvn test -Dspring.profiles.active=e2e-test

# Run with debug logging
mvn test -Dlogging.level.com.zamaz.mcp.github=DEBUG
```

## Test Scenarios

### 1. Happy Path Workflow

**Scenario**: Complete PR review workflow
- ✅ Webhook received and validated
- ✅ Installation and repository configuration verified
- ✅ PR analysis triggered asynchronously
- ✅ Review results saved to database
- ✅ Comments posted to GitHub
- ✅ Notifications sent

**Validation**:
- Review record created with correct status
- Comments and issues saved
- GitHub API calls made
- Database state consistent

### 2. Installation Management

**Scenario**: GitHub app installation lifecycle
- ✅ Installation created webhook
- ✅ Installation deleted webhook
- ✅ Installation suspended/unsuspended
- ✅ Repository access changes

**Validation**:
- Installation records properly managed
- Repository configurations updated
- Webhook processing continues for active installations

### 3. Configuration Changes

**Scenario**: Repository configuration impact
- ✅ Auto-review enabled/disabled
- ✅ Branch pattern filtering
- ✅ Notification settings
- ✅ Real-time configuration changes

**Validation**:
- Configuration changes take effect immediately
- Existing reviews not affected
- New webhooks respect new configuration

### 4. Error Handling

**Scenario**: Various failure conditions
- ✅ Invalid webhook signatures
- ✅ Unknown installations
- ✅ GitHub API failures
- ✅ Database connectivity issues
- ✅ Rate limiting

**Validation**:
- Graceful error handling
- Proper error responses
- System recovery
- Error logging

### 5. Performance Testing

**Scenario**: High load conditions
- ✅ 100+ concurrent webhooks
- ✅ Multiple installations
- ✅ Database performance
- ✅ Memory usage
- ✅ Response times

**Validation**:
- Throughput > 10 webhooks/second
- Memory usage < 100MB increase
- Database queries < 1 second
- No memory leaks

## Test Data

### Database Setup

The tests use TestContainers with PostgreSQL and Redis:

```sql
-- Test installations
INSERT INTO github_installations (id, account_login, account_type, status, access_token, created_at, updated_at) 
VALUES (99999, 'test-system-user', 'User', 'ACTIVE', 'test-system-token', NOW(), NOW());

-- Test repository configurations
INSERT INTO repository_configs (installation_id, repository_full_name, auto_review_enabled, notifications_enabled, branch_patterns, created_at, updated_at) 
VALUES (99999, 'test-system/test-repo', true, true, 'main,develop', NOW(), NOW());
```

### GitHub API Mocking

WireMock provides controlled GitHub API responses:

```java
// Mock PR details
stubFor(get(urlEqualTo("/repos/test-owner/test-repo/pulls/123"))
    .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody(createPullRequestResponse())));

// Mock post comment
stubFor(post(urlEqualTo("/repos/test-owner/test-repo/issues/123/comments"))
    .willReturn(aResponse()
        .withStatus(201)
        .withHeader("Content-Type", "application/json")
        .withBody(createCommentResponse())));
```

## Test Configuration

### Application Properties

```properties
# Test database configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/github_integration_test
spring.datasource.username=test_user
spring.datasource.password=test_password

# Test Redis configuration
spring.redis.host=localhost
spring.redis.port=6379

# Test GitHub API configuration
github.api.base-url=http://localhost:8080
github.webhook.secret=test-secret-key-for-testing

# Test performance settings
spring.task.execution.pool.core-size=10
spring.task.execution.pool.max-size=20
```

### JVM Settings

```bash
# Recommended JVM settings for E2E tests
export MAVEN_OPTS="-Xmx2g -XX:+UseG1GC -Djava.awt.headless=true"
```

## Monitoring and Metrics

### Test Execution Metrics

The tests collect and validate:
- **Webhook processing time**: Average and 95th percentile
- **Memory usage**: Before and after test execution
- **Database performance**: Query execution times
- **GitHub API calls**: Request count and response times
- **Error rates**: Failed vs successful operations

### Performance Benchmarks

- **Throughput**: > 10 webhooks/second
- **Memory**: < 100MB increase under load
- **Database**: < 1 second for complex queries
- **API Response**: < 500ms average response time

## Troubleshooting

### Common Issues

1. **Docker not running**
   ```bash
   # Check Docker status
   docker info
   
   # Start Docker (macOS)
   open /Applications/Docker.app
   ```

2. **Port conflicts**
   ```bash
   # Check port usage
   lsof -i :5432  # PostgreSQL
   lsof -i :6379  # Redis
   lsof -i :8080  # WireMock
   
   # Kill processes using ports
   sudo kill -9 <PID>
   ```

3. **Memory issues**
   ```bash
   # Increase heap size
   export MAVEN_OPTS="-Xmx4g"
   
   # Check memory usage
   free -h
   ```

4. **Test timeouts**
   ```bash
   # Run with increased timeout
   mvn test -Dtest.timeout=120s
   
   # Run with debug logging
   mvn test -Dlogging.level.com.zamaz.mcp.github=DEBUG
   ```

### Debug Mode

Enable debug logging for detailed test execution:

```bash
# Run with debug logging
mvn test -Dlogging.level.com.zamaz.mcp.github=DEBUG -Dlogging.level.org.testcontainers=DEBUG

# Run specific test with debug
mvn test -Dtest=GitHubIntegrationE2ETest#testCompleteePullRequestReviewWorkflow -Dlogging.level.root=DEBUG
```

## Contributing

### Adding New Tests

1. **Create test class** in `src/test/java/com/zamaz/mcp/github/e2e/`
2. **Use TestContainers** for database setup
3. **Mock GitHub API** with WireMock
4. **Follow naming conventions**: `*E2ETest.java`
5. **Add to test suite** in `GitHubIntegrationE2ETestSuite`

### Test Guidelines

- **Isolation**: Each test should be independent
- **Cleanup**: Use `@Transactional` for database cleanup
- **Assertions**: Use AssertJ for readable assertions
- **Timeouts**: Use Awaitility for async operations
- **Documentation**: Add clear test descriptions

### Code Quality

```bash
# Run code quality checks
mvn checkstyle:check
mvn spotbugs:check
mvn pmd:check

# Format code
mvn spotless:apply
```

## CI/CD Integration

### GitHub Actions

```yaml
name: E2E Tests
on: [push, pull_request]

jobs:
  e2e-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run E2E Tests
        run: ./run-e2e-tests.sh all
```

### Docker Compose

```yaml
version: '3.8'
services:
  github-integration-e2e:
    build: .
    environment:
      - SPRING_PROFILES_ACTIVE=e2e-test
    depends_on:
      - postgres
      - redis
    command: ./run-e2e-tests.sh all
```

## Test Reports

### Surefire Reports

After test execution, detailed reports are available:

```bash
# Generate HTML report
mvn surefire-report:report site:site

# View report
open target/site/surefire-report.html
```

### Test Coverage

```bash
# Generate coverage report
mvn jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

## Support

For issues or questions related to E2E tests:

1. Check the troubleshooting section above
2. Review test logs in `target/surefire-reports/`
3. Verify Docker containers are running properly
4. Check GitHub API mock responses in test output

## References

- [TestContainers Documentation](https://www.testcontainers.org/)
- [WireMock Documentation](http://wiremock.org/)
- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/)
- [AssertJ Documentation](https://assertj.github.io/doc/)
- [Awaitility Documentation](https://github.com/awaitility/awaitility)