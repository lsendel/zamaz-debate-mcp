# Karate API Tests for Zamaz Debate MCP

A comprehensive API testing suite using Karate DSL for the Zamaz Debate MCP microservices platform.

## Overview

This project provides automated API testing for all microservices in the Zamaz Debate MCP platform, including:

- **Authentication Service** - User login, registration, and JWT token management
- **Organization Service** - Multi-tenant organization management
- **LLM Service** - AI model integration and completion APIs
- **Debate Controller** - Debate lifecycle and participant management
- **RAG Service** - Document ingestion and retrieval-augmented generation
- **Template Service** - Debate template management
- **Integration Tests** - Cross-service scenarios and end-to-end flows

## Features

### Test Coverage
- ✅ **Complete API Coverage** - All REST endpoints and WebSocket connections
- ✅ **Authentication & Authorization** - JWT tokens, role-based access control
- ✅ **Multi-tenant Isolation** - Organization-level data separation
- ✅ **Real-time Features** - WebSocket testing for live debates
- ✅ **Performance Testing** - Load testing and response time validation
- ✅ **Security Testing** - Rate limiting, input validation, and vulnerability checks
- ✅ **Data Validation** - JSON schema validation and business rule enforcement
- ✅ **Error Handling** - Comprehensive error scenario testing

### Advanced Features
- 🚀 **Parallel Execution** - Run tests in parallel for faster feedback
- 📊 **Rich Reporting** - HTML and JSON reports with detailed metrics
- 🔧 **Environment Management** - Multiple test environments (dev, ci, performance)
- 🛠️ **Test Data Management** - Automated test data setup and cleanup
- 🔄 **CI/CD Integration** - GitHub Actions workflows for automated testing
- 🐳 **Docker Support** - Containerized test environment setup
- 📈 **Performance Metrics** - Response time and throughput analysis

## Quick Start

### Prerequisites

- **Java 21** or higher
- **Maven 3.8+**
- **Docker & Docker Compose**
- **curl** and **jq** (for scripts)

### Setup

1. **Clone and Navigate**
   ```bash
   cd karate-api-tests
   ```

2. **Setup Test Environment**
   ```bash
   chmod +x scripts/setup-test-env.sh
   ./scripts/setup-test-env.sh
   ```

3. **Start Application Services**
   ```bash
   # From the main project directory
   cd ..
   make start  # or docker-compose up -d
   ```

4. **Run Tests**
   ```bash
   chmod +x scripts/run-tests.sh
   ./scripts/run-tests.sh --tags @smoke
   ```

## Test Execution

### Running Different Test Suites

```bash
# Smoke tests (quick validation)
./scripts/run-tests.sh --tags @smoke

# Full regression suite
./scripts/run-tests.sh --tags @regression

# Security tests
./scripts/run-tests.sh --tags @security --profile security

# Performance tests
./scripts/run-tests.sh --tags @performance --profile performance --parallel 8

# Integration tests
./scripts/run-tests.sh --tags @integration

# Specific service tests
./scripts/run-tests.sh --suite organization
./scripts/run-tests.sh --suite debate
./scripts/run-tests.sh --suite llm
```

### Advanced Options

```bash
# Run with custom parallelism
./scripts/run-tests.sh --parallel 4 --tags @regression

# Run with service startup
./scripts/run-tests.sh --services --clean --tags @smoke

# Run specific environment
./scripts/run-tests.sh --profile ci --parallel 8 --tags "~@slow"
```

### Maven Direct Execution

```bash
# Run all tests
mvn clean test

# Run specific test class
mvn test -Dtest=AuthTestRunner

# Run with custom environment
mvn test -Dkarate.env=ci -Dparallel.threads=4

# Run with specific tags
mvn test -Dkarate.options="--tags @smoke"
```

## Project Structure

```
karate-api-tests/
├── src/test/
│   ├── java/                           # Test runners
│   │   ├── TestRunner.java             # Main test runner
│   │   ├── authentication/             # Auth test runners
│   │   ├── organization/               # Organization test runners
│   │   ├── debate/                     # Debate test runners
│   │   ├── llm/                        # LLM test runners
│   │   ├── rag/                        # RAG test runners
│   │   ├── integration/                # Integration test runners
│   │   ├── performance/                # Performance test runners
│   │   └── security/                   # Security test runners
│   └── resources/
│       ├── karate-config.js            # Global configuration
│       ├── authentication/             # Authentication feature files
│       ├── organization/               # Organization feature files
│       ├── debate/                     # Debate feature files
│       ├── llm/                        # LLM feature files
│       ├── rag/                        # RAG feature files
│       ├── integration/                # Integration feature files
│       ├── performance/                # Performance feature files
│       ├── security/                   # Security feature files
│       ├── fixtures/                   # Test fixtures and utilities
│       │   ├── auth-fixtures.js        # Authentication utilities
│       │   ├── organization-fixtures.js # Organization utilities
│       │   ├── debate-fixtures.js      # Debate utilities
│       │   └── common-fixtures.js      # Common utilities
│       ├── test-data/                  # Test data files
│       │   ├── organizations.json      # Organization test data
│       │   ├── users.json              # User test data
│       │   ├── debates.json            # Debate test data
│       │   └── templates.json          # Template test data
│       ├── schemas/                    # JSON schemas
│       │   ├── debate-schema.json      # Debate response schema
│       │   ├── organization-schema.json # Organization response schema
│       │   └── error-schema.json       # Error response schema
│       └── utils/                      # Utility functions
│           ├── auth-utils.js           # Authentication utilities
│           ├── data-utils.js           # Data manipulation utilities
│           ├── websocket-utils.js      # WebSocket utilities
│           └── performance-utils.js    # Performance utilities
├── scripts/                            # Utility scripts
│   ├── run-tests.sh                    # Test execution script
│   ├── setup-test-env.sh               # Environment setup script
│   ├── generate-evidence-reports.sh    # Evidence generation script
│   └── cleanup-test-data.sh            # Data cleanup script
├── test-evidence-reports/              # 📊 TEST EVIDENCE REPORTS (GENERATED)
│   ├── index.html                      # Main evidence dashboard
│   ├── consolidated-evidence-*.html    # Consolidated service reports
│   ├── html/                           # Individual service HTML reports
│   ├── json/                           # Raw evidence data (JSON)
│   └── summary/                        # Text summaries
├── docker-compose.test.yml             # Test environment Docker Compose
├── pom.xml                             # Maven configuration
├── EVIDENCE_GENERATION_GUIDE.md        # Detailed evidence generation guide
└── README.md                           # This file
```

## Configuration

### Environment Variables

The tests use different configurations based on the environment:

```javascript
// karate-config.js
if (env === 'dev') {
    config.baseUrl = 'http://localhost';
    config.debug = true;
    config.parallel = 1;
} else if (env === 'ci') {
    config.baseUrl = 'http://localhost';
    config.debug = false;
    config.parallel = 4;
} else if (env === 'performance') {
    config.baseUrl = 'http://localhost';
    config.parallel = 8;
    config.performance.users = 100;
}
```

### Service Configuration

```javascript
// Service URLs and ports
config.serviceUrls = {
    gateway: 'http://localhost:8080',
    organization: 'http://localhost:5005',
    llm: 'http://localhost:5002',
    controller: 'http://localhost:5013',
    rag: 'http://localhost:5004',
    template: 'http://localhost:5006'
};
```

## Test Data Management

### Fixtures

The framework provides reusable fixtures for common operations:

```javascript
// Authentication
var auth = authFixtures.login('user@example.com', 'password');
var headers = authFixtures.getAuthHeaders(auth.token);

// Organization
var org = orgFixtures.createOrganization({name: 'Test Org'});
var user = orgFixtures.addUserToOrganization(org.id, {email: 'user@test.com'});

// Debate
var debate = debateFixtures.createDebate({topic: 'Test Topic'});
var participant = debateFixtures.addParticipant(debate.id, user.id);
```

### Test Data

Structured test data is stored in JSON files:

```json
// organizations.json
{
  "defaultOrganization": {
    "name": "Test Organization",
    "settings": {
      "allowPublicDebates": true,
      "maxDebateParticipants": 10
    },
    "tier": "ENTERPRISE"
  }
}
```

## Feature Examples

### Authentication Tests

```gherkin
@smoke
Scenario: Successful user login
  Given path '/api/v1/auth/login'
  And request { email: 'user@example.com', password: 'password' }
  When method post
  Then status 200
  And match response.token == '#string'
  And match response.user.email == 'user@example.com'
```

### Organization Tests

```gherkin
@regression
Scenario: Create organization with full setup
  * def orgData = { name: 'New Organization', tier: 'PRO' }
  * def org = orgFixtures.createOrganization(orgData)
  * def users = orgFixtures.addMultipleUsers(org.id, 5)
  * match users.length == 5
  * match org.tier == 'PRO'
```

### Debate Tests

```gherkin
@integration
Scenario: Complete debate flow
  * def debate = debateFixtures.createDebate({topic: 'AI Ethics'})
  * def participants = debateFixtures.addParticipants(debate.id, 4)
  * def startedDebate = debateFixtures.startDebate(debate.id)
  * def responses = debateFixtures.submitResponses(debate.id, participants)
  * def completedDebate = debateFixtures.completeDebate(debate.id)
  * match completedDebate.status == 'COMPLETED'
```

### WebSocket Tests

```gherkin
@realtime
Scenario: Real-time debate updates
  * def debate = debateFixtures.createDebate()
  * def wsConnection = websocketUtils.connect(debate.id)
  * def response = debateFixtures.submitResponse(debate.id, 'Test response')
  * def wsMessage = websocketUtils.waitForMessage(wsConnection, 5000)
  * match wsMessage.type == 'debate.update'
  * match wsMessage.data.newResponse == '#object'
```

## Performance Testing

### Load Testing

```gherkin
@performance
Scenario: Concurrent user registration
  * def users = []
  * for (var i = 0; i < 100; i++) users.push({email: 'user' + i + '@test.com'})
  * def startTime = Date.now()
  * def results = karate.parallel(users, function(user) {
      return karate.call('register-user.feature', user);
    })
  * def endTime = Date.now()
  * def duration = endTime - startTime
  * assert duration < 10000  // Should complete within 10 seconds
```

### Response Time Validation

```gherkin
@performance
Scenario: API response time validation
  * def startTime = Date.now()
  Given path '/api/v1/organizations'
  When method get
  Then status 200
  * def endTime = Date.now()
  * def responseTime = endTime - startTime
  * assert responseTime < 500  // Should respond within 500ms
```

## Security Testing

### Authentication Security

```gherkin
@security
Scenario: Rate limiting on login attempts
  * def invalidCredentials = {email: 'test@test.com', password: 'wrong'}
  * def attempts = []
  * for (var i = 0; i < 35; i++) attempts.push(invalidCredentials)
  * def results = karate.parallel(attempts, function(creds) {
      return karate.call('login.feature', creds);
    })
  * def rateLimitedCount = results.filter(r => r.responseStatus == 429).length
  * assert rateLimitedCount > 0  // Should have rate-limited requests
```

### Input Validation

```gherkin
@security
Scenario: SQL injection prevention
  * def maliciousInput = "'; DROP TABLE users; --"
  Given path '/api/v1/organizations'
  And param search = maliciousInput
  When method get
  Then status 400
  And match response.error.code == 'INVALID_INPUT'
```

## CI/CD Integration

### GitHub Actions

```yaml
name: Karate API Tests

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
      - name: Setup Test Environment
        run: ./karate-api-tests/scripts/setup-test-env.sh
      - name: Run Tests
        run: ./karate-api-tests/scripts/run-tests.sh --profile ci --parallel 4 --tags "~@slow"
      - name: Upload Test Reports
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: test-reports
          path: karate-api-tests/target/karate-reports/
```

## Evidence Generation and Reporting

### 📊 Evidence Reports

This framework generates comprehensive evidence reports for all services:

```bash
# Generate evidence reports for all services
./scripts/generate-evidence-reports.sh

# Generate evidence for specific services only
./scripts/generate-evidence-reports.sh --services "authentication,organization,llm"

# Custom output directory
./scripts/generate-evidence-reports.sh --output-dir "custom-reports"
```

### 🌐 Viewing Evidence Reports

After generation, reports are located in `test-evidence-reports/`:

```bash
# Method 1: Direct browser access
open test-evidence-reports/index.html

# Method 2: HTTP Server (recommended)
cd test-evidence-reports
python3 -m http.server 8080
# Then open: http://localhost:8080

# Method 3: VS Code Live Server
code test-evidence-reports
# Use Live Server extension on index.html
```

### 📋 Evidence Report Structure

- **📋 index.html** - Main dashboard with links to all reports
- **🔍 consolidated-evidence-*.html** - Overview of all services  
- **🚀 html/** - Individual service evidence reports
- **📄 json/** - Raw evidence data for programmatic access
- **📝 summary/** - Text summaries for quick review

### 📊 What Evidence Reports Include

- **API Coverage**: All endpoints tested with request/response examples
- **Performance Metrics**: Response times, throughput, and load testing results
- **Security Validation**: Authentication, authorization, and data protection evidence
- **Integration Testing**: Cross-service communication and workflow validation
- **Error Handling**: Comprehensive error scenario testing
- **Quality Metrics**: Validation results and test coverage statistics

### HTML Reports

After test execution, detailed HTML reports are generated:

```bash
# View basic Karate reports
open target/karate-reports/karate-summary.html

# View comprehensive evidence reports
open test-evidence-reports/index.html
```

### JSON Reports

Machine-readable reports for CI/CD integration:

```bash
# Parse test results
jq '.scenariosPassed' target/karate-reports/karate-summary.json
jq '.scenariosFailed' target/karate-reports/karate-summary.json
jq '.elapsedTime' target/karate-reports/karate-summary.json

# Parse evidence data
jq '.summary' test-evidence-reports/json/authentication-evidence-*.json
jq '.performance.totalRequests' test-evidence-reports/json/llm-evidence-*.json
```

## Best Practices

### Test Organization

1. **Use Tags** - Organize tests with meaningful tags (@smoke, @regression, @security)
2. **Parallel Execution** - Run tests in parallel for faster feedback
3. **Data Isolation** - Each test should clean up its own data
4. **Reusable Fixtures** - Use fixtures for common operations
5. **Environment Separation** - Use different configurations for different environments

### Test Data

1. **Generated Data** - Use dynamic data generation to avoid conflicts
2. **Test Isolation** - Each test should be independent
3. **Cleanup** - Always clean up test data after execution
4. **Realistic Data** - Use realistic test data that matches production patterns

### Performance

1. **Parallel Execution** - Use parallel execution for better performance
2. **Connection Pooling** - Reuse HTTP connections when possible
3. **Selective Testing** - Use tags to run only relevant tests
4. **Resource Management** - Clean up resources after tests

## Troubleshooting

### Common Issues

1. **Service Not Running**
   ```bash
   # Check service health
   curl -f http://localhost:8080/health
   
   # Restart services
   docker-compose restart
   ```

2. **Database Connection Issues**
   ```bash
   # Check PostgreSQL
   docker exec postgres-test pg_isready -U test_user
   
   # Check database
   docker exec postgres-test psql -U test_user -d test_debate_db -c "SELECT 1;"
   ```

3. **Test Data Conflicts**
   ```bash
   # Clean test data
   ./scripts/cleanup-test-data.sh
   
   # Reset database
   ./scripts/setup-test-env.sh --cleanup
   ./scripts/setup-test-env.sh
   ```

### Debug Mode

```bash
# Run tests with debug output
mvn test -Dkarate.env=dev -Dkarate.options="--tags @smoke"

# Run single test with debug
mvn test -Dtest=AuthTestRunner -Dkarate.env=dev
```

## Contributing

1. **Add New Tests** - Create feature files in the appropriate directory
2. **Update Fixtures** - Add reusable utilities to fixture files
3. **Document Changes** - Update README and inline documentation
4. **Test Coverage** - Ensure new features have comprehensive test coverage
5. **Performance Impact** - Consider performance impact of new tests

## Support

For issues and questions:

1. Check the troubleshooting section above
2. Review the existing test examples
3. Check the Karate documentation: https://karatelabs.github.io/karate/
4. Create an issue in the project repository

## License

This project is part of the Zamaz Debate MCP platform and follows the same licensing terms.