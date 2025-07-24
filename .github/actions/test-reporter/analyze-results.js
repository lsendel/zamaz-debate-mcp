#!/usr/bin/env node

const fs = require('fs');
const path = require('path');
const glob = require('glob');
const { DOMParser } = require('xmldom');
const xpath = require('xpath');

class TestResultAnalyzer {
  constructor(resultsPath, format) {
    this.resultsPath = resultsPath;
    this.format = format;
    this.results = {
      total: 0,
      passed: 0,
      failed: 0,
      skipped: 0,
      failures: [],
      duration: 0
    };
  }

  async analyze() {
    const files = glob.sync(this.resultsPath);
    console.log(`Found ${files.length} test result files`);

    for (const file of files) {
      console.log(`Analyzing ${file}...`);
      await this.analyzeFile(file);
    }

    // Calculate percentages
    this.results.passRate = this.results.total > 0 
      ? ((this.results.passed / this.results.total) * 100).toFixed(2) 
      : 0;
    this.results.failRate = this.results.total > 0 
      ? ((this.results.failed / this.results.total) * 100).toFixed(2) 
      : 0;
    this.results.skipRate = this.results.total > 0 
      ? ((this.results.skipped / this.results.total) * 100).toFixed(2) 
      : 0;

    // Analyze error patterns
    this.results.patterns = this.analyzeErrorPatterns();

    return this.results;
  }

  async analyzeFile(filePath) {
    const content = fs.readFileSync(filePath, 'utf8');

    switch (this.format) {
      case 'junit':
      case 'jest':
        this.analyzeJUnit(content, filePath);
        break;
      case 'mocha':
        this.analyzeMocha(content, filePath);
        break;
      default:
        throw new Error(`Unsupported format: ${this.format}`);
    }
  }

  analyzeJUnit(content, filePath) {
    const doc = new DOMParser().parseFromString(content);
    const testsuites = xpath.select('//testsuite', doc);

    testsuites.forEach(testsuite => {
      const tests = parseInt(testsuite.getAttribute('tests') || '0');
      const failures = parseInt(testsuite.getAttribute('failures') || '0');
      const errors = parseInt(testsuite.getAttribute('errors') || '0');
      const skipped = parseInt(testsuite.getAttribute('skipped') || '0');
      const time = parseFloat(testsuite.getAttribute('time') || '0');

      this.results.total += tests;
      this.results.failed += failures + errors;
      this.results.skipped += skipped;
      this.results.passed += tests - failures - errors - skipped;
      this.results.duration += time * 1000; // Convert to ms

      // Analyze individual test cases
      const testcases = xpath.select('.//testcase', testsuite);
      testcases.forEach(testcase => {
        const failure = xpath.select('./failure', testcase)[0];
        const error = xpath.select('./error', testcase)[0];

        if (failure || error) {
          const failureInfo = {
            name: testcase.getAttribute('name'),
            classname: testcase.getAttribute('classname'),
            file: this.extractFileFromClassname(testcase.getAttribute('classname')),
            time: parseFloat(testcase.getAttribute('time') || '0') * 1000,
            error: '',
            stackTrace: '',
            type: 'failure'
          };

          if (failure) {
            failureInfo.error = failure.getAttribute('message') || failure.textContent;
            failureInfo.stackTrace = failure.textContent;
            failureInfo.type = failure.getAttribute('type') || 'assertion';
          } else if (error) {
            failureInfo.error = error.getAttribute('message') || error.textContent;
            failureInfo.stackTrace = error.textContent;
            failureInfo.type = error.getAttribute('type') || 'error';
          }

          this.results.failures.push(failureInfo);
        }
      });
    });
  }

  analyzeMocha(content, filePath) {
    // Parse Mocha JSON reporter output
    try {
      const report = JSON.parse(content);
      this.results.total += report.stats.tests;
      this.results.passed += report.stats.passes;
      this.results.failed += report.stats.failures;
      this.results.skipped += report.stats.pending;
      this.results.duration += report.stats.duration;

      // Analyze failures
      if (report.failures) {
        report.failures.forEach(failure => {
          this.results.failures.push({
            name: failure.title,
            classname: failure.fullTitle,
            file: failure.file,
            time: failure.duration,
            error: failure.err.message,
            stackTrace: failure.err.stack,
            type: failure.err.name || 'error'
          });
        });
      }
    } catch (e) {
      console.error(`Failed to parse Mocha results: ${e.message}`);
    }
  }

  extractFileFromClassname(classname) {
    if (!classname) return 'unknown';
    
    // Try to extract file path from classname
    // Examples: 
    // - com.example.TestClass -> com/example/TestClass.java
    // - src.components.Button.test -> src/components/Button.test.js
    
    const parts = classname.split('.');
    if (parts.length > 1) {
      // Remove test suffix if present
      const lastPart = parts[parts.length - 1];
      if (lastPart.endsWith('Test') || lastPart.endsWith('Spec')) {
        return parts.join('/') + '.java';
      }
      return parts.join('/') + '.js';
    }
    
    return classname;
  }

  analyzeErrorPatterns() {
    const patterns = {};
    
    this.results.failures.forEach(failure => {
      // Categorize by error type
      let category = 'unknown';
      
      if (failure.error.includes('timeout') || failure.error.includes('exceeded')) {
        category = 'timeout';
      } else if (failure.error.includes('assert') || failure.error.includes('expect')) {
        category = 'assertion';
      } else if (failure.error.includes('null') || failure.error.includes('undefined')) {
        category = 'null-reference';
      } else if (failure.error.includes('network') || failure.error.includes('ECONNREFUSED')) {
        category = 'network';
      } else if (failure.error.includes('permission') || failure.error.includes('EACCES')) {
        category = 'permission';
      }
      
      patterns[category] = (patterns[category] || 0) + 1;
    });

    // Convert to array and add descriptions
    return Object.entries(patterns).map(([type, count]) => ({
      type,
      count,
      description: this.getPatternDescription(type),
      percentage: ((count / this.results.failures.length) * 100).toFixed(2)
    })).sort((a, b) => b.count - a.count);
  }

  getPatternDescription(type) {
    const descriptions = {
      'timeout': 'Tests timing out - may indicate performance issues or infinite loops',
      'assertion': 'Assertion failures - logic errors or incorrect expectations',
      'null-reference': 'Null/undefined errors - missing data or improper initialization',
      'network': 'Network errors - connectivity issues or service unavailability',
      'permission': 'Permission errors - file system or resource access issues',
      'unknown': 'Uncategorized errors - review individually'
    };
    
    return descriptions[type] || 'Unknown error pattern';
  }

  outputResults() {
    // Output for GitHub Actions
    console.log(`::set-output name=total::${this.results.total}`);
    console.log(`::set-output name=passed::${this.results.passed}`);
    console.log(`::set-output name=failed::${this.results.failed}`);
    console.log(`::set-output name=skipped::${this.results.skipped}`);

    // Save detailed results
    fs.writeFileSync('test-summary.json', JSON.stringify({
      name: process.env.INPUT_NAME || 'Test Results',
      ...this.results
    }, null, 2));

    // Save error analysis
    fs.writeFileSync('error-analysis.json', JSON.stringify({
      name: process.env.INPUT_NAME || 'Test Results',
      total: this.results.total,
      failed: this.results.failed,
      passRate: this.results.passRate,
      failures: this.results.failures,
      patterns: this.results.patterns
    }, null, 2));
  }
}

// Parse command line arguments
const args = process.argv.slice(2);
let resultsPath = '';
let format = 'junit';

for (let i = 0; i < args.length; i++) {
  if (args[i] === '--results' && i + 1 < args.length) {
    resultsPath = args[i + 1];
  } else if (args[i] === '--format' && i + 1 < args.length) {
    format = args[i + 1];
  }
}

if (!resultsPath) {
  console.error('Error: --results parameter is required');
  process.exit(1);
}

// Run analyzer
const analyzer = new TestResultAnalyzer(resultsPath, format);
analyzer.analyze()
  .then(() => {
    analyzer.outputResults();
    console.log('Analysis complete!');
  })
  .catch(error => {
    console.error('Analysis failed:', error);
    process.exit(1);
  });