#!/usr/bin/env node

const fs = require('fs');
const path = require('path');

class TestReportGenerator {
  constructor(name) {
    this.name = name;
    this.summary = JSON.parse(fs.readFileSync('test-summary.json', 'utf8'));
    this.errorAnalysis = JSON.parse(fs.readFileSync('error-analysis.json', 'utf8'));
  }

  generateHTML() {
    const html = `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Test Report - ${this.name}</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            line-height: 1.6;
            color: #333;
            max-width: 1200px;
            margin: 0 auto;
            padding: 20px;
            background-color: #f5f5f5;
        }
        .header {
            background-color: #fff;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            margin-bottom: 20px;
        }
        h1 {
            margin: 0;
            color: #2c3e50;
        }
        .summary {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
            margin-bottom: 30px;
        }
        .metric {
            background-color: #fff;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            text-align: center;
        }
        .metric h3 {
            margin: 0;
            font-size: 14px;
            color: #7f8c8d;
            text-transform: uppercase;
        }
        .metric .value {
            font-size: 36px;
            font-weight: bold;
            margin: 10px 0;
        }
        .metric.passed .value { color: #27ae60; }
        .metric.failed .value { color: #e74c3c; }
        .metric.skipped .value { color: #f39c12; }
        .metric.total .value { color: #3498db; }
        .failures {
            background-color: #fff;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        .failure {
            border-left: 4px solid #e74c3c;
            padding: 15px;
            margin-bottom: 15px;
            background-color: #fff5f5;
            border-radius: 4px;
        }
        .failure h4 {
            margin: 0 0 10px 0;
            color: #c0392b;
        }
        .failure .file {
            font-size: 12px;
            color: #666;
            font-family: monospace;
        }
        .failure .error {
            margin: 10px 0;
            color: #e74c3c;
        }
        .failure .stack {
            font-family: monospace;
            font-size: 12px;
            background-color: #f8f8f8;
            padding: 10px;
            border-radius: 4px;
            overflow-x: auto;
            max-height: 200px;
            overflow-y: auto;
        }
        .patterns {
            background-color: #fff;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            margin-top: 20px;
        }
        .pattern {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 10px 0;
            border-bottom: 1px solid #ecf0f1;
        }
        .pattern:last-child {
            border-bottom: none;
        }
        .pattern .type {
            font-weight: bold;
            color: #2c3e50;
        }
        .pattern .count {
            background-color: #e74c3c;
            color: white;
            padding: 2px 8px;
            border-radius: 12px;
            font-size: 12px;
        }
        .progress-bar {
            width: 100%;
            height: 30px;
            background-color: #ecf0f1;
            border-radius: 15px;
            overflow: hidden;
            margin: 20px 0;
        }
        .progress-bar .passed {
            height: 100%;
            background-color: #27ae60;
            float: left;
            display: flex;
            align-items: center;
            justify-content: center;
            color: white;
            font-weight: bold;
        }
        .progress-bar .failed {
            height: 100%;
            background-color: #e74c3c;
            float: left;
            display: flex;
            align-items: center;
            justify-content: center;
            color: white;
            font-weight: bold;
        }
        .progress-bar .skipped {
            height: 100%;
            background-color: #f39c12;
            float: left;
            display: flex;
            align-items: center;
            justify-content: center;
            color: white;
            font-weight: bold;
        }
        .timestamp {
            color: #7f8c8d;
            font-size: 14px;
        }
    </style>
</head>
<body>
    <div class="header">
        <h1>📊 Test Report: ${this.name}</h1>
        <p class="timestamp">Generated on ${new Date().toLocaleString()}</p>
    </div>

    <div class="progress-bar">
        <div class="passed" style="width: ${this.summary.passRate}%">${this.summary.passRate}%</div>
        <div class="failed" style="width: ${this.summary.failRate}%">${this.summary.failRate}%</div>
        <div class="skipped" style="width: ${this.summary.skipRate}%">${this.summary.skipRate}%</div>
    </div>

    <div class="summary">
        <div class="metric total">
            <h3>Total Tests</h3>
            <div class="value">${this.summary.total}</div>
        </div>
        <div class="metric passed">
            <h3>Passed</h3>
            <div class="value">${this.summary.passed}</div>
            <small>${this.summary.passRate}%</small>
        </div>
        <div class="metric failed">
            <h3>Failed</h3>
            <div class="value">${this.summary.failed}</div>
            <small>${this.summary.failRate}%</small>
        </div>
        <div class="metric skipped">
            <h3>Skipped</h3>
            <div class="value">${this.summary.skipped}</div>
            <small>${this.summary.skipRate}%</small>
        </div>
    </div>

    ${this.summary.failed > 0 ? this.generateFailuresSection() : ''}
    ${this.errorAnalysis.patterns && this.errorAnalysis.patterns.length > 0 ? this.generatePatternsSection() : ''}
</body>
</html>`;

    return html;
  }

  generateFailuresSection() {
    let html = '<div class="failures"><h2>❌ Failed Tests</h2>';
    
    this.errorAnalysis.failures.slice(0, 20).forEach(failure => {
      html += `
        <div class="failure">
            <h4>${this.escapeHtml(failure.name)}</h4>
            <div class="file">📁 ${this.escapeHtml(failure.file)}</div>
            <div class="error">${this.escapeHtml(failure.error)}</div>
            ${failure.stackTrace ? `<pre class="stack">${this.escapeHtml(failure.stackTrace)}</pre>` : ''}
        </div>
      `;
    });

    if (this.errorAnalysis.failures.length > 20) {
      html += `<p><em>... and ${this.errorAnalysis.failures.length - 20} more failures</em></p>`;
    }

    html += '</div>';
    return html;
  }

  generatePatternsSection() {
    let html = '<div class="patterns"><h2>📈 Error Patterns</h2>';
    
    this.errorAnalysis.patterns.forEach(pattern => {
      html += `
        <div class="pattern">
            <div>
                <div class="type">${this.escapeHtml(pattern.type)}</div>
                <small>${this.escapeHtml(pattern.description)}</small>
            </div>
            <div>
                <span class="count">${pattern.count}</span>
            </div>
        </div>
      `;
    });

    html += '</div>';
    return html;
  }

  escapeHtml(text) {
    const map = {
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#039;'
    };
    return String(text).replace(/[&<>"']/g, m => map[m]);
  }

  generateMarkdown() {
    let md = `# Test Report: ${this.name}\n\n`;
    md += `Generated on ${new Date().toLocaleString()}\n\n`;
    
    md += `## Summary\n\n`;
    md += `| Metric | Count | Percentage |\n`;
    md += `|--------|-------|------------|\n`;
    md += `| Total | ${this.summary.total} | 100% |\n`;
    md += `| ✅ Passed | ${this.summary.passed} | ${this.summary.passRate}% |\n`;
    md += `| ❌ Failed | ${this.summary.failed} | ${this.summary.failRate}% |\n`;
    md += `| ⏭️ Skipped | ${this.summary.skipped} | ${this.summary.skipRate}% |\n\n`;

    if (this.summary.failed > 0) {
      md += `## Failed Tests\n\n`;
      this.errorAnalysis.failures.slice(0, 10).forEach((failure, index) => {
        md += `### ${index + 1}. ${failure.name}\n`;
        md += `- **File:** \`${failure.file}\`\n`;
        md += `- **Error:** ${failure.error}\n`;
        if (failure.stackTrace) {
          md += `- **Stack Trace:**\n\`\`\`\n${failure.stackTrace.slice(0, 500)}...\n\`\`\`\n`;
        }
        md += `\n`;
      });

      if (this.errorAnalysis.failures.length > 10) {
        md += `*... and ${this.errorAnalysis.failures.length - 10} more failures*\n\n`;
      }
    }

    if (this.errorAnalysis.patterns && this.errorAnalysis.patterns.length > 0) {
      md += `## Error Patterns\n\n`;
      this.errorAnalysis.patterns.forEach(pattern => {
        md += `- **${pattern.type}**: ${pattern.count} occurrences (${pattern.percentage}%)\n`;
        md += `  - ${pattern.description}\n`;
      });
    }

    return md;
  }

  saveReports() {
    // Save HTML report
    const htmlReport = this.generateHTML();
    fs.writeFileSync('test-report.html', htmlReport);
    console.log('Generated test-report.html');

    // Save Markdown report
    const mdReport = this.generateMarkdown();
    fs.writeFileSync('test-report.md', mdReport);
    console.log('Generated test-report.md');

    // Output URL for GitHub Actions
    const artifactUrl = `${process.env.GITHUB_SERVER_URL}/${process.env.GITHUB_REPOSITORY}/actions/runs/${process.env.GITHUB_RUN_ID}`;
    console.log(`::set-output name=url::${artifactUrl}`);
  }
}

// Parse command line arguments
const args = process.argv.slice(2);
let name = 'Test Results';

for (let i = 0; i < args.length; i++) {
  if (args[i] === '--name' && i + 1 < args.length) {
    name = args[i + 1];
  }
}

// Generate reports
const generator = new TestReportGenerator(name);
generator.saveReports();