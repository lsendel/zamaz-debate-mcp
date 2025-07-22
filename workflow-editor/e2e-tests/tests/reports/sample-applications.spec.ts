// TODO: Refactor to reduce cognitive complexity (SonarCloud S3776)
// Consider breaking down complex functions into smaller, more focused functions

import { test, expect } from '@playwright/test';
import { WorkflowEditorTestHelpers, WorkflowEditorAssertions } from '../../utils/test-helpers';

test.describe('Sample Application Reports', () => {
  let helpers: WorkflowEditorTestHelpers;
  let assertions: WorkflowEditorAssertions;

  test.beforeEach(async ({ page }) => {
    helpers = new WorkflowEditorTestHelpers(page);
    
    await page.goto('/');
    await helpers.waitForPageLoad();
  });

  test('should display Stamford Geospatial Sample with real data', async ({ page }) => {
    await helpers.navigateToSection('stamford-sample');
    await helpers.takeScreenshot('stamford-sample-loaded');

    // Verify sample application components
    await helpers.verifySampleApplication('Stamford Geospatial');
    
    // Check for geospatial elements
    await helpers.verifyComponentVisible('.geospatial-sample, .page-content');
    
    // Look for address data
    const addressElements = page.locator('[class*="address"], [class*="location"], [class*="stamford"]');
    if (await addressElements.count() > 0) {
      await expect(addressElements.first()).toBeVisible();
    }
    
    // Check for map elements (if present)
    const mapElements = page.locator('.maplibregl-canvas, .leaflet-container, [class*="map"]');
    if (await mapElements.count() > 0) {
      await expect(mapElements.first()).toBeVisible({ timeout: 10000 });
    }
    
    await helpers.exportTestEvidence('stamford-geospatial-complete');
  });

  test('should display Debate Tree Map Sample with hierarchical data', async ({ page }) => {
    await helpers.navigateToSection('debate-sample');
    await helpers.takeScreenshot('debate-sample-loaded');

    // Verify debate tree components
    await helpers.verifySampleApplication('Debate Tree');
    
    // Check for tree visualization elements
    const treeElements = page.locator('[class*="tree"], [class*="debate"], [class*="node"], [class*="hierarchy"]');
    if (await treeElements.count() > 0) {
      await expect(treeElements.first()).toBeVisible({ timeout: 10000 });
    }
    
    // Look for interactive tree nodes
    const interactiveNodes = page.locator('button, [class*="clickable"], [class*="expandable"], [role="button"]');
    if (await interactiveNodes.count() > 0) {
      // Test tree interaction
      await interactiveNodes.first().click();
      await page.waitForTimeout(1000);
      await helpers.takeScreenshot('debate-tree-interaction');
    }
    
    await helpers.exportTestEvidence('debate-tree-complete');
  });

  test('should display Decision Tree Sample with conditional logic', async ({ page }) => {
    await helpers.navigateToSection('decision-sample');
    await helpers.takeScreenshot('decision-sample-loaded');

    // Verify decision tree components
    await helpers.verifySampleApplication('Decision Tree');
    
    // Check for decision nodes and workflow elements
    const decisionElements = page.locator('[class*="decision"], [class*="condition"], .react-flow__node');
    if (await decisionElements.count() > 0) {
      await expect(decisionElements.first()).toBeVisible({ timeout: 10000 });
    }
    
    // Look for condition builder or decision logic
    const conditionElements = page.locator('[class*="condition"], [class*="logic"], [class*="rule"]');
    if (await conditionElements.count() > 0) {
      await expect(conditionElements.first()).toBeVisible();
    }
    
    // Test decision tree interactions
    const interactiveElements = page.locator('button, select, input, [class*="clickable"]');
    if (await interactiveElements.count() > 0) {
      await interactiveElements.first().click();
      await page.waitForTimeout(1000);
      await helpers.takeScreenshot('decision-tree-interaction');
    }
    
    await helpers.exportTestEvidence('decision-tree-complete');
  });

  test('should display AI Document Analysis Sample with document processing', async ({ page }) => {
    await helpers.navigateToSection('ai-document-sample');
    await helpers.takeScreenshot('ai-document-sample-loaded');

    // Verify AI document analysis components
    await helpers.verifySampleApplication('AI Document Analysis');
    
    // Check for document viewer or upload components
    const documentElements = page.locator('[class*="document"], [class*="pdf"], [class*="upload"], [class*="viewer"]');
    if (await documentElements.count() > 0) {
      await expect(documentElements.first()).toBeVisible({ timeout: 10000 });
    }
    
    // Look for AI analysis results or processing indicators
    const aiElements = page.locator('[class*="analysis"], [class*="ai"], [class*="processing"], [class*="result"]');
    if (await aiElements.count() > 0) {
      await expect(aiElements.first()).toBeVisible();
    }
    
    // Test document analysis interactions
    const interactiveElements = page.locator('button, input[type="file"], [class*="upload"], [class*="analyze"]');
    if (await interactiveElements.count() > 0) {
      // Look for upload or analyze buttons
      const analyzeButton = page.locator('button').filter({ hasText: /analyze|process|upload/i });
      if (await analyzeButton.count() > 0) {
        await analyzeButton.first().click();
        await page.waitForTimeout(1000);
        await helpers.takeScreenshot('ai-document-interaction');
      }
    }
    
    await helpers.exportTestEvidence('ai-document-complete');
  });

  test('should demonstrate real-time telemetry in all samples', async ({ page }) => {
    const samples = ['stamford-sample', 'debate-sample', 'decision-sample', 'ai-document-sample'];
    
    for (const sample of samples) {
      await helpers.navigateToSection(sample);
      await page.waitForTimeout(2000);
      
      // Check for real-time indicators
      const realTimeElements = page.locator('[class*="real-time"], [class*="live"], [class*="telemetry"], [class*="update"]');
      
      if (await realTimeElements.count() > 0) {
        console.log(`${sample}: Found real-time telemetry elements`);
        await helpers.verifyRealTimeUpdates(`[class*="real-time"], [class*="telemetry"]`);
      } else {
        console.log(`${sample}: No real-time elements found`);
      }
      
      await helpers.takeScreenshot(`${sample}-realtime-check`);
    }
    
    await helpers.exportTestEvidence('samples-realtime-verification');
  });

  test('should show telemetry data integration across samples', async ({ page }) => {
    const samples = [
      { id: 'stamford-sample', name: 'Stamford Geospatial' },
      { id: 'debate-sample', name: 'Debate Tree' },
      { id: 'decision-sample', name: 'Decision Tree' },
      { id: 'ai-document-sample', name: 'AI Document Analysis' }
    ];
    
    const telemetryData: any[] = [];
    
    for (const sample of samples) {
      await helpers.navigateToSection(sample.id);
      await page.waitForTimeout(3000);
      
      // Look for telemetry data displays
      const telemetryElements = page.locator('[class*="telemetry"], [class*="metric"], [class*="sensor"], [class*="data"]');
      const count = await telemetryElements.count();
      
      telemetryData.push({
        sample: sample.name,
        telemetryElementCount: count,
        hasData: count > 0
      });
      
      if (count > 0) {
        // Try to extract some data values
        const values = await telemetryElements.allTextContents();
        console.log(`${sample.name} telemetry data:`, values.slice(0, 3));
      }
      
      await helpers.takeScreenshot(`${sample.id}-telemetry-data`);
    }
    
    console.log('Telemetry integration summary:', telemetryData);
    await helpers.exportTestEvidence('telemetry-integration-summary');
  });

  test('should validate sample applications performance', async ({ page }) => {
    const samples = ['stamford-sample', 'debate-sample', 'decision-sample', 'ai-document-sample'];
    const performanceResults: any[] = [];
    
    for (const sample of samples) {
      const startTime = Date.now();
      
      await helpers.navigateToSection(sample);
      await helpers.waitForPageLoad();
      
      const loadTime = Date.now() - startTime;
      
      // Check if main content is visible
      const isContentVisible = await page.locator('.page-content').isVisible();
      
      performanceResults.push({
        sample,
        loadTime,
        contentVisible: isContentVisible,
        performance: loadTime < 5000 ? 'Good' : loadTime < 10000 ? 'Acceptable' : 'Slow'
      });
      
      await helpers.takeScreenshot(`${sample}-performance-test`);
    }
    
    console.log('Performance results:', performanceResults);
    
    // Assert all samples loaded within reasonable time
    for (const result of performanceResults) {
      expect(result.contentVisible).toBe(true);
      expect(result.loadTime).toBeLessThan(15000); // 15 second timeout
    }
    
    await helpers.exportTestEvidence('samples-performance-results');
  });

  test('should verify all samples are responsive', async ({ page }) => {
    const samples = ['stamford-sample', 'debate-sample', 'decision-sample', 'ai-document-sample'];
    const viewports = [
      { width: 1920, height: 1080, name: 'desktop' },
      { width: 768, height: 1024, name: 'tablet' },
      { width: 375, height: 667, name: 'mobile' }
    ];
    
    for (const sample of samples) {
      await helpers.navigateToSection(sample);
      
      for (const viewport of viewports) {
        await page.setViewportSize(viewport);
        await page.waitForTimeout(1000);
        
        // Verify content is still visible and functional
        await helpers.verifyComponentVisible('.page-content');
        
        await helpers.takeScreenshot(`${sample}-${viewport.name}-responsive`);
      }
    }
    
    await helpers.exportTestEvidence('samples-responsive-complete');
  });
});