// TODO: Refactor to reduce cognitive complexity (SonarCloud S3776)
// Consider breaking down complex functions into smaller, more focused functions

import { test, expect } from '@playwright/test';
import { WorkflowEditorTestHelpers, WorkflowEditorAssertions } from '../../utils/test-helpers';

test.describe('Telemetry Dashboard Reports', () => {
  let helpers: WorkflowEditorTestHelpers;
  let assertions: WorkflowEditorAssertions;

  test.beforeEach(async ({ page }) => {
    helpers = new WorkflowEditorTestHelpers(page);
    assertions = new WorkflowEditorAssertions(page);
    
    await page.goto('/');
    await helpers.waitForPageLoad();
    await helpers.takeScreenshot('dashboard-initial-load');
  });

  test('should display telemetry dashboard with real-time data', async ({ page }) => {
    // Navigate to telemetry dashboard
    await helpers.navigateToSection('telemetry-dashboard');
    await helpers.takeScreenshot('telemetry-dashboard-loaded');

    // Verify dashboard components are present
    await helpers.verifyTelemetryDashboard();
    
    // Check for data visualization
    await helpers.verifyDataVisualization();
    
    // Verify real-time updates (if applicable)
    await helpers.verifyRealTimeUpdates('.telemetry-dashboard');
    
    // Assert dashboard data is loaded
    await assertions.assertDashboardDataLoaded();
    
    // Export evidence
    await helpers.exportTestEvidence('telemetry-dashboard-complete');
  });

  test('should show telemetry metrics and charts', async ({ page }) => {
    await helpers.navigateToSection('telemetry-dashboard');
    
    // Check for specific chart elements
    await expect(page.locator('canvas, svg, .recharts-wrapper')).toBeVisible({ timeout: 10000 });
    
    // Verify chart data is present
    const chartData = page.locator('.recharts-line, .recharts-bar, .recharts-area, circle[r], path[d]');
    await expect(chartData.first()).toBeVisible({ timeout: 15000 });
    
    // Check for metric values
    const metrics = page.locator('[class*="metric"], [class*="value"], .telemetry-value');
    if (await metrics.count() > 0) {
      await expect(metrics.first()).toBeVisible();
    }
    
    await helpers.takeScreenshot('telemetry-metrics-verified');
  });

  test('should handle telemetry dashboard interactions', async ({ page }) => {
    await helpers.navigateToSection('telemetry-dashboard');
    
    // Try clicking on interactive elements
    const interactiveElements = page.locator('button, select, input[type="range"], .clickable');
    const elementCount = await interactiveElements.count();
    
    if (elementCount > 0) {
      // Test first interactive element
      await interactiveElements.first().click();
      await page.waitForTimeout(1000);
      await helpers.takeScreenshot('dashboard-interaction-1');
      
      // Test additional elements if available
      if (elementCount > 1) {
        await interactiveElements.nth(1).click();
        await page.waitForTimeout(1000);
        await helpers.takeScreenshot('dashboard-interaction-2');
      }
    }
    
    await helpers.exportTestEvidence('telemetry-dashboard-interactions');
  });

  test('should display telemetry alerts and thresholds', async ({ page }) => {
    await helpers.navigateToSection('telemetry-dashboard');
    
    // Look for alert indicators
    const alertElements = page.locator('[class*="alert"], [class*="warning"], [class*="threshold"], .telemetry-alert');
    
    if (await alertElements.count() > 0) {
      await expect(alertElements.first()).toBeVisible();
      await helpers.takeScreenshot('telemetry-alerts-present');
    } else {
      console.log('No alert elements found - this may be expected if no thresholds are exceeded');
      await helpers.takeScreenshot('telemetry-alerts-none');
    }
    
    await helpers.exportTestEvidence('telemetry-alerts-check');
  });

  test('should show historical data trends', async ({ page }) => {
    await helpers.navigateToSection('telemetry-dashboard');
    
    // Check for time-series data
    const timeSeriesElements = page.locator('[class*="time"], [class*="historical"], [class*="trend"], .recharts-line');
    
    if (await timeSeriesElements.count() > 0) {
      await expect(timeSeriesElements.first()).toBeVisible({ timeout: 10000 });
      
      // Look for time axis or timestamps
      const timeAxis = page.locator('.recharts-xAxis, .recharts-cartesian-axis, [class*="timestamp"]');
      if (await timeAxis.count() > 0) {
        await expect(timeAxis.first()).toBeVisible();
      }
      
      await helpers.takeScreenshot('historical-data-trends');
    }
    
    await helpers.exportTestEvidence('historical-data-verification');
  });

  test('should be responsive on different screen sizes', async ({ page }) => {
    await helpers.navigateToSection('telemetry-dashboard');
    
    // Test desktop view
    await page.setViewportSize({ width: 1920, height: 1080 });
    await page.waitForTimeout(1000);
    await helpers.takeScreenshot('dashboard-desktop-1920');
    
    // Test tablet view
    await page.setViewportSize({ width: 768, height: 1024 });
    await page.waitForTimeout(1000);
    await helpers.takeScreenshot('dashboard-tablet-768');
    
    // Test mobile view
    await page.setViewportSize({ width: 375, height: 667 });
    await page.waitForTimeout(1000);
    await helpers.takeScreenshot('dashboard-mobile-375');
    
    // Verify dashboard is still functional on mobile
    await helpers.verifyComponentVisible('.telemetry-dashboard, .page-content');
    
    await helpers.exportTestEvidence('dashboard-responsive-test');
  });
});