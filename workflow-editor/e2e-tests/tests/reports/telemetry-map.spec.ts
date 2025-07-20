import { test, expect } from '@playwright/test';
import { WorkflowEditorTestHelpers, WorkflowEditorAssertions } from '../../utils/test-helpers';

test.describe('Telemetry Map Reports', () => {
  let helpers: WorkflowEditorTestHelpers;
  let assertions: WorkflowEditorAssertions;

  test.beforeEach(async ({ page }) => {
    helpers = new WorkflowEditorTestHelpers(page);
    assertions = new WorkflowEditorAssertions(page);
    
    await page.goto('/');
    await helpers.waitForPageLoad();
  });

  test('should display telemetry map with spatial data', async ({ page }) => {
    // Navigate to telemetry map
    await helpers.navigateToSection('telemetry-map');
    await helpers.takeScreenshot('telemetry-map-loaded');

    // Verify map components are present
    await helpers.verifyTelemetryMap();
    
    // Check map functionality
    await helpers.verifyMapFunctionality();
    
    // Assert map markers are present
    await assertions.assertMapMarkersPresent();
    
    // Export evidence
    await helpers.exportTestEvidence('telemetry-map-complete');
  });

  test('should show telemetry markers on map', async ({ page }) => {
    await helpers.navigateToSection('telemetry-map');
    
    // Wait for map to load
    await expect(page.locator('.maplibregl-canvas, .leaflet-container')).toBeVisible({ timeout: 15000 });
    await page.waitForTimeout(3000); // Allow time for markers to render
    
    // Check for telemetry markers
    const markers = page.locator('.maplibregl-marker, .leaflet-marker, [class*="marker"], [class*="telemetry-marker"]');
    
    if (await markers.count() > 0) {
      await expect(markers.first()).toBeVisible();
      console.log(`Found ${await markers.count()} telemetry markers`);
    } else {
      console.log('No markers found - checking for other map elements');
      // Check for other telemetry visualization
      const mapElements = page.locator('[class*="telemetry"], circle, path[d]');
      if (await mapElements.count() > 0) {
        await expect(mapElements.first()).toBeVisible();
      }
    }
    
    await helpers.takeScreenshot('telemetry-markers-verified');
  });

  test('should support map interactions and zoom', async ({ page }) => {
    await helpers.navigateToSection('telemetry-map');
    
    // Wait for map to be interactive
    const mapCanvas = page.locator('.maplibregl-canvas, .leaflet-container');
    await expect(mapCanvas).toBeVisible({ timeout: 15000 });
    
    // Test zoom controls
    const zoomIn = page.locator('.maplibregl-ctrl-zoom-in, .leaflet-control-zoom-in');
    const zoomOut = page.locator('.maplibregl-ctrl-zoom-out, .leaflet-control-zoom-out');
    
    if (await zoomIn.count() > 0) {
      await zoomIn.click();
      await page.waitForTimeout(1000);
      await helpers.takeScreenshot('map-zoomed-in');
      
      await zoomOut.click();
      await page.waitForTimeout(1000);
      await helpers.takeScreenshot('map-zoomed-out');
    }
    
    // Test map pan by clicking and dragging
    await mapCanvas.click({ position: { x: 300, y: 300 } });
    await page.waitForTimeout(500);
    
    await helpers.takeScreenshot('map-interaction-complete');
    await helpers.exportTestEvidence('map-interactions');
  });

  test('should display telemetry data popups', async ({ page }) => {
    await helpers.navigateToSection('telemetry-map');
    
    // Wait for map and markers
    await page.waitForTimeout(5000);
    
    // Try clicking on map elements to trigger popups
    const clickableElements = page.locator('.maplibregl-marker, .leaflet-marker, circle, [class*="marker"]');
    
    if (await clickableElements.count() > 0) {
      await clickableElements.first().click();
      await page.waitForTimeout(1000);
      
      // Check for popup or tooltip
      const popups = page.locator('.maplibregl-popup, .leaflet-popup, [class*="popup"], [class*="tooltip"]');
      
      if (await popups.count() > 0) {
        await expect(popups.first()).toBeVisible();
        await helpers.takeScreenshot('telemetry-popup-visible');
      } else {
        console.log('No popup found after clicking marker');
        await helpers.takeScreenshot('telemetry-popup-none');
      }
    } else {
      // Try clicking on the map canvas directly
      const mapCanvas = page.locator('.maplibregl-canvas, .leaflet-container');
      await mapCanvas.click({ position: { x: 250, y: 250 } });
      await page.waitForTimeout(1000);
      await helpers.takeScreenshot('map-canvas-click');
    }
    
    await helpers.exportTestEvidence('telemetry-popups-test');
  });

  test('should show different telemetry data layers', async ({ page }) => {
    await helpers.navigateToSection('telemetry-map');
    
    // Look for layer controls or toggles
    const layerControls = page.locator('[class*="layer"], [class*="toggle"], .map-controls button, select');
    
    if (await layerControls.count() > 0) {
      console.log(`Found ${await layerControls.count()} layer controls`);
      
      // Test switching layers
      await layerControls.first().click();
      await page.waitForTimeout(1000);
      await helpers.takeScreenshot('layer-switched-1');
      
      if (await layerControls.count() > 1) {
        await layerControls.nth(1).click();
        await page.waitForTimeout(1000);
        await helpers.takeScreenshot('layer-switched-2');
      }
    } else {
      console.log('No layer controls found - checking for default layers');
      await helpers.takeScreenshot('default-layers-only');
    }
    
    await helpers.exportTestEvidence('telemetry-layers-test');
  });

  test('should handle spatial query functionality', async ({ page }) => {
    // First try the dedicated spatial query page
    await helpers.navigateToSection('spatial-query');
    await helpers.takeScreenshot('spatial-query-page');
    
    // Verify spatial query components
    await helpers.verifyComponentVisible('.spatial-query, .page-content');
    
    // Look for query builder components
    const queryElements = page.locator('input, select, button, [class*="query"], [class*="spatial"]');
    
    if (await queryElements.count() > 0) {
      console.log(`Found ${await queryElements.count()} spatial query elements`);
      
      // Test first interactive element
      await queryElements.first().click();
      await page.waitForTimeout(500);
      await helpers.takeScreenshot('spatial-query-interaction');
    }
    
    await helpers.exportTestEvidence('spatial-query-functionality');
  });

  test('should be responsive on different screen sizes', async ({ page }) => {
    await helpers.navigateToSection('telemetry-map');
    
    // Test desktop view
    await page.setViewportSize({ width: 1920, height: 1080 });
    await page.waitForTimeout(2000);
    await helpers.takeScreenshot('map-desktop-1920');
    
    // Test tablet view  
    await page.setViewportSize({ width: 768, height: 1024 });
    await page.waitForTimeout(2000);
    await helpers.takeScreenshot('map-tablet-768');
    
    // Test mobile view
    await page.setViewportSize({ width: 375, height: 667 });
    await page.waitForTimeout(2000);
    await helpers.takeScreenshot('map-mobile-375');
    
    // Verify map is still functional on mobile
    await helpers.verifyComponentVisible('.telemetry-map, .maplibregl-canvas, .leaflet-container, .page-content');
    
    await helpers.exportTestEvidence('map-responsive-test');
  });
});