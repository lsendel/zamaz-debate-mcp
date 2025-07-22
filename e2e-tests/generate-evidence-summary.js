const fs = require('fs').promises;
const path = require('path');

async function generateEvidenceSummary(testRunDir) {
  console.log('Generating evidence summary...');
  
  const summary = {
    testRunId: path.basename(testRunDir),
    timestamp: new Date().toISOString(),
    environment: {
      nodeVersion: process.version,
      platform: process.platform,
      baseUrl: process.env.BASE_URL || 'http://localhost:3000',
      apiUrl: process.env.API_BASE_URL || 'http://localhost:8080'
    },
    testResults: {},
    screenshots: [],
    videos: [],
    logs: [],
    keyFindings: [],
    recommendations: []
  };

  try {
    // Read test results
    const resultsPath = path.join(testRunDir, 'report.json');
    if (await fileExists(resultsPath)) {
      const results = JSON.parse(await fs.readFile(resultsPath, 'utf8'));
      summary.testResults = {
        total: results.stats.expected,
        passed: results.stats.expected - results.stats.unexpected,
        failed: results.stats.unexpected,
        skipped: results.stats.skipped,
        duration: results.stats.duration
      };
    }

    // Collect screenshots
    const screenshotsDir = path.join(testRunDir, 'screenshots');
    if (await fileExists(screenshotsDir)) {
      const screenshots = await fs.readdir(screenshotsDir);
      summary.screenshots = screenshots.filter(f => f.endsWith('.png'));
    }

    // Collect videos
    const videosDir = path.join(testRunDir, 'videos');
    if (await fileExists(videosDir)) {
      const videos = await fs.readdir(videosDir);
      summary.videos = videos.filter(f => f.endsWith('.webm') || f.endsWith('.mp4'));
    }

    // Collect logs
    const logsDir = path.join(testRunDir, 'logs');
    if (await fileExists(logsDir)) {
      const logs = await fs.readdir(logsDir);
      summary.logs = logs.filter(f => f.endsWith('.json') || f.endsWith('.log'));
    }

    // Analyze test suite summaries
    const testSuites = [
      'debate-creation',
      'participant-management',
      'real-time-interaction',
      'quality-analysis'
    ];

    for (const suite of testSuites) {
      const summaryPath = path.join(testRunDir, '..', `*-${suite}`, 'summary.json');
      const files = await findFiles(path.dirname(summaryPath), 'summary.json');
      
      for (const file of files) {
        const suiteSummary = JSON.parse(await fs.readFile(file, 'utf8'));
        if (suiteSummary.keyFindings) {
          summary.keyFindings.push(...suiteSummary.keyFindings);
        }
      }
    }

    // Generate recommendations based on results
    if (summary.testResults.failed > 0) {
      summary.recommendations.push('Fix failing tests before deployment');
    }
    
    if (summary.screenshots.length < 20) {
      summary.recommendations.push('Consider adding more screenshot evidence for better coverage');
    }

    summary.recommendations.push('Run performance testing under load conditions');
    summary.recommendations.push('Validate WebSocket connection stability over extended periods');
    summary.recommendations.push('Test with multiple concurrent users');

    // Write comprehensive summary
    await fs.writeFile(
      path.join(testRunDir, 'evidence-summary.json'),
      JSON.stringify(summary, null, 2)
    );

    // Generate executive summary
    const executiveSummary = `
DEBATE PLATFORM E2E TEST EVIDENCE SUMMARY
========================================

Test Run ID: ${summary.testRunId}
Date: ${new Date().toLocaleDateString()}
Time: ${new Date().toLocaleTimeString()}

TEST RESULTS
------------
Total Tests: ${summary.testResults.total || 'N/A'}
Passed: ${summary.testResults.passed || 'N/A'}
Failed: ${summary.testResults.failed || 'N/A'}
Duration: ${Math.round((summary.testResults.duration || 0) / 1000)}s

EVIDENCE COLLECTED
-----------------
Screenshots: ${summary.screenshots.length}
Videos: ${summary.videos.length}
Logs: ${summary.logs.length}

KEY FINDINGS
------------
${summary.keyFindings.map((f, i) => `${i + 1}. ${f}`).join('\n')}

RECOMMENDATIONS
--------------
${summary.recommendations.map((r, i) => `${i + 1}. ${r}`).join('\n')}

EVIDENCE LOCATION
----------------
${testRunDir}
`;

    await fs.writeFile(
      path.join(testRunDir, 'executive-summary.txt'),
      executiveSummary
    );

    console.log('Evidence summary generated successfully!');
    
  } catch (error) {
    console.error('Error generating evidence summary:', error);
  }
}

async function fileExists(filePath) {
  try {
    await fs.access(filePath);
    return true;
  } catch {
    return false;
  }
}

async function findFiles(dir, filename) {
  const files = [];
  try {
    const entries = await fs.readdir(dir, { withFileTypes: true });
    for (const entry of entries) {
      const fullPath = path.join(dir, entry.name);
      if (entry.isDirectory()) {
        files.push(...await findFiles(fullPath, filename));
      } else if (entry.name === filename) {
        files.push(fullPath);
      }
    }
  } catch (error) {
      console.error("Error:", error);
    // Directory might not exist
    console.error("Error:", error);
  }
  return files;
}

// Run if called directly
if (require.main === module) {
  const testRunDir = process.argv[2];
  if (!testRunDir) {
    console.error('Usage: node generate-evidence-summary.js <test-run-directory>');
    process.exit(1);
  }
  generateEvidenceSummary(testRunDir);
}

module.exports = { generateEvidenceSummary };