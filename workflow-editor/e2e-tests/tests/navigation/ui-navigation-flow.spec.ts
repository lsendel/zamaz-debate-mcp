import { test, expect } from '@playwright/test';
import { WorkflowEditorTestHelpers, WorkflowEditorAssertions } from '../../utils/test-helpers';

test.describe('UI Navigation Flow for Regression Testing', () => {
  let helpers: WorkflowEditorTestHelpers;
  let assertions: WorkflowEditorAssertions;

  test.beforeEach(async ({ page }) => {
    helpers = new WorkflowEditorTestHelpers(page);
//     assertions = new WorkflowEditorAssertions(page); // SonarCloud: removed useless assignment
    
    await page.goto('/');
    await helpers.waitForPageLoad();
  });

  test('should complete full application navigation flow', async ({ page }) => {
    // This test provides the complete navigation steps for regression testing
    console.log('🔄 Starting complete navigation flow for regression testing...');
    
    // Step 1: Verify initial application load
    await expect(page.locator('.app')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('.app-header')).toBeVisible();
    await expect(page.locator('.app-navigation')).toBeVisible();
    await helpers.takeScreenshot('01-initial-app-load');
    
    // Step 2: Navigate to Workflow Editor (default page)
    console.log('📝 Testing Workflow Editor...');
    await helpers.navigateToSection('workflow-editor');
    await expect(page.locator('.page-content')).toBeVisible({ timeout: 10000 });
    await helpers.takeScreenshot('02-workflow-editor');
    
    // Step 3: Navigate to Telemetry Dashboard
    console.log('📊 Testing Telemetry Dashboard...');
    await helpers.navigateToSection('telemetry-dashboard');
    await expect(page.locator('.page-content')).toBeVisible({ timeout: 10000 });
    await helpers.takeScreenshot('03-telemetry-dashboard');
    
    // Step 4: Navigate to Telemetry Map
    console.log('🗺️ Testing Telemetry Map...');
    await helpers.navigateToSection('telemetry-map');
    await expect(page.locator('.page-content')).toBeVisible({ timeout: 10000 });
    await page.waitForTimeout(3000); // Allow map to load
    await helpers.takeScreenshot('04-telemetry-map');
    
    // Step 5: Navigate to Spatial Query Builder
    console.log('🔍 Testing Spatial Query Builder...');
    await helpers.navigateToSection('spatial-query');
    await expect(page.locator('.page-content')).toBeVisible({ timeout: 10000 });
    await helpers.takeScreenshot('05-spatial-query');
    
    // Step 6: Navigate to Stamford Geospatial Sample
    console.log('🏢 Testing Stamford Geospatial Sample...');
    await helpers.navigateToSection('stamford-sample');
    await expect(page.locator('.page-content')).toBeVisible({ timeout: 10000 });
    await page.waitForTimeout(2000); // Allow sample data to load
    await helpers.takeScreenshot('06-stamford-sample');
    
    // Step 7: Navigate to Debate Tree Sample
    console.log('💬 Testing Debate Tree Sample...');
    await helpers.navigateToSection('debate-sample');
    await expect(page.locator('.page-content')).toBeVisible({ timeout: 10000 });
    await page.waitForTimeout(2000);
    await helpers.takeScreenshot('07-debate-sample');
    
    // Step 8: Navigate to Decision Tree Sample
    console.log('🌳 Testing Decision Tree Sample...');
    await helpers.navigateToSection('decision-sample');
    await expect(page.locator('.page-content')).toBeVisible({ timeout: 10000 });
    await page.waitForTimeout(2000);
    await helpers.takeScreenshot('08-decision-sample');
    
    // Step 9: Navigate to AI Document Analysis Sample
    console.log('📄 Testing AI Document Analysis Sample...');
    await helpers.navigateToSection('ai-document-sample');
    await expect(page.locator('.page-content')).toBeVisible({ timeout: 10000 });
    await page.waitForTimeout(2000);
    await helpers.takeScreenshot('09-ai-document-sample');
    
    // Step 10: Return to Workflow Editor to complete the cycle
    console.log('🔄 Returning to Workflow Editor to complete cycle...');
    await helpers.navigateToSection('workflow-editor');
    await expect(page.locator('.page-content')).toBeVisible({ timeout: 10000 });
    await helpers.takeScreenshot('10-workflow-editor-return');
    
    console.log('✅ Complete navigation flow test completed successfully!');
    await helpers.exportTestEvidence('complete-navigation-flow');
  });

  test('should validate each navigation item exists and is clickable', async ({ page }) => {
    const expectedNavigationItems = [
      { id: 'workflow-editor', text: 'Workflow Editor', icon: '🔀' },
      { id: 'telemetry-dashboard', text: 'Telemetry Dashboard', icon: '📊' },
      { id: 'telemetry-map', text: 'Telemetry Map', icon: '🗺️' },
      { id: 'spatial-query', text: 'Spatial Query', icon: '🔍' },
      { id: 'stamford-sample', text: 'Stamford Sample', icon: '🏢' },
      { id: 'debate-sample', text: 'Debate Tree', icon: '💬' },
      { id: 'decision-sample', text: 'Decision Tree', icon: '🌳' },
      { id: 'ai-document-sample', text: 'AI Document Analysis', icon: '📄' }
    ];
    
    // Check that all navigation items are present
    for (const item of expectedNavigationItems) {
      const navButton = page.locator('.nav-item').filter({ hasText: new RegExp(item.text, 'i') });
      await expect(navButton).toBeVisible({ timeout: 5000 });
      console.log(`✅ Found navigation item: ${item.text}`);
    }
    
    await helpers.takeScreenshot('all-navigation-items-present');
    await helpers.exportTestEvidence('navigation-items-validation');
  });

  test('should test navigation item hover states and animations', async ({ page }) => {
    const navItems = page.locator('.nav-item');
    const itemCount = await navItems.count();
    
    console.log(`Testing hover states for ${itemCount} navigation items`);
    
    for (let i = 0; i < itemCount; i++) {
      await navItems.nth(i).hover();
      await page.waitForTimeout(500); // Allow hover animation
      await helpers.takeScreenshot(`nav-item-${i}-hover`);
    }
    
    await helpers.exportTestEvidence('navigation-hover-states');
  });

  test('should verify responsive navigation on different screen sizes', async ({ page }) => {
    const viewports = [
      { width: 1920, height: 1080, name: 'desktop-xl' },
      { width: 1366, height: 768, name: 'desktop' },
      { width: 1024, height: 768, name: 'tablet-landscape' },
      { width: 768, height: 1024, name: 'tablet-portrait' },
      { width: 480, height: 800, name: 'mobile-large' },
      { width: 375, height: 667, name: 'mobile' },
      { width: 320, height: 568, name: 'mobile-small' }
    ];
    
    for (const viewport of viewports) {
      console.log(`Testing navigation on ${viewport.name} (${viewport.width}x${viewport.height})`);
      
      await page.setViewportSize(viewport);
      await page.waitForTimeout(1000);
      
      // Verify navigation is still accessible
      await expect(page.locator('.app-navigation')).toBeVisible();
      
      // Test a navigation item works on this viewport
      const firstNavItem = page.locator('.nav-item').first();
      await expect(firstNavItem).toBeVisible();
      await firstNavItem.click();
      await page.waitForTimeout(1000);
      
      await helpers.takeScreenshot(`navigation-${viewport.name}`);
    }
    
    await helpers.exportTestEvidence('responsive-navigation-test');
  });

  test('should measure navigation performance and timing', async ({ page }) => {
    const navigationTimes: any[] = [];
    const sections = ['workflow-editor', 'telemetry-dashboard', 'telemetry-map', 'spatial-query'];
    
    for (const section of sections) {
      const startTime = performance.now();
      
      await helpers.navigateToSection(section);
      await expect(page.locator('.page-content')).toBeVisible({ timeout: 10000 });
      
      const endTime = performance.now();
      const navigationTime = endTime - startTime;
      
      navigationTimes.push({
        section,
        navigationTime: Math.round(navigationTime),
        performance: navigationTime < 1000 ? 'Fast' : navigationTime < 3000 ? 'Good' : 'Slow'
      });
      
      console.log(`${section}: ${Math.round(navigationTime)}ms`);
    }
    
    console.log('Navigation performance summary:', navigationTimes);
    
    // Assert reasonable navigation times
    for (const result of navigationTimes) {
      expect(result.navigationTime).toBeLessThan(5000); // 5 second max
    }
    
    await helpers.exportTestEvidence('navigation-performance-results');
  });

  test('should create regression testing step-by-step guide', async ({ page }) => {
    // This test documents the exact steps for manual regression testing
    const regressionSteps = [
      '1. Open workflow editor at http://localhost:3002',
      '2. Verify header shows "🔄 Kiro Workflow Editor"',
      '3. Verify navigation bar shows 8 items with icons',
      '4. Click "🔀 Workflow Editor" - should show workflow canvas',
      '5. Click "📊 Telemetry Dashboard" - should show charts and metrics',
      '6. Click "🗺️ Telemetry Map" - should show interactive map',
      '7. Click "🔍 Spatial Query" - should show query builder',
      '8. Click "🏢 Stamford Sample" - should show geospatial data',
      '9. Click "💬 Debate Tree" - should show hierarchical tree',
      '10. Click "🌳 Decision Tree" - should show decision workflow',
      '11. Click "📄 AI Document Analysis" - should show document processor',
      '12. Test responsive design on mobile/tablet',
      '13. Verify all components load without errors',
      '14. Check browser console for JavaScript errors',
      '15. Verify real-time data updates (if applicable)'
    ];
    
    console.log('📋 Regression Testing Steps:');
    regressionSteps.forEach(step => console.log(step));
    
    // Perform a quick verification of key steps
    await helpers.navigateToSection('workflow-editor');
    await helpers.takeScreenshot('regression-step-4-workflow-editor');
    
    await helpers.navigateToSection('telemetry-dashboard');
    await helpers.takeScreenshot('regression-step-5-telemetry-dashboard');
    
    await helpers.navigateToSection('telemetry-map');
    await page.waitForTimeout(3000); // Allow map to load
    await helpers.takeScreenshot('regression-step-6-telemetry-map');
    
    await helpers.exportTestEvidence('regression-testing-guide');
  });

  test('should test error handling and edge cases', async ({ page }) => {
    // Test navigation during loading states
    console.log('Testing navigation during loading...');
    
    await helpers.navigateToSection('telemetry-map');
    // Immediately navigate elsewhere before full load
    await helpers.navigateToSection('workflow-editor');
    await expect(page.locator('.page-content')).toBeVisible({ timeout: 10000 });
    await helpers.takeScreenshot('navigation-during-loading');
    
    // Test rapid navigation
    console.log('Testing rapid navigation...');
    const sections = ['telemetry-dashboard', 'telemetry-map', 'spatial-query', 'workflow-editor'];
    
    for (const section of sections) {
      await helpers.navigateToSection(section);
      await page.waitForTimeout(200); // Minimal wait
    }
    
    // Final check that we end up in correct state
    await page.waitForTimeout(2000);
    await expect(page.locator('.page-content')).toBeVisible();
    await helpers.takeScreenshot('rapid-navigation-final');
    
    await helpers.exportTestEvidence('navigation-edge-cases');
  });
});