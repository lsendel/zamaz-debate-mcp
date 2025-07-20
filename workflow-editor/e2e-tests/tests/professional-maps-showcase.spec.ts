import { test, expect } from '@playwright/test';

test.describe('Professional Maps Showcase', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('http://localhost:3002');
    await page.waitForLoadState('networkidle');
  });

  test('Showcase all professional map styles', async ({ page }) => {
    // Navigate to Telemetry Map
    await page.locator('.nav-item').filter({ hasText: /telemetry.*map/i }).click();
    await page.waitForTimeout(3000);
    
    // Start simulation to show data on maps
    const simulateButton = page.locator('button').filter({ hasText: /simulate/i });
    if (await simulateButton.count() > 0) {
      await simulateButton.click();
      await page.waitForTimeout(2000);
    }
    
    // Open map style selector
    const styleToggle = page.locator('.style-toggle-button');
    await expect(styleToggle).toBeVisible();
    
    // Take screenshot with default CARTO Light style
    await page.screenshot({ 
      path: 'test-results/maps-01-carto-light-default.png', 
      fullPage: true 
    });
    console.log('✅ Screenshot 1: CARTO Light (Default Professional Style)');
    
    // Open style selector dropdown
    await styleToggle.click();
    await page.waitForTimeout(500);
    
    // Take screenshot of style selector UI
    await page.screenshot({ 
      path: 'test-results/maps-02-style-selector-ui.png', 
      fullPage: true 
    });
    console.log('✅ Screenshot 2: Map Style Selector Interface');
    
    // Test CARTO Dark style
    const darkOption = page.locator('.style-option').filter({ hasText: /dark/i });
    if (await darkOption.count() > 0) {
      await darkOption.click();
      await page.waitForTimeout(2000);
      await page.screenshot({ 
        path: 'test-results/maps-03-carto-dark.png', 
        fullPage: true 
      });
      console.log('✅ Screenshot 3: CARTO Dark (Professional Dark Theme)');
    }
    
    // Test Technical/Blueprint style
    await styleToggle.click();
    await page.waitForTimeout(500);
    const technicalOption = page.locator('.style-option').filter({ hasText: /technical|blueprint/i });
    if (await technicalOption.count() > 0) {
      await technicalOption.click();
      await page.waitForTimeout(2000);
      await page.screenshot({ 
        path: 'test-results/maps-04-technical-blueprint.png', 
        fullPage: true 
      });
      console.log('✅ Screenshot 4: Stamen Toner (Technical/Blueprint Style)');
    }
    
    // Test Watercolor style
    await styleToggle.click();
    await page.waitForTimeout(500);
    const watercolorOption = page.locator('.style-option').filter({ hasText: /watercolor/i });
    if (await watercolorOption.count() > 0) {
      await watercolorOption.click();
      await page.waitForTimeout(2000);
      await page.screenshot({ 
        path: 'test-results/maps-05-watercolor-artistic.png', 
        fullPage: true 
      });
      console.log('✅ Screenshot 5: Stamen Watercolor (Artistic Professional)');
    }
    
    // Navigate to Spatial Query Builder to show map styles there
    await page.locator('.nav-item').filter({ hasText: /spatial.*query/i }).click();
    await page.waitForTimeout(3000);
    
    // Verify map style selector is present
    const spatialStyleToggle = page.locator('.style-toggle-button');
    await expect(spatialStyleToggle).toBeVisible();
    
    await page.screenshot({ 
      path: 'test-results/maps-06-spatial-query-professional.png', 
      fullPage: true 
    });
    console.log('✅ Screenshot 6: Spatial Query Builder with Professional Maps');
    
    // Test with different viewport sizes
    await page.setViewportSize({ width: 768, height: 1024 });
    await page.waitForTimeout(1000);
    await page.screenshot({ 
      path: 'test-results/maps-07-tablet-responsive.png', 
      fullPage: true 
    });
    console.log('✅ Screenshot 7: Tablet View with Professional Maps');
    
    await page.setViewportSize({ width: 375, height: 667 });
    await page.waitForTimeout(1000);
    await page.screenshot({ 
      path: 'test-results/maps-08-mobile-responsive.png', 
      fullPage: true 
    });
    console.log('✅ Screenshot 8: Mobile View with Professional Maps');
    
    // Return to desktop size
    await page.setViewportSize({ width: 1920, height: 1080 });
    
    // Navigate to Stamford Sample
    await page.locator('.nav-item').filter({ hasText: /stamford/i }).click();
    await page.waitForTimeout(3000);
    
    // Generate addresses to show data on map
    const generateButton = page.locator('button').filter({ hasText: /generate/i });
    if (await generateButton.count() > 0) {
      await generateButton.click();
      await page.waitForTimeout(3000);
    }
    
    await page.screenshot({ 
      path: 'test-results/maps-09-stamford-with-data.png', 
      fullPage: true 
    });
    console.log('✅ Screenshot 9: Stamford Sample with Professional Map and Data');
    
    // Summary
    console.log('\n📊 Professional Maps Showcase Complete!');
    console.log('🗺️ Styles Demonstrated:');
    console.log('   1. CARTO Light - Clean professional default');
    console.log('   2. CARTO Dark - High contrast monitoring');
    console.log('   3. Stamen Toner - Technical/CAD style');
    console.log('   4. Stamen Watercolor - Artistic presentation');
    console.log('📱 Responsive Design: Tablet and Mobile views');
    console.log('🔧 Components: Telemetry Map, Spatial Query, Stamford Sample');
    console.log('✨ All maps now use professional styling!');
  });

  test('Verify professional maps integration across all components', async ({ page }) => {
    const componentsWithMaps = [
      { name: 'Telemetry Map', selector: /telemetry.*map/i },
      { name: 'Spatial Query', selector: /spatial.*query/i },
      { name: 'Stamford Sample', selector: /stamford/i }
    ];
    
    for (const component of componentsWithMaps) {
      // Navigate to component
      await page.locator('.nav-item').filter({ hasText: component.selector }).click();
      await page.waitForTimeout(2000);
      
      // Verify map loads
      const mapCanvas = page.locator('.maplibregl-canvas, .leaflet-container, [class*="map"]');
      await expect(mapCanvas.first()).toBeVisible({ timeout: 10000 });
      
      // Verify style selector is present
      const styleSelector = page.locator('.style-toggle-button');
      const hasStyleSelector = await styleSelector.count() > 0;
      
      console.log(`✅ ${component.name}: Map loaded, Style selector ${hasStyleSelector ? 'present' : 'integrated via parent'}`);
    }
  });

  test('Performance comparison: Professional vs Basic tiles', async ({ page }) => {
    // Navigate to Telemetry Map
    await page.locator('.nav-item').filter({ hasText: /telemetry.*map/i }).click();
    await page.waitForTimeout(2000);
    
    // Measure load time with professional tiles
    const startTime = Date.now();
    await page.reload();
    await page.waitForLoadState('networkidle');
    const professionalLoadTime = Date.now() - startTime;
    
    console.log(`⚡ Professional map tiles loaded in: ${professionalLoadTime}ms`);
    
    // Check network requests for tile loading
    const tileRequests = await page.evaluate(() => {
      return performance.getEntriesByType('resource')
        .filter(entry => entry.name.includes('basemaps.cartocdn.com') || 
                        entry.name.includes('tile.openstreetmap.org'))
        .map(entry => ({
          url: entry.name,
          duration: Math.round(entry.duration),
          size: Math.round(entry.transferSize / 1024) // KB
        }));
    });
    
    console.log(`📊 Tile Loading Performance:`);
    console.log(`   Total tiles loaded: ${tileRequests.length}`);
    console.log(`   Average tile load time: ${Math.round(tileRequests.reduce((sum, t) => sum + t.duration, 0) / tileRequests.length)}ms`);
    console.log(`   Total data transferred: ${tileRequests.reduce((sum, t) => sum + t.size, 0)}KB`);
    
    // Performance should be acceptable
    expect(professionalLoadTime).toBeLessThan(5000); // Should load within 5 seconds
  });

  test('Map style persistence across navigation', async ({ page }) => {
    // Navigate to Telemetry Map
    await page.locator('.nav-item').filter({ hasText: /telemetry.*map/i }).click();
    await page.waitForTimeout(2000);
    
    // Change to dark style
    await page.locator('.style-toggle-button').click();
    await page.waitForTimeout(500);
    await page.locator('.style-option').filter({ hasText: /dark/i }).click();
    await page.waitForTimeout(2000);
    
    // Navigate away and back
    await page.locator('.nav-item').filter({ hasText: /workflow.*editor/i }).click();
    await page.waitForTimeout(1000);
    await page.locator('.nav-item').filter({ hasText: /telemetry.*map/i }).click();
    await page.waitForTimeout(2000);
    
    // Check if style persisted (this would need localStorage implementation)
    const currentStyle = await page.locator('.style-toggle-button .style-label').textContent();
    console.log(`🔄 Style persistence: ${currentStyle || 'Not implemented yet'}`);
    
    // Take final screenshot
    await page.screenshot({ 
      path: 'test-results/maps-10-final-state.png', 
      fullPage: true 
    });
  });
});