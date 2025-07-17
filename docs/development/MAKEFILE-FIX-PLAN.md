# Makefile Fix Plan

## Current Issues Identified

1. **Test Directory Structure**
   - `e2e-tests` directory exists but Makefile references both `e2e-tests` and `playwright-tests`
   - No clear separation between different test types
   - Test runner dockerfile needs validation

2. **UI Port Configuration**
   - UI runs on port 3001 (not 3000 as expected)
   - Port conflicts need handling
   - Environment variable consistency

3. **Service Dependencies**
   - Services need proper health checks before UI starts
   - WebSocket ports need verification
   - API endpoints need validation

4. **Test Execution**
   - Docker test runner profile needs verification
   - Local vs Docker test execution clarity
   - Test result collection and reporting

## Fix Strategy

### Phase 1: Environment & Configuration
1. Update `.env.example` with all required variables
2. Add port conflict detection
3. Create health check scripts
4. Validate all service URLs

### Phase 2: Makefile Improvements
1. Add prerequisite checks
2. Improve error handling
3. Add service health verification
4. Create composite commands

### Phase 3: Test Infrastructure
1. Consolidate test directories
2. Create unified test runner
3. Add test evidence collection
4. Implement test reporting

### Phase 4: UI Validation
1. Add UI readiness checks
2. Create screenshot evidence
3. Implement E2E test suite
4. Add regression tests

## Implementation Steps

### 1. Environment Setup
```bash
# Check and create .env if missing
# Validate all required API keys
# Set default ports with conflict detection
```

### 2. Service Health Checks
```bash
# Wait for PostgreSQL
# Wait for Redis
# Wait for all MCP services
# Verify API endpoints
```

### 3. UI Launch Verification
```bash
# Check port availability
# Launch UI with proper environment
# Wait for UI to be ready
# Capture initial state
```

### 4. Test Execution
```bash
# Run unit tests
# Run integration tests
# Run E2E tests with evidence
# Generate reports
```

## Success Criteria

1. **All services start successfully**
   - PostgreSQL healthy
   - Redis healthy
   - All MCP services responding
   - UI accessible

2. **Tests execute properly**
   - All test suites run
   - Evidence collected
   - Reports generated
   - No false failures

3. **UI fully functional**
   - Organization switcher works
   - Debates display correctly
   - Create debate works
   - LLM integration functional

## Evidence Collection

1. **Service Status**
   - Docker ps output
   - Service health checks
   - API response samples

2. **UI Screenshots**
   - Initial load
   - Organization switcher
   - Debate list
   - Create dialog
   - LLM test

3. **Test Results**
   - Test execution logs
   - Pass/fail summary
   - Performance metrics
   - Error details

## Risk Mitigation

1. **Port Conflicts**
   - Dynamic port allocation
   - Port availability checks
   - Fallback ports

2. **Service Failures**
   - Retry mechanisms
   - Clear error messages
   - Recovery procedures

3. **Test Flakiness**
   - Proper waits
   - State cleanup
   - Isolated test runs