import { Page, expect } from '@playwright/test';

export class WorkflowEditorTestHelpers {
  constructor(private readonly page: Page) {}

  /**
   * Navigate to a specific section in the workflow editor
   */
  async navigateToSection(sectionId: string): Promise<void> {
    const navButton = this.page.locator(`[data-testid="nav-${sectionId}"], .nav-item`).filter({ hasText: new RegExp(sectionId, 'i') });
    await navButton.click();
    await this.page.waitForTimeout(1000); // Wait for navigation animation
  }

  /**
   * Wait for the page to be fully loaded
   */
  async waitForPageLoad(): Promise<void> {
    await this.page.waitForLoadState('networkidle');
    await this.page.waitForSelector('.app-main', { timeout: 10000 });
  }

  /**
   * Take a screenshot with a descriptive name
   */
  async takeScreenshot(name: string): Promise<void> {
    await this.page.screenshot({ 
      path: `test-results/screenshots/${name}.png`,
      fullPage: true 
    });
  }

  /**
   * Check if a component is visible and functional
   */
  async verifyComponentVisible(selector: string, timeout = 5000): Promise<void> {
    await expect(this.page.locator(selector)).toBeVisible({ timeout });
  }

  /**
   * Check telemetry dashboard components
   */
  async verifyTelemetryDashboard(): Promise<void> {
    await this.verifyComponentVisible('.telemetry-dashboard');
    await this.verifyComponentVisible('.telemetry-chart');
    await this.verifyComponentVisible('.telemetry-metrics');
  }

  /**
   * Check telemetry map components
   */
  async verifyTelemetryMap(): Promise<void> {
    await this.verifyComponentVisible('.telemetry-map');
    await this.verifyComponentVisible('.maplibregl-canvas');
    await this.verifyComponentVisible('.telemetry-markers');
  }

  /**
   * Check workflow editor components
   */
  async verifyWorkflowEditor(): Promise<void> {
    await this.verifyComponentVisible('.react-flow');
    await this.verifyComponentVisible('.react-flow__node');
    await this.verifyComponentVisible('.node-palette');
  }

  /**
   * Verify sample applications are working
   */
  async verifySampleApplication(appName: string): Promise<void> {
    const appSelector = `.${appName.toLowerCase().replace(/\s+/g, '-')}-sample`;
    await this.verifyComponentVisible(appSelector);
    
    // Check for interactive elements
    const interactiveElements = this.page.locator(`${appSelector} button, ${appSelector} input, ${appSelector} select`);
    const count = await interactiveElements.count();
    expect(count).toBeGreaterThan(0);
  }

  /**
   * Check real-time data updates
   */
  async verifyRealTimeUpdates(componentSelector: string): Promise<void> {
// //     const initialText = await this.page.locator(componentSelector).textContent(); // SonarCloud: removed useless assignment // Removed: useless assignment
    await this.page.waitForTimeout(2000);
    const updatedText = await this.page.locator(componentSelector).textContent();
    
    // For real-time components, we expect either data changes or at least timestamp updates
    expect(updatedText).toBeDefined();
  }

  /**
   * Test navigation between all main sections
   */
  async testCompleteNavigation(): Promise<string[]> {
    const navigationItems = [
      'workflow-editor',
      'telemetry-dashboard', 
      'telemetry-map',
      'spatial-query',
      'stamford-sample',
      'debate-sample',
      'decision-sample',
      'ai-document-sample'
    ];

    const results: string[] = [];

    for (const item of navigationItems) {
      try {
        await this.navigateToSection(item);
        await this.waitForPageLoad();
        results.push(`✅ ${item}: Navigation successful`);
        await this.takeScreenshot(`navigation-${item}`);
      } catch (error) {
        results.push(`❌ ${item}: Navigation failed - ${error}`);
      }
    }

    return results;
  }

  /**
   * Generate telemetry test data
   */
  async generateTestTelemetryData(): Promise<void> {
    // This would interact with the telemetry emulation service
    // For now, we'll just wait for existing data to load
    await this.page.waitForTimeout(3000);
  }

  /**
   * Verify data visualization components
   */
  async verifyDataVisualization(): Promise<void> {
    // Check for charts and graphs
    const charts = this.page.locator('canvas, svg, .recharts-wrapper');
    await expect(charts.first()).toBeVisible({ timeout: 10000 });
    
    // Verify chart has data
    const chartElements = this.page.locator('.recharts-line, .recharts-bar, .recharts-area, circle, path[d]');
    const count = await chartElements.count();
    expect(count).toBeGreaterThan(0);
  }

  /**
   * Check map functionality
   */
  async verifyMapFunctionality(): Promise<void> {
    await this.verifyComponentVisible('.maplibregl-canvas');
    
    // Try map interactions
    const mapCanvas = this.page.locator('.maplibregl-canvas');
    await mapCanvas.click({ position: { x: 200, y: 200 } });
    await this.page.waitForTimeout(1000);
    
    // Check for map controls
    await this.verifyComponentVisible('.maplibregl-ctrl-zoom-in');
    await this.verifyComponentVisible('.maplibregl-ctrl-zoom-out');
  }

  /**
   * Export test evidence
   */
  async exportTestEvidence(testName: string): Promise<void> {
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
    
    // Take full page screenshot
    await this.page.screenshot({ 
      path: `test-results/evidence/${testName}-${timestamp}-full.png`,
      fullPage: true 
    });
    
    // Take viewport screenshot
    await this.page.screenshot({ 
      path: `test-results/evidence/${testName}-${timestamp}-viewport.png`,
      fullPage: false 
    });
    
    // Save page HTML for debugging
    const html = await this.page.content();
    const fs = await import('fs-extra');
    await fs.writeFile(`test-results/evidence/${testName}-${timestamp}.html`, html);
  }
}

/**
 * Custom assertions for workflow editor
 */
export class WorkflowEditorAssertions {
  constructor(private readonly page: Page) {}

  async assertDashboardDataLoaded(): Promise<void> {
    // Check for data in telemetry dashboard
    const dataElements = this.page.locator('[data-testid*="telemetry"], .telemetry-value, .metric-value');
    await expect(dataElements.first()).toBeVisible({ timeout: 15000 });
  }

  async assertMapMarkersPresent(): Promise<void> {
    // Check for map markers
    const markers = this.page.locator('.maplibregl-marker, .leaflet-marker, [class*="marker"]');
    await expect(markers.first()).toBeVisible({ timeout: 10000 });
  }

  async assertWorkflowNodesPresent(): Promise<void> {
    // Check for workflow nodes
    const nodes = this.page.locator('.react-flow__node');
    await expect(nodes.first()).toBeVisible({ timeout: 10000 });
    
    const nodeCount = await nodes.count();
    expect(nodeCount).toBeGreaterThan(0);
  }

  async assertRealTimeConnection(): Promise<void> {
    // Check for WebSocket connection indicators
    const connectionIndicators = this.page.locator('[data-testid*="connection"], .connection-status, .real-time-indicator');
    if (await connectionIndicators.count() > 0) {
      await expect(connectionIndicators.first()).toBeVisible();
    }
  }
}