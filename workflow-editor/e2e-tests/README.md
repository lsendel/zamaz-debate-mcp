# Workflow Editor E2E Tests

Comprehensive end-to-end testing suite for the Kiro Workflow Editor using Playwright.

## 🚀 Quick Start

### Prerequisites
- Node.js 16+
- Workflow Editor running on http://localhost:3002

### Installation & Setup
```bash
# From the project root
make setup-workflow-e2e

# Or manually:
cd workflow-editor/e2e-tests
npm install
npx playwright install
```

### Running Tests

#### Basic Test Execution
```bash
# Run all tests
npm test

# Run tests with browser visible
npm run test:headed

# Run tests with Playwright UI
npm run test:ui

# Run only report tests
npm run test:reports

# Run only dashboard tests  
npm run test:dashboards
```

#### From Project Root (using Makefile)
```bash
# Run workflow editor E2E tests
make workflow-test

# Setup and run tests
make setup-workflow-e2e && make workflow-test
```

## 📁 Test Structure

```
e2e-tests/
├── tests/
│   ├── reports/                    # Report and dashboard tests
│   │   ├── telemetry-dashboard.spec.ts
│   │   ├── telemetry-map.spec.ts
│   │   └── sample-applications.spec.ts
│   ├── navigation/                 # UI navigation tests
│   │   └── ui-navigation-flow.spec.ts
│   └── workflows/                  # Workflow editor tests
│       └── workflow-editor.spec.ts
├── utils/
│   └── test-helpers.ts            # Reusable test utilities
├── fixtures/                      # Test data and fixtures
├── test-results/                  # Test outputs and screenshots
└── playwright.config.ts           # Playwright configuration
```

## 🧪 Test Categories

### Report Tests (`tests/reports/`)
- **Telemetry Dashboard**: Validates real-time data visualization, charts, metrics
- **Telemetry Map**: Tests spatial data visualization, map interactions, markers
- **Sample Applications**: Verifies all 4 sample apps with real data integration

### Navigation Tests (`tests/navigation/`)
- **UI Navigation Flow**: Complete navigation regression testing
- **Responsive Design**: Cross-device compatibility testing
- **Performance**: Navigation timing and responsiveness

### Workflow Tests (`tests/workflows/`)
- **Workflow Editor**: React Flow canvas, node interactions, performance
- **Condition Builder**: Query building and logic testing

## 📊 Evidence Generation

Tests automatically generate comprehensive evidence:

### Screenshots
- `test-results/screenshots/` - Individual test screenshots
- `test-results/evidence/` - Complete test evidence packages

### Reports
- `test-results/html-report/` - HTML test report
- `test-results/results.json` - JSON test results
- `test-results/results.xml` - JUnit XML results

### Video & Traces
- Videos of test failures
- Playwright traces for debugging

## 🔄 Regression Testing Guide

### Complete UI Navigation Steps

1. **Application Load**
   ```bash
   npm run test:ui -- --grep "complete full application navigation flow"
   ```

2. **Step-by-Step Manual Testing**
   - Open http://localhost:3002
   - Verify header shows "🔄 Kiro Workflow Editor"
   - Test each navigation item:
     - 🔀 Workflow Editor → workflow canvas
     - 📊 Telemetry Dashboard → charts and metrics
     - 🗺️ Telemetry Map → interactive map
     - 🔍 Spatial Query → query builder
     - 🏢 Stamford Sample → geospatial data
     - 💬 Debate Tree → hierarchical tree
     - 🌳 Decision Tree → decision workflow
     - 📄 AI Document Analysis → document processor

3. **Responsive Testing**
   - Test on desktop (1920x1080)
   - Test on tablet (768x1024)
   - Test on mobile (375x667)

### Performance Validation
```bash
# Test navigation performance
npm run test:ui -- --grep "navigation performance"

# Test component load times
npm run test:ui -- --grep "performance"
```

## 🛠️ Test Configuration

### Environment Variables
```bash
# Test target URL
PLAYWRIGHT_BASE_URL=http://localhost:3002

# Test timeouts
PLAYWRIGHT_TIMEOUT=30000
PLAYWRIGHT_ACTION_TIMEOUT=10000

# Test execution
PLAYWRIGHT_WORKERS=4
PLAYWRIGHT_RETRIES=2
```

### Browsers Tested
- Chromium (Desktop & Mobile)
- Firefox
- WebKit (Safari)
- Microsoft Edge
- Google Chrome

## 📝 Writing New Tests

### Basic Test Structure
```typescript
import { test, expect } from '@playwright/test';
import { WorkflowEditorTestHelpers } from '../utils/test-helpers';

test.describe('My Test Suite', () => {
  let helpers: WorkflowEditorTestHelpers;

  test.beforeEach(async ({ page }) => {
    helpers = new WorkflowEditorTestHelpers(page);
    await page.goto('/');
    await helpers.waitForPageLoad();
  });

  test('should test my feature', async ({ page }) => {
    await helpers.navigateToSection('workflow-editor');
    await helpers.verifyComponentVisible('.my-component');
    await helpers.takeScreenshot('my-test');
    await helpers.exportTestEvidence('my-test-complete');
  });
});
```

### Helper Functions Available
- `navigateToSection(sectionId)` - Navigate to app sections
- `verifyComponentVisible(selector)` - Check component visibility
- `takeScreenshot(name)` - Capture screenshots
- `exportTestEvidence(testName)` - Generate evidence package
- `verifyTelemetryDashboard()` - Validate dashboard components
- `verifyTelemetryMap()` - Validate map components
- `verifyWorkflowEditor()` - Validate workflow editor

## 🚨 Troubleshooting

### Common Issues

#### Application Not Running
```bash
Error: Navigation timeout exceeded
```
**Solution**: Start the workflow editor first
```bash
make workflow-ui  # or cd workflow-editor/client/workflow-editor && npm start
```

#### Browser Installation
```bash
Error: Executable doesn't exist
```
**Solution**: Install Playwright browsers
```bash
npx playwright install
```

#### Port Conflicts
```bash
Error: EADDRINUSE
```
**Solution**: Check if port 3002 is available
```bash
lsof -i :3002
# Kill process if needed
```

### Debugging Tests

#### Run Single Test with Debug
```bash
npx playwright test --debug tests/reports/telemetry-dashboard.spec.ts
```

#### View Test Results
```bash
npm run test:report
# Opens HTML report in browser
```

#### Trace Analysis
```bash
npx playwright show-trace test-results/trace.zip
```

## 📈 Test Metrics

### Coverage Areas
- ✅ UI Navigation (8 sections)
- ✅ Report Functionality (3 report types)
- ✅ Sample Applications (4 samples)
- ✅ Responsive Design (5 viewports)
- ✅ Performance Testing
- ✅ Error Handling

### Success Criteria
- All navigation items functional
- Real-time data visualization
- Map interactions working
- Sample applications load with data
- Responsive across devices
- Performance under 5s per navigation

## 🤝 Contributing

### Adding New Tests
1. Create test file in appropriate directory
2. Use helper functions for common operations
3. Add evidence generation
4. Update this README if needed

### Test Naming Convention
- Descriptive test names
- Group related tests in describe blocks
- Use consistent screenshot naming
- Export evidence for important tests

## 📞 Support

For questions about the E2E testing framework:
1. Check test output and screenshots
2. Review HTML test report
3. Check Playwright documentation
4. Run tests in debug mode for troubleshooting