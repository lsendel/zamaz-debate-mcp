# AI Debate System - Comprehensive Test Documentation

## Overview

This document describes the comprehensive test suite created for the AI Debate System. All tests are designed to run against real services without mocking, focusing on testing the core functionality of debates between Claude 3.5 Sonnet and Gemini 2.5 Pro.

## Test Evidence Collection

All test evidence is collected in the `/test_probe` directory:
- **Screenshots**: Captured on failures and key test points
- **Videos**: Full test execution recordings
- **Debate Transcripts**: JSON files with complete debate content
- **Performance Metrics**: Response times and resource usage
- **Test Reports**: HTML, JSON, and JUnit format reports

## Test Structure

### 1. Smoke Tests (`playwright-tests/tests/smoke/smoke.spec.ts`)
- **Purpose**: Quick validation of core functionality
- **Runtime**: < 2 minutes
- **Coverage**:
  - Service health checks
  - UI loading
  - Basic debate creation
  - LLM connectivity

### 2. Comprehensive Debate Tests (`playwright-tests/tests/comprehensive/comprehensive-debate.spec.ts`)
- **Purpose**: Test the core debate functionality between Claude 3.5 Sonnet and Gemini 2.5 Pro
- **Runtime**: ~5 minutes per test
- **Coverage**:
  - Complete debate flow (create → start → turns → complete)
  - Problem-solving discussions
  - AI Ethics debates
  - Debate quality analysis
  - Database persistence verification
  - Performance testing

### 3. UI Component Tests (`playwright-tests/tests/ui/ui-components.spec.ts`)
- **Purpose**: Systematic testing of all UI components
- **Coverage**:
  - Navigation and layout
  - Connection status display
  - Create debate dialog
  - Debate list and filtering
  - Error states
  - Toast notifications
  - Accessibility

### 4. LLM Integration Tests (`playwright-tests/tests/llm/llm-integration.spec.ts`)
- **Purpose**: Test LLM service integration
- **Coverage**:
  - Model availability (Claude, Gemini, OpenAI)
  - Chat completions
  - Streaming responses
  - Model switching
  - Error handling
  - Performance metrics

### 5. Database Verification Tests (`playwright-tests/tests/database/database-verification.spec.ts`)
- **Purpose**: Verify data persistence and integrity
- **Coverage**:
  - Debate metadata persistence
  - Status transitions
  - Turn history tracking
  - Concurrent operations
  - Data constraints
  - Query performance

### 6. Performance Tests (`playwright-tests/tests/performance/performance.spec.ts`)
- **Purpose**: Measure system performance
- **Coverage**:
  - Page load times
  - API response times
  - Concurrent debate handling
  - LLM response latency
  - Memory usage

### 7. E2E Tests (Updated Puppeteer Tests)
- **Purpose**: End-to-end testing with Puppeteer
- **Updated**: Switched from Llama models to Claude/Gemini
- **Location**: `e2e-tests/src/tests/debate-flow.test.ts`

## Running Tests

### Quick Test Commands

```bash
# Run all tests with Docker
make test

# Run specific test suites
cd playwright-tests
npm run test:smoke          # Quick smoke tests
npm run test:comprehensive  # Comprehensive debate tests
npm run test:ui            # UI component tests
npm run test:llm           # LLM integration tests
npm run test:db            # Database tests
npm run test:perf          # Performance tests

# Run with visible browser
npm run test:headed

# Debug mode
npm run test:debug
```

### Test Configuration

#### Environment Variables
```bash
# Required for LLM tests
ANTHROPIC_API_KEY=your_claude_key
GOOGLE_API_KEY=your_gemini_key
OPENAI_API_KEY=your_openai_key (optional)

# Test configuration
BASE_URL=http://localhost:3000
TEST_PROBE_DIR=/Users/lsendel/IdeaProjects/zamaz-debate-mcp/test_probe
```

#### Playwright Configuration
- **Browsers**: Chromium (primary)
- **Workers**: 2 (limited for LLM stability)
- **Retries**: 1-2 attempts per test
- **Timeouts**:
  - Default: 2 minutes
  - LLM operations: 5 minutes
  - Navigation: 1 minute

## Key Test Scenarios

### 1. Claude vs Gemini Debate Test
```typescript
// Creates a debate between Claude 3.5 Sonnet and Gemini 2.5 Pro
// Topic: "How can we design a sustainable city transportation system for 2030?"
// Tracks:
// - Turn generation and quality
// - Response coherence
// - Topic relevance
// - Performance metrics
```

### 2. AI Ethics Certification Debate
```typescript
// Tests different perspectives on AI regulation
// Claude: Ethics advocate
// Gemini: Innovation focus
// Analyzes keyword frequency and argument quality
```

### 3. Concurrent Debate Handling
```typescript
// Creates and runs 3 simultaneous debates
// Measures system stability and performance
// Verifies turn ordering and data integrity
```

## Evidence Collection Details

### Screenshots
- Captured automatically on test failures
- Manual captures at key points:
  - Debate configuration
  - Turn progression
  - Error states
  - Performance metrics

### Debate Transcripts
Each debate generates a JSON file with:
```json
{
  "debateId": "uuid",
  "topic": "debate topic",
  "participants": [...],
  "turns": [
    {
      "turnNumber": 1,
      "speaker": "Claude Sonnet 4 Advocate",
      "content": "...",
      "timestamp": "2024-07-12T...",
      "contentLength": 523
    }
  ]
}
```

### Performance Reports
- Response time tracking for each LLM
- Page load metrics
- API performance data
- Memory usage snapshots

## Troubleshooting

### Common Issues

1. **LLM Service Timeout**
   - Ensure API keys are configured
   - Check service health: `curl http://localhost:5002/health`
   - Increase timeout in test configuration

2. **WebSocket Connection Failed**
   - Verify debate service is running: `docker-compose ps`
   - Check port 5013 is accessible

3. **Test Evidence Not Saved**
   - Ensure test_probe directory exists
   - Check Docker volume mounting
   - Verify write permissions

### Debug Commands

```bash
# Check service logs
docker-compose logs -f mcp-llm
docker-compose logs -f mcp-debate

# Run single test with debug output
px playwright test comprehensive-debate.spec.ts --debug

# View test report
npm run show-report
```

## Test Results Analysis

After running tests, review:

1. **HTML Report**: `test_probe/evidence/html-report/index.html`
2. **Debate Transcripts**: `test_probe/evidence/debate-transcripts/`
3. **Screenshots**: `test_probe/evidence/screenshots/`
4. **Performance Metrics**: `test_probe/evidence/performance-metrics/`

## Important Notes

1. **No Mocking**: All tests run against real services
2. **LLM Models**: Primary focus on Claude 3.5 Sonnet vs Gemini 2.5 Pro
3. **Evidence Collection**: Comprehensive tracking in test_probe directory
4. **Retry Logic**: Built-in retries for transient failures
5. **Performance**: Extended timeouts for LLM operations

## Next Steps

To ensure 100% test pass rate:
1. Run `make test` to execute all tests
2. Review test_probe directory for any failures
3. Check service logs if tests fail
4. Ensure all API keys are properly configured
5. Verify services are healthy before running tests