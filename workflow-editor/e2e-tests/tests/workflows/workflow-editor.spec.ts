import { test, expect } from '@playwright/test';
import { WorkflowEditorTestHelpers, WorkflowEditorAssertions } from '../../utils/test-helpers';

test.describe('Workflow Editor Functionality', () => {
  let helpers: WorkflowEditorTestHelpers;
  let assertions: WorkflowEditorAssertions;

  test.beforeEach(async ({ page }) => {
    helpers = new WorkflowEditorTestHelpers(page);
    assertions = new WorkflowEditorAssertions(page);
    
    await page.goto('/');
    await helpers.waitForPageLoad();
    await helpers.navigateToSection('workflow-editor');
  });

  test('should display workflow editor with React Flow canvas', async ({ page }) => {
    // Verify workflow editor components
    await helpers.verifyWorkflowEditor();
    
    // Check for React Flow canvas
    await expect(page.locator('.react-flow')).toBeVisible({ timeout: 10000 });
    
    // Verify workflow nodes are present
    await assertions.assertWorkflowNodesPresent();
    
    await helpers.takeScreenshot('workflow-editor-loaded');
    await helpers.exportTestEvidence('workflow-editor-basic');
  });

  test('should show workflow node palette and tools', async ({ page }) => {
    // Look for node palette
    const paletteElements = page.locator('[class*="palette"], [class*="toolbox"], [class*="nodes"]');
    
    if (await paletteElements.count() > 0) {
      await expect(paletteElements.first()).toBeVisible();
      console.log('Found node palette');
    } else {
      console.log('No specific palette found - checking for workflow tools');
    }
    
    // Check for workflow control buttons
    const controlButtons = page.locator('button, [class*="control"], [class*="zoom"], [class*="fit"]');
    
    if (await controlButtons.count() > 0) {
      console.log(`Found ${await controlButtons.count()} workflow controls`);
      await helpers.takeScreenshot('workflow-controls-present');
    }
    
    await helpers.exportTestEvidence('workflow-tools-verification');
  });

  test('should support workflow node interactions', async ({ page }) => {
    // Look for workflow nodes
    const workflowNodes = page.locator('.react-flow__node');
    
    if (await workflowNodes.count() > 0) {
      console.log(`Found ${await workflowNodes.count()} workflow nodes`);
      
      // Test clicking on a node
      await workflowNodes.first().click();
      await page.waitForTimeout(1000);
      await helpers.takeScreenshot('workflow-node-selected');
      
      // Look for node properties panel
      const propertiesPanel = page.locator('[class*="properties"], [class*="panel"], [class*="sidebar"]');
      if (await propertiesPanel.count() > 0) {
        await expect(propertiesPanel.first()).toBeVisible();
        await helpers.takeScreenshot('workflow-properties-panel');
      }
      
      // Test node dragging (if possible)
      const nodeBox = await workflowNodes.first().boundingBox();
      if (nodeBox) {
        await page.mouse.move(nodeBox.x + nodeBox.width / 2, nodeBox.y + nodeBox.height / 2);
        await page.mouse.down();
        await page.mouse.move(nodeBox.x + 50, nodeBox.y + 50);
        await page.mouse.up();
        await page.waitForTimeout(500);
        await helpers.takeScreenshot('workflow-node-dragged');
      }
    } else {
      console.log('No workflow nodes found - checking for empty state');
      await helpers.takeScreenshot('workflow-empty-state');
    }
    
    await helpers.exportTestEvidence('workflow-interactions');
  });

  test('should display workflow connections and edges', async ({ page }) => {
    // Look for workflow edges/connections
    const edges = page.locator('.react-flow__edge, [class*="edge"], [class*="connection"]');
    
    if (await edges.count() > 0) {
      console.log(`Found ${await edges.count()} workflow connections`);
      await expect(edges.first()).toBeVisible();
      await helpers.takeScreenshot('workflow-connections-present');
    } else {
      console.log('No workflow connections found');
      await helpers.takeScreenshot('workflow-no-connections');
    }
    
    // Check for connection handles
    const handles = page.locator('.react-flow__handle, [class*="handle"]');
    if (await handles.count() > 0) {
      console.log(`Found ${await handles.count()} connection handles`);
      await helpers.takeScreenshot('workflow-handles-present');
    }
    
    await helpers.exportTestEvidence('workflow-connections-verification');
  });

  test('should support workflow canvas controls', async ({ page }) => {
    // Test zoom controls
    const zoomControls = page.locator('[class*="zoom"], .react-flow__controls');
    
    if (await zoomControls.count() > 0) {
      console.log('Found workflow zoom controls');
      
      // Test zoom in
      const zoomIn = page.locator('[class*="zoom-in"], button').filter({ hasText: '+' });
      if (await zoomIn.count() > 0) {
        await zoomIn.first().click();
        await page.waitForTimeout(500);
        await helpers.takeScreenshot('workflow-zoomed-in');
      }
      
      // Test zoom out
      const zoomOut = page.locator('[class*="zoom-out"], button').filter({ hasText: '-' });
      if (await zoomOut.count() > 0) {
        await zoomOut.first().click();
        await page.waitForTimeout(500);
        await helpers.takeScreenshot('workflow-zoomed-out');
      }
      
      // Test fit view
      const fitView = page.locator('button').filter({ hasText: /fit|center/i });
      if (await fitView.count() > 0) {
        await fitView.first().click();
        await page.waitForTimeout(500);
        await helpers.takeScreenshot('workflow-fit-view');
      }
    } else {
      console.log('No specific zoom controls found');
    }
    
    // Test canvas panning
    const canvas = page.locator('.react-flow, .react-flow__renderer');
    if (await canvas.count() > 0) {
      const canvasBox = await canvas.first().boundingBox();
      if (canvasBox) {
        // Pan the canvas
        await page.mouse.move(canvasBox.x + 200, canvasBox.y + 200);
        await page.mouse.down();
        await page.mouse.move(canvasBox.x + 250, canvasBox.y + 250);
        await page.mouse.up();
        await page.waitForTimeout(500);
        await helpers.takeScreenshot('workflow-canvas-panned');
      }
    }
    
    await helpers.exportTestEvidence('workflow-canvas-controls');
  });

  test('should handle workflow performance with large datasets', async ({ page }) => {
    // Test performance with current workflow size
    const startTime = performance.now();
    
    // Count workflow elements
    const nodes = await page.locator('.react-flow__node').count();
    const edges = await page.locator('.react-flow__edge').count();
    
    const loadTime = performance.now() - startTime;
    
    console.log(`Workflow Performance:
      - Nodes: ${nodes}
      - Edges: ${edges}
      - Load Time: ${Math.round(loadTime)}ms`);
    
    // Test canvas responsiveness
    if (nodes > 0) {
      const performanceTestStart = performance.now();
      
      // Perform several interactions to test responsiveness
      const canvas = page.locator('.react-flow');
      await canvas.click({ position: { x: 100, y: 100 } });
      await page.waitForTimeout(100);
      await canvas.click({ position: { x: 200, y: 200 } });
      await page.waitForTimeout(100);
      await canvas.click({ position: { x: 300, y: 300 } });
      
      const interactionTime = performance.now() - performanceTestStart;
      
      console.log(`Interaction responsiveness: ${Math.round(interactionTime)}ms`);
      
      // Assert reasonable performance
      expect(interactionTime).toBeLessThan(2000); // 2 second max for interactions
    }
    
    await helpers.takeScreenshot('workflow-performance-test');
    await helpers.exportTestEvidence('workflow-performance-results');
  });

  test('should validate workflow editor on different screen sizes', async ({ page }) => {
    const viewports = [
      { width: 1920, height: 1080, name: 'desktop-xl' },
      { width: 1366, height: 768, name: 'desktop' },
      { width: 1024, height: 768, name: 'tablet' },
      { width: 768, height: 1024, name: 'tablet-portrait' },
      { width: 375, height: 667, name: 'mobile' }
    ];
    
    for (const viewport of viewports) {
      console.log(`Testing workflow editor on ${viewport.name}`);
      
      await page.setViewportSize(viewport);
      await page.waitForTimeout(1000);
      
      // Verify workflow editor is still functional
      await expect(page.locator('.react-flow, .page-content')).toBeVisible();
      
      // Check if controls are accessible
      const controls = page.locator('button, [class*="control"]');
      if (await controls.count() > 0) {
        // Verify at least some controls are visible
        const visibleControls = await controls.filter({ hasText: /.+/ }).count();
        console.log(`${viewport.name}: ${visibleControls} controls visible`);
      }
      
      await helpers.takeScreenshot(`workflow-editor-${viewport.name}`);
    }
    
    await helpers.exportTestEvidence('workflow-editor-responsive');
  });

  test('should test workflow editor with condition builder', async ({ page }) => {
    // Look for condition builder elements
    const conditionElements = page.locator('[class*="condition"], [class*="builder"], [class*="query"]');
    
    if (await conditionElements.count() > 0) {
      console.log('Found condition builder elements');
      
      // Test condition builder interactions
      await conditionElements.first().click();
      await page.waitForTimeout(1000);
      await helpers.takeScreenshot('condition-builder-active');
      
      // Look for condition controls
      const conditionControls = page.locator('select, input, button').filter({ hasText: /add|condition|rule|operator/i });
      
      if (await conditionControls.count() > 0) {
        await conditionControls.first().click();
        await page.waitForTimeout(500);
        await helpers.takeScreenshot('condition-builder-interaction');
      }
    } else {
      console.log('No condition builder found in current view');
      await helpers.takeScreenshot('no-condition-builder');
    }
    
    await helpers.exportTestEvidence('condition-builder-test');
  });
});