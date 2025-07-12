# Playwright E2E Tests for AI Debate System

This project contains comprehensive end-to-end tests for the AI Debate System using Playwright.

## 📁 Project Structure

```
playwright-tests/
├── tests/                  # Test specifications
│   ├── smoke.spec.ts      # Basic functionality tests
│   ├── organization.spec.ts # Organization management tests
│   ├── template.spec.ts   # Template management tests
│   ├── debate.spec.ts     # Debate functionality tests
│   └── integration.spec.ts # End-to-end integration tests
├── pages/                  # Page Object Models
│   ├── organization.page.ts
│   ├── template.page.ts
│   └── debate.page.ts
├── utils/                  # Helper utilities
│   └── helpers.ts         # Common helper functions
├── global-setup.ts        # Global setup before all tests
├── global-teardown.ts     # Global cleanup after all tests
├── playwright.config.ts   # Playwright configuration
├── package.json           # Project dependencies
└── README.md             # This file
```

## 🚀 Getting Started

### Prerequisites

- Node.js 18 or higher
- The AI Debate System should be running (all services)

### Installation

```bash
cd playwright-tests
npm install
```

### Running Tests

```bash
# Run all tests
npm test

# Run tests in headed mode (see browser)
npm run test:headed

# Run tests in UI mode (interactive)
npm run test:ui

# Run specific test file
npm test tests/smoke.spec.ts

# Run tests with specific tag
npm test --grep @smoke

# Generate HTML report
npm run test:report
```

## 🧪 Test Categories

### Smoke Tests (`smoke.spec.ts`)
- Basic page loading
- Navigation between tabs
- Core UI elements visibility
- Service health checks

### Organization Tests (`organization.spec.ts`)
- Create new organizations
- Switch between organizations
- View organization history
- Organization persistence
- Special character handling

### Template Tests (`template.spec.ts`)
- Create, edit, delete templates
- Template search and filtering
- Template preview
- Complex Jinja2 template support
- Template validation

### Debate Tests (`debate.spec.ts`)
- Create debates with/without templates
- Use templates from gallery
- Search and filter debates
- Multiple participants support
- Quick actions functionality

### Integration Tests (`integration.spec.ts`)
- Complete workflows across features
- Data persistence
- Concurrent operations
- Performance testing
- Mobile responsiveness

## 📝 Writing New Tests

### Test Structure

```typescript
import { test, expect } from '@playwright/test';
import { SomePage } from '../pages/some.page';

test.describe('Feature Name', () => {
  let somePage: SomePage;

  test.beforeEach(async ({ page }) => {
    somePage = new SomePage(page);
    await somePage.goto();
  });

  test('should do something', async ({ page }) => {
    // Arrange
    const testData = { /* ... */ };
    
    // Act
    await somePage.performAction(testData);
    
    // Assert
    await expect(page.getByText('Expected Result')).toBeVisible();
  });
});
```

### Page Object Pattern

Create page objects in `pages/` directory:

```typescript
export class NewFeaturePage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/feature');
  }

  async performAction(data: any) {
    // Implement page interactions
  }
}
```

### Helper Functions

Add reusable functions to `utils/helpers.ts`:

```typescript
export function generateTestData() {
  return {
    id: randomString(),
    // ... more fields
  };
}
```

## 🔧 Configuration

### Environment Variables

Create a `.env` file for test-specific configuration:

```env
BASE_URL=http://localhost:3000
TEST_TIMEOUT=30000
```

### Playwright Config

Modify `playwright.config.ts` for:
- Browsers to test
- Test timeout
- Retry logic
- Reporter configuration
- Screenshot/video settings

## 🐛 Debugging

### Debug Single Test
```bash
npm test -- --debug tests/specific.spec.ts
```

### View Test Report
```bash
npm run show-report
```

### Take Screenshots on Failure
Tests are configured to automatically capture screenshots on failure.

### Trace Viewer
```bash
# Run with trace
npm test -- --trace on

# View trace
npx playwright show-trace trace.zip
```

## 📊 CI/CD Integration

### GitHub Actions Example

```yaml
name: Playwright Tests
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
      - run: npm ci
      - run: npx playwright install --with-deps
      - run: npm test
      - uses: actions/upload-artifact@v3
        if: always()
        with:
          name: playwright-report
          path: playwright-report/
```

## 🎯 Best Practices

1. **Use Page Objects**: Keep test logic separate from page interactions
2. **Data Independence**: Generate unique test data for each test
3. **Explicit Waits**: Use Playwright's built-in waiting mechanisms
4. **Meaningful Assertions**: Write clear, specific assertions
5. **Test Isolation**: Each test should be independent
6. **Parallel Execution**: Tests should support parallel execution
7. **Cleanup**: Clean up test data in teardown hooks

## 🚨 Common Issues

### Services Not Ready
- Ensure all services are running before tests
- Check `global-setup.ts` service health checks

### Flaky Tests
- Add explicit waits for dynamic content
- Use `waitForLoadState('networkidle')`
- Increase timeout for slow operations

### Element Not Found
- Check selectors are unique and stable
- Use data-testid attributes for critical elements
- Verify element is visible before interaction

## 📚 Resources

- [Playwright Documentation](https://playwright.dev)
- [Best Practices](https://playwright.dev/docs/best-practices)
- [Debugging Guide](https://playwright.dev/docs/debug)
- [CI/CD Guide](https://playwright.dev/docs/ci)