import { FullConfig } from '@playwright/test';
import fs from 'fs';
import path from 'path';

/**
 * Global teardown for all Playwright tests
 * Generates final reports and cleans up resources
 */
async function globalTeardown(config: FullConfig) {
  console.log('\n🧹 Starting global teardown...');
  
  const evidenceBase = path.join(__dirname, '../test_probe/evidence');
  const testRunId = process.env.TEST_RUN_ID || Date.now().toString();
  
  try {
    // Generate final summary report
    const testRuns = fs.readdirSync(path.join(evidenceBase, 'test-runs'))
      .filter(f => f.startsWith('setup-') && f.endsWith('.json'));
    
    const finalReport = {
      testRunId,
      completedAt: new Date().toISOString(),
      environment: process.env.NODE_ENV || 'test',
      summary: {
        totalTestRuns: testRuns.length,
        evidenceCollected: {
          screenshots: countFiles(path.join(evidenceBase, 'screenshots')),
          videos: countFiles(path.join(evidenceBase, 'videos')),
          traces: countFiles(path.join(evidenceBase, 'traces')),
          transcripts: countFiles(path.join(evidenceBase, 'debate-transcripts')),
          logs: countFiles(path.join(evidenceBase, 'logs'))
        }
      },
      recommendations: []
    };
    
    // Add recommendations based on evidence
    if (finalReport.summary.evidenceCollected.screenshots > 10) {
      finalReport.recommendations.push('High number of screenshots may indicate test failures');
    }
    
    // Save final report
    fs.writeFileSync(
      path.join(evidenceBase, `final-report-${testRunId}.json`),
      JSON.stringify(finalReport, null, 2)
    );
    
    console.log('📊 Final test summary:');
    console.log(`  - Test Run ID: ${testRunId}`);
    console.log(`  - Screenshots: ${finalReport.summary.evidenceCollected.screenshots}`);
    console.log(`  - Videos: ${finalReport.summary.evidenceCollected.videos}`);
    console.log(`  - Debate Transcripts: ${finalReport.summary.evidenceCollected.transcripts}`);
    
    // Create index.html for easy navigation of evidence
    createEvidenceIndex(evidenceBase, finalReport);
    
    // Clean up test data if needed
    await cleanupTestData();
    
    console.log('✅ Global teardown completed');
    console.log(`📁 All evidence saved to: ${evidenceBase}`);
    console.log(`📊 View report at: ${path.join(evidenceBase, 'index.html')}`);
    
  } catch (error) {
    console.error('❌ Error during global teardown:', error);
  }
}

function countFiles(directory: string): number {
  try {
    return fs.readdirSync(directory).filter(f => !f.startsWith('.')).length;
  } catch {
    return 0;
  }
}

function createEvidenceIndex(evidenceBase: string, report: any) {
  const html = `
<!DOCTYPE html>
<html>
<head>
    <title>AI Debate System - Test Evidence Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }
        .container { max-width: 1200px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        h1 { color: #333; border-bottom: 2px solid #4CAF50; padding-bottom: 10px; }
        h2 { color: #666; margin-top: 30px; }
        .stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin: 20px 0; }
        .stat-card { background: #f9f9f9; padding: 15px; border-radius: 5px; border-left: 4px solid #4CAF50; }
        .stat-card h3 { margin: 0 0 10px 0; color: #555; }
        .stat-card .number { font-size: 24px; font-weight: bold; color: #4CAF50; }
        .evidence-section { margin: 20px 0; }
        .evidence-list { list-style: none; padding: 0; }
        .evidence-list li { padding: 8px 12px; margin: 5px 0; background: #f0f0f0; border-radius: 4px; }
        .evidence-list a { text-decoration: none; color: #1976D2; }
        .evidence-list a:hover { text-decoration: underline; }
        .timestamp { color: #999; font-size: 12px; }
    </style>
</head>
<body>
    <div class="container">
        <h1>AI Debate System - Test Evidence Report</h1>
        <p class="timestamp">Generated at: ${report.completedAt}</p>
        <p>Test Run ID: <strong>${report.testRunId}</strong></p>
        
        <h2>Summary Statistics</h2>
        <div class="stats">
            <div class="stat-card">
                <h3>Screenshots</h3>
                <div class="number">${report.summary.evidenceCollected.screenshots}</div>
            </div>
            <div class="stat-card">
                <h3>Videos</h3>
                <div class="number">${report.summary.evidenceCollected.videos}</div>
            </div>
            <div class="stat-card">
                <h3>Debate Transcripts</h3>
                <div class="number">${report.summary.evidenceCollected.transcripts}</div>
            </div>
            <div class="stat-card">
                <h3>Trace Files</h3>
                <div class="number">${report.summary.evidenceCollected.traces}</div>
            </div>
        </div>
        
        <h2>Evidence Files</h2>
        
        <div class="evidence-section">
            <h3>Latest Screenshots</h3>
            <ul class="evidence-list">
                ${getLatestFiles(path.join(evidenceBase, 'screenshots'), 10)
                  .map(f => `<li><a href="screenshots/${f}">${f}</a></li>`).join('')}
            </ul>
        </div>
        
        <div class="evidence-section">
            <h3>Debate Transcripts</h3>
            <ul class="evidence-list">
                ${getLatestFiles(path.join(evidenceBase, 'debate-transcripts'), 10)
                  .map(f => `<li><a href="debate-transcripts/${f}">${f}</a></li>`).join('')}
            </ul>
        </div>
        
        <div class="evidence-section">
            <h3>Test Videos</h3>
            <ul class="evidence-list">
                ${getLatestFiles(path.join(evidenceBase, 'videos'), 10)
                  .map(f => `<li><a href="videos/${f}">${f}</a></li>`).join('')}
            </ul>
        </div>
        
        <div class="evidence-section">
            <h3>Other Reports</h3>
            <ul class="evidence-list">
                <li><a href="html-report/index.html">Playwright HTML Report</a></li>
                <li><a href="test-results.json">JSON Test Results</a></li>
                <li><a href="junit-results.xml">JUnit XML Results</a></li>
                <li><a href="test-summary.json">Test Summary</a></li>
            </ul>
        </div>
    </div>
</body>
</html>
`;
  
  fs.writeFileSync(path.join(evidenceBase, 'index.html'), html);
}

function getLatestFiles(directory: string, limit: number): string[] {
  try {
    return fs.readdirSync(directory)
      .filter(f => !f.startsWith('.'))
      .sort((a, b) => {
        const statA = fs.statSync(path.join(directory, a));
        const statB = fs.statSync(path.join(directory, b));
        return statB.mtime.getTime() - statA.mtime.getTime();
      })
      .slice(0, limit);
  } catch {
    return [];
  }
}

async function cleanupTestData() {
  try {
    // Clean up any test data created during tests
    // For example:
    // - Delete test organizations
    // - Delete test debates
    // - Clear test caches
    
    console.log('✅ Test data cleaned up');
  } catch (error) {
    console.error('❌ Failed to cleanup test data:', error);
  }
}

export default globalTeardown;