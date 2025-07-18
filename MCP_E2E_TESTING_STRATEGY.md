# Debate Application E2E Testing Strategy

## Overview
This document outlines the comprehensive end-to-end testing strategy for the debate application using Playwright. The tests will cover real integration scenarios without mocking services.

## Testing Architecture

### Test Environment Setup
- **Frontend**: React application running on port 3000
- **Backend Services**: 
  - API Gateway: Port 8080
  - Debate Service: Port 8083
  - Organization Service: Port 8084
  - LLM Service: Port 8085
  - Context Service: Port 8086
  - Controller Service: Port 8087
- **Infrastructure**:
  - PostgreSQL: Port 5432
  - Redis: Port 6379
  - Kafka: Port 9092

### Main Debate Scenarios to Test

#### 1. Debate Creation Flow
- **Scenario**: User creates a new debate
- **Steps**:
  1. Login/Authentication
  2. Navigate to debate creation
  3. Fill debate details (topic, format, organization)
  4. Configure settings (max rounds, time limits)
  5. Save and verify debate created
- **Evidence**: Screenshots of creation form, API responses, database entries

#### 2. Participant Management
- **Scenario**: Adding human and AI participants
- **Steps**:
  1. Open existing debate
  2. Add human participants
  3. Configure AI participants (provider, model, temperature)
  4. Verify participant constraints
  5. Test position balance (for/against)
- **Evidence**: Participant list screenshots, WebSocket events

#### 3. Real-time Debate Interaction
- **Scenario**: Live debate with multiple participants
- **Steps**:
  1. Start debate (status transition)
  2. Submit arguments in turns
  3. Real-time updates via WebSocket
  4. Vote on responses
  5. Add comments
- **Evidence**: WebSocket message logs, real-time screenshots, vote counts

#### 4. Round Management
- **Scenario**: Progress through debate rounds
- **Steps**:
  1. Start first round
  2. All participants submit responses
  3. Auto-advance to next round
  4. Enforce time limits
  5. Complete final round
- **Evidence**: Round progression screenshots, timing logs

#### 5. Quality Analysis
- **Scenario**: AI-powered debate analysis
- **Steps**:
  1. Complete a debate
  2. Trigger quality analysis
  3. Review argument scores
  4. Check coherence/sentiment metrics
  5. View overall debate grade
- **Evidence**: Analysis reports, scoring visualizations

#### 6. Export and Reporting
- **Scenario**: Export debate in various formats
- **Steps**:
  1. Navigate to completed debate
  2. Export as JSON
  3. Export as PDF
  4. Export as Markdown
  5. Verify export contents
- **Evidence**: Exported files, format comparisons

#### 7. Error Handling and Resilience
- **Scenario**: System behavior under stress
- **Steps**:
  1. Test rate limiting
  2. WebSocket disconnection/reconnection
  3. AI provider failures
  4. Concurrent user actions
- **Evidence**: Error messages, recovery logs

#### 8. MCP Tool Integration
- **Scenario**: Using MCP endpoints
- **Steps**:
  1. Create debate via MCP tool
  2. List debates
  3. Submit turn via MCP
  4. Retrieve debate details
- **Evidence**: MCP request/response logs

## Test Implementation Plan

### Phase 1: Setup (Day 1)
- Install Playwright
- Configure test environment
- Create test utilities and helpers
- Set up test data fixtures

### Phase 2: Core Scenarios (Days 2-3)
- Implement debate creation tests
- Implement participant management tests
- Implement real-time interaction tests
- Implement round management tests

### Phase 3: Advanced Scenarios (Days 4-5)
- Implement quality analysis tests
- Implement export functionality tests
- Implement error handling tests
- Implement MCP integration tests

### Phase 4: Evidence Collection (Day 6)
- Run all test suites
- Collect screenshots and logs
- Generate test reports
- Create evidence documentation

## Success Criteria
- All main scenarios pass with 100% success rate
- Screenshots capture key user interactions
- WebSocket events are properly logged
- Database state changes are verified
- No mocked services - all tests run against real services
- Performance metrics are within acceptable ranges
- Error scenarios are handled gracefully

## Deliverables
1. Playwright test suite covering all scenarios
2. Test execution reports with pass/fail status
3. Screenshot evidence for each scenario
4. Performance metrics and timing data
5. WebSocket event logs
6. Database state verification results