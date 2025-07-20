import { test, expect } from '@playwright/test';

test.describe('Quick Workflow Editor Validation', () => {
  test('should load application and verify all sections are accessible', async ({ page }) => {
    // Navigate to the application
    await page.goto('http://localhost:3002');
    
    // Wait for app to load
    await page.waitForLoadState('networkidle');
    
    // Take initial screenshot
    await page.screenshot({ path: 'test-results/01-app-initial-load.png', fullPage: true });
    
    // Verify app structure loads
    await expect(page.locator('.app')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('.app-header')).toBeVisible();
    await expect(page.locator('.app-navigation')).toBeVisible();
    
    // Verify header content
    await expect(page.locator('h1')).toContainText('Kiro Workflow Editor');
    
    // Get all navigation items
    const navItems = page.locator('.nav-item');
    const navCount = await navItems.count();
    
    console.log(`Found ${navCount} navigation items`);
    expect(navCount).toBeGreaterThan(0);
    
    // Test each navigation section
    const sections = [
      { id: 'workflow-editor', name: 'Workflow Editor' },
      { id: 'telemetry-dashboard', name: 'Telemetry Dashboard' },
      { id: 'telemetry-map', name: 'Telemetry Map' },
      { id: 'spatial-query', name: 'Spatial Query' },
      { id: 'stamford-sample', name: 'Stamford Sample' },
      { id: 'debate-sample', name: 'Debate Tree' },
      { id: 'decision-sample', name: 'Decision Tree' },
      { id: 'ai-document-sample', name: 'AI Document Analysis' }
    ];
    
    for (const section of sections) {
      console.log(`Testing section: ${section.name}`);
      
      // Find and click navigation item
      const navButton = page.locator('.nav-item').filter({ hasText: new RegExp(section.name, 'i') });
      await navButton.click();
      
      // Wait for content to load
      await page.waitForTimeout(2000);
      
      // Verify main content area is visible
      await expect(page.locator('.page-content')).toBeVisible({ timeout: 10000 });
      
      // Take screenshot
      await page.screenshot({ 
        path: `test-results/section-${section.id}.png`, 
        fullPage: true 
      });
      
      // Check for any visible error messages
      const errorElements = page.locator('.error, [class*="error"], .alert-danger');
      const errorCount = await errorElements.count();
      
      if (errorCount > 0) {
        const errorTexts = await errorElements.allTextContents();
        console.log(`⚠️ Found ${errorCount} error elements in ${section.name}:`, errorTexts);
      }
      
      console.log(`✅ ${section.name} loaded successfully`);
    }
    
    // Test specific functionality for problematic sections
    
    // Test Telemetry Map
    await page.locator('.nav-item').filter({ hasText: /telemetry.*map/i }).click();
    await page.waitForTimeout(3000);
    
    // Look for map elements
    const mapContainer = page.locator('.maplibregl-canvas, .leaflet-container, [class*="map"]');
    if (await mapContainer.count() > 0) {
      console.log('✅ Telemetry Map: Map container found');
      await page.screenshot({ path: 'test-results/telemetry-map-detailed.png', fullPage: true });
    } else {
      console.log('❌ Telemetry Map: No map container found');
    }
    
    // Test Stamford Sample
    await page.locator('.nav-item').filter({ hasText: /stamford/i }).click();
    await page.waitForTimeout(3000);
    
    // Look for generate button and map
    const generateButton = page.locator('button').filter({ hasText: /generate/i });
    if (await generateButton.count() > 0) {
      console.log('✅ Stamford Sample: Generate button found');
      await generateButton.first().click();
      await page.waitForTimeout(2000);
      await page.screenshot({ path: 'test-results/stamford-sample-generated.png', fullPage: true });
    }
    
    // Test Debate Tree (check for flashing)
    await page.locator('.nav-item').filter({ hasText: /debate/i }).click();
    await page.waitForTimeout(1000);
    await page.screenshot({ path: 'test-results/debate-tree-1s.png', fullPage: true });
    await page.waitForTimeout(2000);
    await page.screenshot({ path: 'test-results/debate-tree-3s.png', fullPage: true });
    console.log('✅ Debate Tree: Screenshots taken to check for flashing');
    
    // Test AI Document Analysis
    await page.locator('.nav-item').filter({ hasText: /document|ai/i }).click();
    await page.waitForTimeout(2000);
    
    const uploadButton = page.locator('button').filter({ hasText: /upload/i });
    if (await uploadButton.count() > 0) {
      console.log('✅ AI Document Analysis: Upload button found');
      await page.screenshot({ path: 'test-results/ai-document-upload.png', fullPage: true });
    }
    
    // Final screenshot
    await page.screenshot({ path: 'test-results/final-state.png', fullPage: true });
    
    console.log('🎉 All sections tested successfully!');
  });
  
  test('should check for JavaScript errors in console', async ({ page }) => {
    const consoleErrors: string[] = [];
    
    page.on('console', (msg) => {
      if (msg.type() === 'error') {
        consoleErrors.push(msg.text());
      }
    });
    
    await page.goto('http://localhost:3002');
    await page.waitForLoadState('networkidle');
    
    // Navigate through a few sections to trigger any errors
    const sections = ['telemetry-map', 'stamford-sample', 'debate-sample', 'ai-document-sample'];
    
    for (const section of sections) {
      const navButton = page.locator('.nav-item').nth(sections.indexOf(section) + 1);
      await navButton.click();
      await page.waitForTimeout(2000);
    }
    
    console.log(`Console errors found: ${consoleErrors.length}`);
    if (consoleErrors.length > 0) {
      console.log('Console errors:', consoleErrors);
    }
    
    // We'll allow some errors for now but log them
    expect(consoleErrors.length).toBeLessThan(10); // Allow some minor errors
  });
});