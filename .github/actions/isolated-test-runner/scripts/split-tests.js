#!/usr/bin/env node

const fs = require('fs');
const path = require('path');
const glob = require('glob');

class TestSplitter {
  constructor(service, suite, workers) {
    this.service = service;
    this.suite = suite;
    this.workers = parseInt(workers);
    this.testFiles = [];
    this.testGroups = [];
  }

  async split() {
    console.log(`📂 Splitting tests for ${this.service} - ${this.suite} suite`);
    console.log(`Workers: ${this.workers}`);

    // Find test files based on service and suite
    this.testFiles = await this.findTestFiles();
    console.log(`Found ${this.testFiles.length} test files`);

    // Get test execution times from history
    const testTimes = await this.getTestExecutionTimes();

    // Split tests into balanced groups
    this.testGroups = this.balanceTestGroups(testTimes);

    return this.testGroups;
  }

  async findTestFiles() {
    const patterns = this.getTestPatterns();
    let files = [];

    for (const pattern of patterns) {
      const matches = glob.sync(pattern, { 
        ignore: ['**/node_modules/**', '**/build/**', '**/dist/**']
      });
      files = files.concat(matches);
    }

    return [...new Set(files)]; // Remove duplicates
  }

  getTestPatterns() {
    const baseDir = this.service;
    
    switch (this.suite) {
      case 'unit':
        return [
          `${baseDir}/src/test/java/**/*Test.java`,
          `${baseDir}/src/test/java/**/*Tests.java`,
          `${baseDir}/src/**/*.test.js`,
          `${baseDir}/src/**/*.test.ts`,
          `${baseDir}/__tests__/**/*.js`,
          `${baseDir}/__tests__/**/*.ts`
        ];
      
      case 'integration':
        return [
          `${baseDir}/src/test/java/**/*IT.java`,
          `${baseDir}/src/test/java/**/*IntegrationTest.java`,
          `${baseDir}/src/**/*.integration.test.js`,
          `${baseDir}/src/**/*.integration.test.ts`
        ];
      
      case 'e2e':
        return [
          `${baseDir}/e2e/**/*.test.js`,
          `${baseDir}/e2e/**/*.spec.js`,
          `${baseDir}/cypress/integration/**/*.spec.js`,
          `${baseDir}/tests/e2e/**/*.test.js`
        ];
      
      default:
        return [`${baseDir}/**/*.test.*`, `${baseDir}/**/*.spec.*`];
    }
  }

  async getTestExecutionTimes() {
    // Try to load historical test execution times
    const historyFile = `.test-history/${this.service}-${this.suite}.json`;
    
    if (fs.existsSync(historyFile)) {
      const history = JSON.parse(fs.readFileSync(historyFile, 'utf8'));
      return history.testTimes || {};
    }

    // If no history, estimate based on file size and type
    const testTimes = {};
    
    for (const file of this.testFiles) {
      const stats = fs.statSync(file);
      const size = stats.size;
      
      // Estimate execution time based on file size and type
      let estimatedTime = size / 1000; // Base estimate: 1ms per byte
      
      if (file.includes('integration') || file.includes('IT')) {
        estimatedTime *= 3; // Integration tests are slower
      }
      if (file.includes('e2e')) {
        estimatedTime *= 5; // E2E tests are even slower
      }
      
      testTimes[file] = estimatedTime;
    }

    return testTimes;
  }

  balanceTestGroups(testTimes) {
    // Sort tests by execution time (descending)
    const sortedTests = Object.entries(testTimes)
      .sort((a, b) => b[1] - a[1])
      .map(([file, time]) => ({ file, time }));

    // Initialize groups
    const groups = Array(this.workers).fill(null).map((_, i) => ({
      id: i + 1,
      tests: [],
      totalTime: 0
    }));

    // Distribute tests using greedy algorithm
    for (const test of sortedTests) {
      // Find group with minimum total time
      const minGroup = groups.reduce((min, group) => 
        group.totalTime < min.totalTime ? group : min
      );

      minGroup.tests.push(test.file);
      minGroup.totalTime += test.time;
    }

    // Calculate efficiency metrics
    const times = groups.map(g => g.totalTime);
    const maxTime = Math.max(...times);
    const avgTime = times.reduce((a, b) => a + b, 0) / times.length;
    const efficiency = (avgTime / maxTime * 100).toFixed(2);

    console.log(`\n📊 Test Distribution:`);
    groups.forEach(group => {
      console.log(`  Worker ${group.id}: ${group.tests.length} tests, ${group.totalTime.toFixed(0)}ms`);
    });
    console.log(`  Efficiency: ${efficiency}%`);

    return groups;
  }

  saveGroups(outputPath) {
    const output = {
      service: this.service,
      suite: this.suite,
      workers: this.workers,
      totalTests: this.testFiles.length,
      groups: this.testGroups,
      metadata: {
        splitTime: new Date().toISOString(),
        efficiency: this.calculateEfficiency()
      }
    };

    fs.writeFileSync(outputPath, JSON.stringify(output, null, 2));
    console.log(`\n✅ Test groups saved to ${outputPath}`);
  }

  calculateEfficiency() {
    const times = this.testGroups.map(g => g.totalTime);
    const maxTime = Math.max(...times);
    const avgTime = times.reduce((a, b) => a + b, 0) / times.length;
    return (avgTime / maxTime * 100).toFixed(2);
  }
}

// Parse command line arguments
const args = process.argv.slice(2);
let service = '';
let suite = '';
let workers = '4';
let output = '';

for (let i = 0; i < args.length; i++) {
  switch (args[i]) {
    case '--service':
      service = args[++i];
      break;
    case '--suite':
      suite = args[++i];
      break;
    case '--workers':
      workers = args[++i];
      break;
    case '--output':
      output = args[++i];
      break;
  }
}

if (!service || !suite || !output) {
  console.error('Error: --service, --suite, and --output are required');
  process.exit(1);
}

// Run splitter
const splitter = new TestSplitter(service, suite, workers);
splitter.split()
  .then(() => {
    splitter.saveGroups(output);
  })
  .catch(error => {
    console.error('Error splitting tests:', error);
    process.exit(1);
  });