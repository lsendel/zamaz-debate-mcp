# Test Evidence Collection Structure - Industry Standard

## Directory Structure

```
test-evidence/
├── test-runs/
│   └── 2025-01-18-run-001/
│       ├── summary.json
│       ├── test-results/
│       │   ├── junit.xml
│       │   ├── test-report.html
│       │   └── raw-results.json
│       ├── screenshots/
│       │   ├── 001-login-page.png
│       │   ├── 002-debate-creation-form.png
│       │   ├── 003-participant-added.png
│       │   └── ...
│       ├── videos/
│       │   ├── debate-creation-flow.webm
│       │   ├── real-time-interaction.webm
│       │   └── ...
│       ├── logs/
│       │   ├── browser-console.log
│       │   ├── network-har.json
│       │   ├── websocket-messages.json
│       │   └── api-requests.json
│       ├── performance/
│       │   ├── lighthouse-report.html
│       │   ├── response-times.csv
│       │   └── resource-usage.json
│       └── artifacts/
│           ├── exported-debate.json
│           ├── exported-debate.pdf
│           └── database-snapshots/
│
├── baseline/
│   ├── screenshots/
│   └── performance-benchmarks.json
│
├── reports/
│   ├── executive-summary.pdf
│   ├── detailed-test-report.html
│   └── trend-analysis.xlsx
│
└── README.md
```

## Evidence Collection Standards

### 1. Test Run Identification
- Unique timestamp-based run ID
- Git commit hash
- Environment details
- Test configuration

### 2. Screenshots
- Sequential numbering
- Descriptive names
- Full-page captures
- Before/after states
- Error conditions

### 3. Videos
- Complete user flows
- 30fps minimum
- Include mouse movements
- Audio narration (optional)

### 4. Logs
- Browser console logs
- Network HAR files
- WebSocket message logs
- API request/response pairs
- Performance metrics

### 5. Test Results
- JUnit XML (CI/CD integration)
- HTML reports (human readable)
- JSON (programmatic access)
- Coverage reports

### 6. Performance Data
- Page load times
- API response times
- WebSocket latency
- Resource usage
- Lighthouse scores

### 7. Artifacts
- Database state snapshots
- Exported files
- Generated content
- Configuration files

## Reporting Standards

### Executive Summary
- Pass/fail statistics
- Critical issues found
- Performance highlights
- Recommendations

### Detailed Report
- Test case descriptions
- Step-by-step results
- Screenshots/videos
- Error analysis
- Root cause analysis

### Trend Analysis
- Test stability over time
- Performance trends
- Regression detection
- Coverage improvements

## Best Practices

1. **Traceability**: Link evidence to test cases and requirements
2. **Reproducibility**: Include all data needed to reproduce issues
3. **Accessibility**: Use standard formats (HTML, PDF, JSON)
4. **Retention**: Keep evidence for at least 6 months
5. **Security**: Sanitize sensitive data before storage
6. **Automation**: Automate evidence collection where possible
7. **Review**: Regular evidence review meetings