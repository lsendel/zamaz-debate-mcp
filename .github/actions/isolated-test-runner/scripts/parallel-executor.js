#!/usr/bin/env node

const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');
const { Worker } = require('worker_threads');

class ParallelTestExecutor {
  constructor(options) {
    this.groups = JSON.parse(fs.readFileSync(options.groups, 'utf8'));
    this.service = options.service;
    this.suite = options.suite;
    this.isolation = options.isolation;
    this.timeout = parseInt(options.timeout) * 60 * 1000; // Convert to ms
    this.workspace = options.workspace;
    this.results = [];
  }

  async execute() {
    console.log(`🚀 Starting parallel test execution`);
    console.log(`Service: ${this.service}`);
    console.log(`Suite: ${this.suite}`);
    console.log(`Workers: ${this.groups.workers}`);
    console.log(`Isolation: ${this.isolation}`);

    const startTime = Date.now();

    // Create results directory
    const resultsDir = path.join(this.workspace, 'test-results');
    fs.mkdirSync(resultsDir, { recursive: true });

    // Execute test groups in parallel
    const promises = this.groups.groups.map(group => 
      this.executeGroup(group, resultsDir)
    );

    try {
      this.results = await Promise.all(promises);
      const duration = Date.now() - startTime;

      console.log(`\n✅ All tests completed in ${(duration / 1000).toFixed(2)}s`);
      this.generateSummary();

    } catch (error) {
      console.error(`\n❌ Test execution failed:`, error);
      throw error;
    }
  }

  async executeGroup(group, resultsDir) {
    const groupStart = Date.now();
    const groupLog = path.join(resultsDir, `group-${group.id}.log`);
    const groupResult = path.join(resultsDir, `group-${group.id}-results.xml`);

    console.log(`\n👷 Worker ${group.id}: Starting ${group.tests.length} tests`);

    // Determine test command based on service type
    const testCommand = this.getTestCommand(group);

    return new Promise((resolve, reject) => {
      const logStream = fs.createWriteStream(groupLog);
      
      // Set up environment for isolated execution
      const env = {
        ...process.env,
        TEST_WORKER_ID: group.id.toString(),
        TEST_ISOLATION: this.isolation,
        TEST_RESULTS_FILE: groupResult,
        // Add isolated resource URLs from provisioning
        ...(process.env.DATABASE_URL && { DATABASE_URL: process.env.DATABASE_URL }),
        ...(process.env.REDIS_URL && { REDIS_URL: process.env.REDIS_URL }),
        ...(process.env.RABBITMQ_URL && { RABBITMQ_URL: process.env.RABBITMQ_URL })
      };

      // Execute tests
      const testProcess = spawn(testCommand.cmd, testCommand.args, {
        env,
        cwd: this.service,
        shell: true
      });

      // Set timeout
      const timeout = setTimeout(() => {
        console.error(`⏱️ Worker ${group.id}: Timeout after ${this.timeout}ms`);
        testProcess.kill('SIGTERM');
      }, this.timeout);

      // Capture output
      testProcess.stdout.on('data', (data) => {
        logStream.write(data);
        if (process.env.DEBUG) {
          process.stdout.write(`[Worker ${group.id}] ${data}`);
        }
      });

      testProcess.stderr.on('data', (data) => {
        logStream.write(data);
        if (process.env.DEBUG) {
          process.stderr.write(`[Worker ${group.id}] ${data}`);
        }
      });

      testProcess.on('close', (code) => {
        clearTimeout(timeout);
        logStream.end();
        
        const duration = Date.now() - groupStart;
        const result = {
          groupId: group.id,
          tests: group.tests.length,
          exitCode: code,
          duration,
          logFile: groupLog,
          resultFile: groupResult,
          success: code === 0
        };

        if (code === 0) {
          console.log(`✅ Worker ${group.id}: Completed in ${(duration / 1000).toFixed(2)}s`);
        } else {
          console.error(`❌ Worker ${group.id}: Failed with code ${code}`);
        }

        resolve(result);
      });

      testProcess.on('error', (error) => {
        clearTimeout(timeout);
        logStream.end();
        console.error(`❌ Worker ${group.id}: Process error:`, error);
        reject(error);
      });
    });
  }

  getTestCommand(group) {
    const testFiles = group.tests;
    
    // Java tests (Maven)
    if (testFiles[0].endsWith('.java')) {
      const testClasses = testFiles.map(f => this.extractJavaTestClass(f)).join(',');
      
      if (this.suite === 'unit') {
        return {
          cmd: 'mvn',
          args: ['test', '-B', `-Dtest=${testClasses}`, '-DfailIfNoTests=false']
        };
      } else if (this.suite === 'integration') {
        return {
          cmd: 'mvn',
          args: ['verify', '-B', `-Dit.test=${testClasses}`, '-DskipUnitTests']
        };
      }
    }
    
    // JavaScript/TypeScript tests (Jest/Mocha)
    if (testFiles[0].match(/\.(js|ts)$/)) {
      const testPattern = testFiles.join(' ');
      
      // Check for test runner
      const packageJson = path.join(this.service, 'package.json');
      if (fs.existsSync(packageJson)) {
        const pkg = JSON.parse(fs.readFileSync(packageJson, 'utf8'));
        
        if (pkg.scripts && pkg.scripts.test) {
          // Use Jest with specific files
          if (pkg.scripts.test.includes('jest')) {
            return {
              cmd: 'npx',
              args: ['jest', '--ci', '--runInBand', '--testResultsProcessor=jest-junit', ...testFiles]
            };
          }
          // Use Mocha with specific files
          if (pkg.scripts.test.includes('mocha')) {
            return {
              cmd: 'npx',
              args: ['mocha', '--reporter=xunit', '--reporter-options', `output=${group.resultFile}`, ...testFiles]
            };
          }
        }
      }
    }

    // Default: run npm test with pattern
    return {
      cmd: 'npm',
      args: ['test', '--', ...testFiles]
    };
  }

  extractJavaTestClass(filePath) {
    // Convert file path to Java class name
    // e.g., src/test/java/com/mcp/gateway/UserTest.java -> com.mcp.gateway.UserTest
    const match = filePath.match(/src\/test\/java\/(.+)\.java$/);
    if (match) {
      return match[1].replace(/\//g, '.');
    }
    return path.basename(filePath, '.java');
  }

  generateSummary() {
    const summary = {
      service: this.service,
      suite: this.suite,
      workers: this.groups.workers,
      totalTests: this.groups.totalTests,
      results: this.results,
      metrics: this.calculateMetrics()
    };

    const summaryPath = path.join(this.workspace, 'execution-summary.json');
    fs.writeFileSync(summaryPath, JSON.stringify(summary, null, 2));

    // Print summary
    console.log('\n📊 Execution Summary:');
    console.log(`  Total Groups: ${this.results.length}`);
    console.log(`  Successful: ${this.results.filter(r => r.success).length}`);
    console.log(`  Failed: ${this.results.filter(r => !r.success).length}`);
    console.log(`  Total Duration: ${summary.metrics.totalDuration}ms`);
    console.log(`  Average Duration: ${summary.metrics.avgDuration}ms`);
    console.log(`  Parallel Efficiency: ${summary.metrics.efficiency}%`);
  }

  calculateMetrics() {
    const durations = this.results.map(r => r.duration);
    const totalDuration = Math.max(...durations);
    const sumDuration = durations.reduce((a, b) => a + b, 0);
    const avgDuration = Math.round(sumDuration / durations.length);
    
    // Calculate efficiency: ideal time / actual time
    const idealTime = sumDuration / this.groups.workers;
    const efficiency = Math.round((idealTime / totalDuration) * 100);

    // Resource usage (if available)
    const resourceUsage = this.collectResourceUsage();

    return {
      totalDuration,
      avgDuration,
      efficiency,
      resourceUsage
    };
  }

  collectResourceUsage() {
    // Collect resource usage from worker logs
    const usage = {
      cpu: [],
      memory: [],
      io: []
    };

    // This would parse worker logs for resource metrics
    // For now, return placeholder data
    return usage;
  }
}

// Parse command line arguments
const args = process.argv.slice(2);
const options = {
  groups: '',
  service: '',
  suite: '',
  isolation: 'process',
  timeout: '30',
  workspace: ''
};

for (let i = 0; i < args.length; i += 2) {
  const key = args[i].replace('--', '');
  if (key in options) {
    options[key] = args[i + 1];
  }
}

// Validate required options
if (!options.groups || !options.service || !options.suite || !options.workspace) {
  console.error('Error: Missing required options');
  process.exit(1);
}

// Execute tests
const executor = new ParallelTestExecutor(options);
executor.execute().catch(error => {
  console.error('Execution failed:', error);
  process.exit(1);
});